package edu.market;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DesignAdminService {
    private final AccountService accounts;
    private final AccountRepository accountStore;
    private final ProductRepository products;
    private final AuditSnapshotRepository snapshots;
    private final ModerationRepository cases;
    private final DesignViewService design;
    private final StatisticsService statistics;
    private final jakarta.persistence.EntityManager database;

    public DesignAdminService(AccountService accounts, AccountRepository accountStore, ProductRepository products,
            AuditSnapshotRepository snapshots, ModerationRepository cases, DesignViewService design,
            StatisticsService statistics, jakarta.persistence.EntityManager database) {
        this.accounts = accounts;
        this.accountStore = accountStore;
        this.products = products;
        this.snapshots = snapshots;
        this.cases = cases;
        this.design = design;
        this.statistics = statistics;
        this.database = database;
    }

    public Map<String, Object> dashboard(String username) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        var queue = new ArrayList<Map<String, Object>>();
        var violations = new ArrayList<Map<String, Object>>();
        var accountsList = accountStore.findAll();
        Map<Long, Account> byId = accountsList.stream().collect(java.util.stream.Collectors.toMap(Account::getId, a -> a));
        var metrics = design.metrics(byId.keySet());
        for (Product p : products.findAll()) {
            if (p.getStatus() == ProductStatus.PENDING_REVIEW) {
                queue.add(item("P-" + p.getId(), p.getId(), p.getName(), p.getCategory(), p.getDescription(), p.getConditionLabel(),
                        p.getImageUrl(), p.getMinPrice(), p.getStock(), "pending", p.getReviewNote(), p.getSellerId(), byId, metrics, "待审核"));
            }
            if (p.getStatus() == ProductStatus.ON_SALE || p.getStatus() == ProductStatus.SOLD_OUT || p.getStatus() == ProductStatus.VIOLATION) {
                violations.add(item("P-" + p.getId(), p.getId(), p.getName(), p.getCategory(), p.getDescription(), p.getConditionLabel(),
                        p.getImageUrl(), p.getMinPrice(), p.getStock(), p.getStatus() == ProductStatus.VIOLATION ? "removed" : "info",
                        p.getReviewNote(), p.getSellerId(), byId, metrics, text(p.getPublishedAt())));
            }
        }
        for (AuditSnapshot s : snapshots.findAllByOrderByIdDesc()) {
            queue.add(item("H-" + s.getId(), s.getProductId(), s.getName(), s.getCategory(), s.getDescription(), s.getConditionLabel(),
                    s.getImageUrl(), s.getPrice(), s.getQuantity(), s.getStatus(), s.getNote(), s.getSellerId(), byId, metrics, text(s.getReviewedAt())));
        }
        var reports = cases.findAllByOrderByIdDesc().stream().map(c -> Map.of(
                "id", "R-" + c.getId(), "rawId", c.getId(), "type", c.getKind().equals("APPEAL") ? "违规申诉" : c.getTargetType().equals("PRODUCT") ? "商品举报" : "用户举报",
                "target", c.getTargetType() + " #" + c.getTargetId(), "reason", text(c.getReason()), "date", text(c.getCreatedAt()),
                "status", c.getStatus().equals("PENDING") ? "pending" : "done", "note", text(c.getFeedback()), "imageUrl", text(c.getImageUrl()))).toList();
        var users = accountsList.stream().filter(Account::getTrader).map(a -> Map.of(
                "id", "U-" + a.getId(), "rawId", a.getId(), "name", a.getDisplayName(), "roles", "买家 / 卖家",
                "status", a.getBanned() ? "已封禁" : "正常", "score", metrics.get(a.getId()).score(), "joined", text(a.getCreatedAt()))).toList();
        return Map.of("products", queue, "violations", violations, "reports", reports, "users", users);
    }

    public Map<String, Object> statistics(String username, int days) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        BusinessException.require(days == 7 || days == 30, "统计范围为7天或30天");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        var series = new ArrayList<Map<String, Object>>();
        // Low-sample bins show ranges only, and are never drawn as an invented exact count.
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            var summary = statistics.summary(username, date, date);
            series.add(Map.of("label", date.toString().substring(5), "range", summary.orderRange(),
                    "value", summary.suppressed() ? 0 : Long.parseLong(summary.orderRange()), "suppressed", summary.suppressed()));
        }
        var totals = statistics.summary(username, today.minusDays(days - 1), today);
        long reservations = database.createQuery("select count(r) from Reservation r where r.createdAt >= :since", Long.class)
                .setParameter("since", today.minusDays(days - 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()).getSingleResult();
        long converted = database.createQuery("select count(distinct o.reservationId) from PurchaseOrder o where o.reservationId is not null "
                + "and o.reservationId in (select r.id from Reservation r where r.createdAt >= :since)", Long.class)
                .setParameter("since", today.minusDays(days - 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()).getSingleResult();
        return Map.of("summary", totals, "series", series, "products", products.count(), "conversion",
                reservations == 0 ? "—" : reservations < 5 ? "低样本合并" : Math.round(1000.0 * converted / reservations) / 10.0 + "%");
    }

    private Map<String, Object> item(String id, Long rawId, String name, String category, String description, String condition,
            String imageUrl, java.math.BigDecimal price, int quantity, String status, String note, Long sellerId,
            Map<Long, Account> byId, Map<Long, DesignViewService.Metric> metrics, String date) {
        var row = new LinkedHashMap<String, Object>();
        row.put("id", id);
        row.put("rawId", rawId);
        row.put("name", text(name));
        row.put("category", text(category));
        row.put("description", text(description));
        row.put("condition", text(condition));
        row.put("imageUrl", text(imageUrl));
        row.put("price", price == null ? java.math.BigDecimal.ZERO : price);
        row.put("quantity", quantity);
        row.put("status", status);
        row.put("auditReason", text(note));
        row.put("flag", status.equals("removed") ? text(note) : "公开商品例行核查");
        row.put("seller", byId.get(sellerId).getDisplayName());
        row.put("sales", metrics.get(sellerId).recentSold());
        row.put("score", metrics.get(sellerId).score());
        row.put("date", date);
        return row;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
