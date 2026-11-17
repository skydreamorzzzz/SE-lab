package edu.market;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
    private final ProductRepository products;
    private final ActivityService activities;
    private final AccountService accounts;
    private final ReservationRepository reservations;
    private final AccountRepository accountStore;
    private final PersonalRepository personal;

    public ProductService(ProductRepository products, AccountService accounts, ReservationRepository reservations, ActivityService activities, AccountRepository accountStore, PersonalRepository personal) {
        this.products = products;
        this.accounts = accounts;
        this.activities = activities;
        this.reservations = reservations;
        this.accountStore = accountStore;
        this.personal = personal;
    }

    public Product save(String username, Long id, Product input) {
        Account seller = accounts.current(username);
        accounts.requireRole(seller, "SELLER");
        Product product;
        if (id == null) {
            product = new Product();
            product.setSellerId(seller.getId());
            product.setStatus(ProductStatus.DRAFT);
            product.setStock(0);
        } else {
            product = locked(id);
            owner(seller, product);
            BusinessException.require(product.getStatus() != ProductStatus.VIOLATION
                    && product.getStatus() != ProductStatus.PENDING_REVIEW, "当前状态不能编辑");
        }
        BusinessException.require(input.getReservationLimit() >= 0, "预约计划不能为负数");
        BusinessException.require(input.getReservationLimit() <= 1000000, "预约计划不得超过 1000000");
        BusinessException.require(input.getName() == null || input.getName().length() <= 100, "名称过长");
        BusinessException.require(input.getCategory() == null || input.getCategory().length() <= 100, "分类过长");
        BusinessException.require(input.getConditionLabel() == null || input.getConditionLabel().length() <= 40,
                "成色说明不得超过 40 字");
        BusinessException.require(input.getImageUrl() == null || input.getImageUrl().length() <= 250,
                "图片地址不得超过 250 字");
        if (input.getMinPrice() != null) {
            BusinessException.require(input.getMinPrice().signum() > 0 && input.getMinPrice().scale() <= 2,
                    "最低价必须为正数，最多两位小数");
        }
        if (input.getMaxPrice() != null) {
            BusinessException.require(input.getMaxPrice().signum() > 0 && input.getMaxPrice().scale() <= 2,
                    "最高价必须为正数，最多两位小数");
        }
        BusinessException.require(input.getDescription() == null || input.getDescription().length() <= 2000,
                "描述不超过 2000 字");
        boolean changed = product.getNegotiable() != input.getNegotiable() || !Objects.equals(product.getName(), input.getName())
                || !Objects.equals(product.getDescription(), input.getDescription())
                || !Objects.equals(product.getMinPrice(), input.getMinPrice())
                || !Objects.equals(product.getMaxPrice(), input.getMaxPrice())
                || !Objects.equals(product.getImageUrl(), input.getImageUrl())
                || !Objects.equals(product.getCategory(), input.getCategory())
                || !Objects.equals(product.getConditionLabel(), input.getConditionLabel());
        product.setNegotiable(input.getNegotiable());
        if (changed) {
            product.setListingRevision(product.getListingRevision() + 1);
        }
        product.setName(input.getName());
        product.setCategory(input.getCategory());
        product.setConditionLabel(input.getConditionLabel());
        String condition = input.getConditionLabel();
        product.setConditionLevel("全新".equals(condition) ? 100 : "近全新".equals(condition) ? 99
                : condition != null && condition.matches("[1-9]成新") ? (condition.charAt(0) - '0') * 10 : 0);
        product.setDescription(input.getDescription());
        product.setImageUrl(input.getImageUrl());
        product.setMinPrice(input.getMinPrice());
        product.setMaxPrice(input.getMaxPrice());
        // Stock is changed through the inventory operation, never mass-assigned from an edit form.
        long reserved = reservations.findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(product.getId())
                .stream().mapToLong(Reservation::getQuantity).sum();
        BusinessException.require(input.getReservationLimit() >= reserved, "计划不能低于当前有效预约总量");
        product.setReservationLimit(input.getReservationLimit());
        if (changed && (product.getStatus() == ProductStatus.ON_SALE
                || product.getStatus() == ProductStatus.SOLD_OUT)) {
            validate(product);
            product.setStatus(ProductStatus.PENDING_REVIEW);
            product.setReviewNote("关键字段修改，等待复审");
        }
        Product saved = products.save(product);
        activities.record(username, "PRODUCT", saved.getId(), "SAVE", saved.getStatus().name());
        return saved;
    }

    public void submit(String username, Long id) {
        Product product = locked(id);
        owner(accounts.current(username), product);
        BusinessException.require(product.getStatus() == ProductStatus.DRAFT
                || product.getStatus() == ProductStatus.OFF_SHELF, "仅草稿或普通下架商品可提交");
        validate(product);
        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setReviewNote("已提交审核");
        activities.record(username, "PRODUCT", id, "SUBMIT", product.getStatus().name());
    }

    public void audit(String username, Long id, boolean approve, String note) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        Product product = locked(id);
        BusinessException.require(product.getStatus() == ProductStatus.PENDING_REVIEW, "商品不在待审核状态");
        BusinessException.require(approve || (note != null && !note.isBlank()), "驳回需填写理由");
        BusinessException.require(note == null || note.length() <= 200, "审核意见不超过 200 字");
        product.setStatus(approve ? (product.getStock() > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT)
                : ProductStatus.DRAFT);
        product.setReviewNote(note);
        if (approve) {
            product.setPublishedAt(java.time.Instant.now());
            allocateStock(product);
            product.setStatus(product.getStock() == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE);
        }
        activities.record(username, "PRODUCT", id, "AUDIT", product.getStatus().name());
    }

    public void withdraw(String username, Long id) {
        Product product = locked(id);
        owner(accounts.current(username), product);
        BusinessException.require(isPublic(product), "仅公开商品可下架");
        product.setListingRevision(product.getListingRevision() + 1);
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setReviewNote("卖家主动下架，再发布需审核");
        activities.record(username, "PRODUCT", id, "WITHDRAW", product.getStatus().name());
    }

    public Product detail(Long id, String username) {
        Product product = products.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
        if (!isPublic(product) || sellerBanned(product)) {
            Account actor = accounts.current(username);
            BusinessException.require(actor.getId().equals(product.getSellerId())
                    || actor.getRole().equals("ADMIN"), "无权查看未公开商品");
        }
        if (username != null) {
            BusinessException.require(personal.findByOwnerIdAndKindAndTargetKey(accounts.current(username).getId(),
                    "BLOCK", product.getSellerId().toString()).isEmpty(), "已屏蔽此卖家");
        }
        return product;
    }

    public List<Product> search(String keyword, String category, BigDecimal min, BigDecimal max,
                                ProductStatus status, String sort) {
        return searchPage(null, keyword, category, min, max, status, sort, 0).getContent();
    }

    public org.springframework.data.domain.Page<Product> searchPage(String username, String keyword, String category,
            BigDecimal min, BigDecimal max, ProductStatus status, String sort, int page) {
        BusinessException.require(page >= 0 && page <= 100000, "页码无效");
        BusinessException.require(keyword == null || keyword.length() <= 100, "关键词不超过100字");
        BusinessException.require(category == null || category.isBlank() || category.matches("[^/%_]{1,50}(/[^/%_]{1,50})?"), "分类无效");
        BusinessException.require(min == null || min.signum() >= 0, "最低价不能为负");
        BusinessException.require(max == null || max.signum() >= 0, "最高价不能为负");
        BusinessException.require(min == null || max == null || min.compareTo(max) <= 0, "价格区间倒置");
        BusinessException.require(status == null || status == ProductStatus.ON_SALE
                || status == ProductStatus.SOLD_OUT, "只能检索公开状态");
        Specification<Product> spec = (root, query, cb) -> {
            var sellers = query.subquery(Long.class);
            var seller = sellers.from(Account.class);
            sellers.select(seller.get("id")).where(cb.isFalse(seller.get("banned")));
            return cb.and(root.get("status").in(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT), root.get("sellerId").in(sellers));
        };
        if (username != null) {
            Long userId = accounts.current(username).getId();
            List<Long> blocked = personal.findByOwnerIdAndKindOrderByUpdatedAtDesc(userId, "BLOCK")
                    .stream().map(PersonalEntry::getTargetId).toList();
            if (!blocked.isEmpty()) {
                spec = spec.and((r, q, c) -> c.not(r.get("sellerId").in(blocked)));
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((r, q, c) -> c.like(c.lower(r.get("name")),
                    "%" + keyword.toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%", '\\'));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((r, q, c) -> c.or(c.equal(r.get("category"), category),
                    c.like(r.get("category"), category + "/%")));
        }
        if (min != null) {
            spec = spec.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("maxPrice"), min));
        }
        if (max != null) {
            spec = spec.and((r, q, c) -> c.lessThanOrEqualTo(r.get("minPrice"), max));
        }
        if (status != null) {
            spec = spec.and((r, q, c) -> c.equal(r.get("status"), status));
        }
        String field = switch (sort == null ? "newest" : sort) {
            case "price" -> "minPrice";
            case "favorites" -> "favoriteCount";
            case "sales" -> "soldQuantity";
            case "reputation" -> "ratingScore";
            case "condition" -> "conditionLevel";
            case "newest", "default" -> "publishedAt";
            default -> throw new BusinessException("排序方式无效");
        };
        Sort order = "price".equals(sort) ? Sort.by(field).ascending() : Sort.by(field).descending();
        return products.findAll(spec, org.springframework.data.domain.PageRequest.of(page, 48, order.and(Sort.by("id").descending())));
    }

    public Product locked(Long id) {
        return products.lockById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public void allocateStock(Product product) {
        if (!isPublic(product) || sellerBanned(product)) {
            return;
        }
        List<Reservation> queue = reservations.findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(product.getId());
        for (Reservation item : queue) {
            int available = product.getStock();
            int remaining = item.getQuantity() - item.getAllocated();
            int allocated = Math.min(available, remaining);
            item.setAllocated(item.getAllocated() + allocated);
            product.setStock(available - allocated);
        }
    }

    public void owner(Account actor, Product product) {
        accounts.requireRole(actor, "SELLER");
        BusinessException.require(actor.getId().equals(product.getSellerId()), "只能管理自己的商品");
    }

    public boolean sellerBanned(Product product) {
        return accountStore.findById(product.getSellerId()).map(Account::getBanned).orElse(true);
    }

    public boolean isPublic(Product product) {
        return product.getStatus() == ProductStatus.ON_SALE || product.getStatus() == ProductStatus.SOLD_OUT;
    }

    private void validate(Product product) {
        BusinessException.require(product.getName() != null && !product.getName().isBlank(), "请填写名称");
        BusinessException.require(product.getCategory() != null && product.getCategory().matches("[^/]+/[^/]+"),
                "分类格式为 一级/二级，例如 数码电子/相机");
        BusinessException.require(product.getConditionLabel() != null && !product.getConditionLabel().isBlank(),
                "请填写成色");
        BusinessException.require(product.getDescription() != null && !product.getDescription().isBlank(),
                "请填写描述");
        BusinessException.require(product.getImageUrl() != null
                && (product.getImageUrl().matches("https://[^\\s]+\\.(?i:png|jpg|jpeg|webp)")
                || product.getImageUrl().equals("/images/sample.svg")), "请提供 HTTPS 图片地址或使用本地示例图");
        BusinessException.require(product.getMinPrice() != null && product.getMaxPrice() != null
                && product.getMinPrice().signum() > 0
                && product.getMinPrice().scale() <= 2 && product.getMaxPrice().scale() <= 2
                && product.getMaxPrice().compareTo(new BigDecimal("99999999")) <= 0
                && product.getMinPrice().compareTo(product.getMaxPrice()) <= 0, "价格须为合法的正数区间，最多两位小数");
    }
}
