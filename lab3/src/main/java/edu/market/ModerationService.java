package edu.market;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ModerationService {
    private final AccountService accounts;
    private final AccountRepository accountStore;
    private final ProductService products;
    private final ProductRepository productStore;
    private final ModerationRepository cases;
    private final ActivityService activities;

    public ModerationService(AccountService accounts, AccountRepository accountStore, ProductService products,
                             ProductRepository productStore, ModerationRepository cases, ActivityService activities) {
        this.accounts = accounts;
        this.accountStore = accountStore;
        this.products = products;
        this.productStore = productStore;
        this.cases = cases;
        this.activities = activities;
    }

    public ModerationCase report(String username, String type, Long target, String reason, String image, Instant now) {
        Account actor = accounts.current(username);
        BusinessException.require(!actor.getBanned() && !actor.getRole().equals("ADMIN"), "仅正常买家或卖家可举报");
        lockTarget(type, target);
        LocalDate day = now.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        BusinessException.require(!cases.existsByAuthorIdAndTargetTypeAndTargetIdAndReportDateAndKind(
                actor.getId(), type, target, day, "REPORT"), "同一对象每自然日仅可举报一次");
        ModerationCase item = create(actor, "REPORT", type, target, reason);
        BusinessException.require(image == null || image.isBlank() || image.matches("https://[^\\s]+\\.(?i:png|jpg|jpeg|webp)")
                || image.matches("/media/[a-zA-Z0-9.-]+"), "举报图片地址格式无效");
        item.setImageUrl(image);
        item.setReportDate(day);
        return cases.save(item);
    }

    public ModerationCase appeal(String username, String type, Long target, String reason) {
        Account actor = accounts.current(username);
        lockTarget(type, target);
        if (type.equals("PRODUCT")) {
            Product product = products.locked(target);
            BusinessException.require(actor.getId().equals(product.getSellerId())
                    && product.getStatus() == ProductStatus.VIOLATION, "只可申诉自己的违规下架商品");
        } else {
            BusinessException.require(actor.getId().equals(target) && actor.getBanned(), "只可申诉自己的封禁账户");
        }
        BusinessException.require(!cases.existsByAuthorIdAndTargetTypeAndTargetIdAndKindAndStatus(
                actor.getId(), type, target, "APPEAL", "PENDING"), "已有待处理申诉");
        return cases.save(create(actor, "APPEAL", type, target, reason));
    }

    public void punish(String username, String type, Long target, String reason) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        note(reason);
        lockTarget(type, target);
        if (type.equals("PRODUCT")) {
            Product product = products.locked(target);
            product.setListingRevision(product.getListingRevision() + 1);
            product.setStatus(ProductStatus.VIOLATION);
            product.setReviewNote(reason);
        } else {
            Account account = accountStore.locked(target).orElseThrow();
            BusinessException.require(!account.getRole().equals("ADMIN"), "不能通过普通违规处置封禁管理员");
            account.setBanned(true);
        }
        activities.record(username, type, target, "PUNISH", reason);
    }

    public void decide(String username, Long id, boolean accept, String feedback) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        var snapshot = cases.target(id).orElseThrow(() -> new BusinessException("记录不存在"));
        lockTarget(snapshot.getTargetType(), snapshot.getTargetId());
        // Re-read under the shared target lock so concurrent reviewers cannot resolve the same case twice.
        ModerationCase item = cases.locked(id).orElseThrow();
        BusinessException.require(item.getStatus().equals("PENDING"), "记录已处理");
        note(feedback);
        if (accept && item.getKind().equals("APPEAL")) {
            if (item.getTargetType().equals("PRODUCT")) {
                Product product = products.locked(item.getTargetId());
                BusinessException.require(product.getStatus() == ProductStatus.VIOLATION, "商品不在违规下架状态");
                product.setStatus(ProductStatus.PENDING_REVIEW);
                product.setReviewNote("申诉通过，需再次审核");
            } else {
                accountStore.locked(item.getTargetId()).orElseThrow().setBanned(false);
            }
        }
        item.setStatus(accept ? "ACCEPTED" : "REJECTED");
        item.setFeedback(feedback);
        activities.record(username, "MODERATION", id, "DECIDE", item.getStatus());
    }

    private ModerationCase create(Account actor, String kind, String type, Long target, String reason) {
        note(reason);
        ModerationCase item = new ModerationCase();
        item.setKind(kind);
        item.setTargetType(type);
        item.setTargetId(target);
        item.setAuthorId(actor.getId());
        item.setReason(reason);
        item.setCreatedAt(Instant.now());
        item.setStatus("PENDING");
        ModerationCase saved = cases.save(item);
        activities.record(actor.getUsername(), "MODERATION", saved.getId(), kind, "PENDING");
        return saved;
    }

    private void lockTarget(String type, Long target) {
        if ("PRODUCT".equals(type)) {
            products.locked(target);
        } else {
            BusinessException.require("ACCOUNT".equals(type), "举报对象类型无效");
            accountStore.locked(target).orElseThrow(() -> new BusinessException("账户不存在"));
        }
    }

    private void note(String value) {
        BusinessException.require(value != null && !value.isBlank() && value.length() <= 500, "原因或反馈为 1–500 字");
    }
}
