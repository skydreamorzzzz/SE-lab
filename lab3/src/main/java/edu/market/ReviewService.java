package edu.market;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {
    private final ActivityService activities;
    private final AccountService accounts;
    private final TradingService trading;
    private final PurchaseOrderRepository orders;
    private final ReviewRepository reviews;

    public ReviewService(AccountService accounts, TradingService trading,
                         PurchaseOrderRepository orders, ReviewRepository reviews, ActivityService activities) {
        this.accounts = accounts;
        this.activities = activities;
        this.trading = trading;
        this.orders = orders;
        this.reviews = reviews;
    }

    public Review submit(String username, Long orderId, int rating, String content) {
        trading.detail(username, orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        Account author = accounts.current(username);
        BusinessException.require(order.getStatus() == OrderStatus.COMPLETED, "交易完成后才可评价");
        BusinessException.require(rating >= 1 && rating <= 5, "评分范围为 1–5");
        BusinessException.require(content != null && !content.isBlank() && content.length() <= 1000,
                "评价内容为 1–1000 字");
        BusinessException.require(!reviews.existsByOrderIdAndAuthorId(orderId, author.getId()), "每方只能评价一次");
        Review review = new Review();
        review.setOrderId(orderId);
        review.setProductId(order.getProductId());
        review.setAuthorId(author.getId());
        review.setRecipientId(author.getId().equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId());
        review.setRating(rating);
        review.setContent(content.strip());
        review.setCreatedAt(Instant.now());
        Review saved = reviews.save(review);
        activities.record(username, "REVIEW", saved.getId(), "SUBMIT", "PUBLISHED");
        return saved;
    }
}
