package edu.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DesignViewService {
    private final AccountRepository accounts;
    private final PersonalRepository personal;
    private final EntityManager database;

    public DesignViewService(AccountRepository accounts, PersonalRepository personal, EntityManager database) {
        this.accounts = accounts;
        this.personal = personal;
        this.database = database;
    }

    public List<Card> cards(List<Product> products, String username, Double latitude, Double longitude) {
        if (products.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = products.stream().map(Product::getSellerId).collect(Collectors.toSet());
        Map<Long, Account> sellers = accounts.findAllById(ids).stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        Map<Long, Metric> metrics = metrics(ids);
        Set<Long> favorites = username == null ? Set.of() : personal.findByOwnerIdAndKindOrderByUpdatedAtDesc(
                accounts.findByUsername(username).orElseThrow().getId(), "FAVORITE").stream()
                .map(PersonalEntry::getTargetId).collect(Collectors.toSet());
        return products.stream().map(p -> {
            Account seller = sellers.get(p.getSellerId());
            Metric metric = metrics.get(p.getSellerId());
            return new Card(p.getId(), p.getName(), p.getCategory(), p.getMinPrice(), p.getMaxPrice(),
                    p.getDescription(), p.getConditionLabel(), p.getImageUrl(), p.getStatus().name(), p.getStatus().getLabel(),
                    p.getStock(), p.getSellerId(), seller.getDisplayName(), seller.getCity(), metric.totalSold(), metric.recentSold(),
                    metric.score(), metric.positiveRate(), metric.reviewCount(), Geo.distance(latitude, longitude, p.getLatitude(), p.getLongitude()),
                    favorites.contains(p.getId()), p.getNegotiable(), p.getCity(), p.getTradeTags(), p.getPublishedAt());
        }).toList();
    }

    public Map<Long, Metric> metrics(Set<Long> sellerIds) {
        Map<Long, Metric> result = new HashMap<>();
        sellerIds.forEach(id -> result.put(id, new Metric(0, 0, 0, 0, 0)));
        if (sellerIds.isEmpty()) {
            return result;
        }
        Map<Long, double[]> ratings = new HashMap<>();
        database.createQuery("select r.recipientId, count(r), avg(r.rating), sum(case when r.rating >= 4 then 1 else 0 end) "
                + "from Review r where r.recipientId in :ids group by r.recipientId", Object[].class)
                .setParameter("ids", sellerIds).getResultList().forEach(row -> ratings.put((Long) row[0],
                        new double[] {((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue(), ((Number) row[3]).doubleValue()}));
        Map<Long, Long> recent = new HashMap<>();
        database.createQuery("select o.sellerId, sum(o.quantity) from PurchaseOrder o where o.sellerId in :ids "
                + "and o.status = :completed and coalesce(o.completedAt, o.createdAt) >= :since group by o.sellerId", Object[].class)
                .setParameter("ids", sellerIds).setParameter("completed", OrderStatus.COMPLETED)
                .setParameter("since", Instant.now().minus(Duration.ofDays(30))).getResultList()
                .forEach(row -> recent.put((Long) row[0], ((Number) row[1]).longValue()));
        Map<Long, Long> total = new HashMap<>();
        database.createQuery("select o.sellerId, sum(o.quantity) from PurchaseOrder o where o.sellerId in :ids "
                + "and o.status = :completed group by o.sellerId", Object[].class).setParameter("ids", sellerIds)
                .setParameter("completed", OrderStatus.COMPLETED).getResultList()
                .forEach(row -> total.put((Long) row[0], ((Number) row[1]).longValue()));
        sellerIds.forEach(id -> {
            double[] rating = ratings.getOrDefault(id, new double[3]);
            result.put(id, new Metric(total.getOrDefault(id, 0L), recent.getOrDefault(id, 0L), rating[1],
                    rating[0] == 0 ? 0 : 100 * rating[2] / rating[0], (long) rating[0]));
        });
        return result;
    }

    public record Metric(long totalSold, long recentSold, double score, double positiveRate, long reviewCount) {
    }

    // Explicit public fields only: never serialize JPA accounts, passwords, phone numbers or order addresses.
    public record Card(Long id, String name, String category, BigDecimal price, BigDecimal maxPrice, String description,
                       String condition, String imageUrl, String status, String statusLabel, int stock, Long sellerId,
                       String seller, String sellerCity, long sold, long recentSold, double score, double rating, long reviewCount,
                       Double distance, boolean liked, boolean negotiable, String city, String tradeTags, Instant publishedAt) {
    }
}
