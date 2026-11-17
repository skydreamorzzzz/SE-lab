package edu.market;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public record CatalogFilter(String condition, Double latitude, Double longitude, Integer radius,
                            int minSold, int minPositive) {
    public static CatalogFilter none() {
        return new CatalogFilter("all", null, null, null, 0, 0);
    }

    public Specification<Product> specification() {
        Geo.validate(latitude, longitude);
        BusinessException.require(minSold >= 0 && minSold <= 1000000 && minPositive >= 0 && minPositive <= 100,
                "销量或好评率条件无效");
        BusinessException.require(radius == null || (latitude != null && radius > 0 && radius <= 100),
                "附近检索需先设置浏览位置，范围为1–100km");
        BusinessException.require(condition == null || List.of("all", "全新", "近全新", "9成新", "8成新", "7成新").contains(condition),
                "成色条件无效");
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (condition != null && !condition.equals("all")) {
                if (condition.equals("7成新")) {
                    predicates.add(cb.between(root.get("conditionLevel"), 1, 70));
                } else {
                    predicates.add(cb.equal(root.get("conditionLabel"), condition));
                }
            }
            if (minSold > 0) {
                var sold = query.subquery(Long.class);
                var order = sold.from(PurchaseOrder.class);
                sold.select(cb.sumAsLong(order.get("quantity"))).where(cb.equal(order.get("sellerId"), root.get("sellerId")),
                        cb.equal(order.get("status"), OrderStatus.COMPLETED));
                predicates.add(cb.ge(cb.coalesce(sold, 0L), minSold));
            }
            if (minPositive > 0) {
                var total = query.subquery(Long.class);
                var reviews = total.from(Review.class);
                total.select(cb.count(reviews)).where(cb.equal(reviews.get("recipientId"), root.get("sellerId")));
                var positive = query.subquery(Long.class);
                var good = positive.from(Review.class);
                positive.select(cb.count(good)).where(cb.equal(good.get("recipientId"), root.get("sellerId")),
                        cb.ge(good.get("rating"), 4));
                predicates.add(cb.gt(total, 0L));
                predicates.add(cb.ge(cb.prod(positive, 100), cb.prod(total, minPositive)));
            }
            if (radius != null) {
                double lat = Math.toRadians(latitude);
                var targetLat = cb.function("radians", Double.class, root.get("latitude"));
                var longitudeDelta = cb.function("radians", Double.class, cb.diff(root.get("longitude"), longitude));
                var cosine = cb.sum(cb.prod(Math.sin(lat), cb.function("sin", Double.class, targetLat)),
                        cb.prod(cb.prod(Math.cos(lat), cb.function("cos", Double.class, targetLat)),
                                cb.function("cos", Double.class, longitudeDelta)));
                var clamped = cb.function("least", Double.class, cb.literal(1.0),
                        cb.function("greatest", Double.class, cb.literal(-1.0), cosine));
                var distance = cb.prod(6371, cb.function("acos", Double.class, clamped));
                predicates.add(cb.isNotNull(root.get("latitude")));
                predicates.add(cb.le(distance, radius));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
