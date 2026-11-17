package edu.market;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class RankingService {
    private final EntityManager entityManager;
    private final PersonalService personal;
    public RankingService(EntityManager entityManager, PersonalService personal) {
        this.entityManager = entityManager;
        this.personal = personal;
    }
    public List<RankRow> rank(String username, String period, String category) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate first = switch (period) {
            case "day" -> today;
            case "week" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> today.withDayOfMonth(1);
            default -> throw new BusinessException("榜单周期无效");
        };
        BusinessException.require(category != null && (category.isBlank() || category.matches("[^/%_]{1,50}(/[^/%_]{1,50})?")), "分类无效");
        Instant since = first.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        List<Long> blocked = personal.blockedSellers(username);
        String jpql = "select p.id, p.name, p.sellerId, sum(o.quantity) from Product p, PurchaseOrder o, Account a "
                + "where o.productId = p.id and a.id = p.sellerId and a.banned = false and o.status = :completed "
                + "and p.status in :visible and coalesce(o.completedAt, o.createdAt) >= :since "
                + "and (:category = '' or p.category = :category or p.category like :prefix) "
                + (blocked.isEmpty() ? "" : "and p.sellerId not in :blocked ")
                + "group by p.id, p.name, p.sellerId order by sum(o.quantity) desc, p.id asc";
        var query = entityManager.createQuery(jpql, Object[].class).setParameter("completed", OrderStatus.COMPLETED)
                .setParameter("visible", List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT)).setParameter("since", since)
                .setParameter("category", category).setParameter("prefix", category + "/%");
        if (!blocked.isEmpty()) {
            query.setParameter("blocked", blocked);
        }
        return query.setMaxResults(20).getResultList().stream()
                .map(r -> new RankRow((Long) r[0], (String) r[1], ((Number) r[3]).longValue())).toList();
    }
    public record RankRow(Long productId, String name, long quantity) {
    }
}
