package edu.market;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class StatisticsService {
    private final AccountService accounts;
    private final EntityManager entityManager;
    public StatisticsService(AccountService accounts, EntityManager entityManager) {
        this.accounts = accounts;
        this.entityManager = entityManager;
    }
    public Summary summary(String username, LocalDate from, LocalDate until) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        BusinessException.require(from != null && until != null && !from.isAfter(until), "日期区间无效");
        Instant start = from.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        Instant end = until.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        Object[] row = entityManager.createQuery("select count(o), sum(o.quantity), sum(o.total) from PurchaseOrder o "
                + "where o.status = :status and coalesce(o.completedAt, o.createdAt) >= :start "
                + "and coalesce(o.completedAt, o.createdAt) < :end", Object[].class)
                .setParameter("status", OrderStatus.COMPLETED).setParameter("start", start).setParameter("end", end).getSingleResult();
        long count = ((Number) row[0]).longValue();
        if (count == 0) {
            return new Summary("0", 0L, BigDecimal.ZERO, false);
        }
        if (count < 5) {
            return new Summary("1–4（低样本合并）", null, null, true);
        }
        return new Summary(Long.toString(count), ((Number) row[1]).longValue(), (BigDecimal) row[2], false);
    }
    public record Summary(String orderRange, Long quantity, BigDecimal gross, boolean suppressed) {
    }
}
