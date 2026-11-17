package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"orderId", "authorId"}))
public class Review {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "author_id", insertable = false, updatable = false)
    private Account authorIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "recipient_id", insertable = false, updatable = false)
    private Account recipientIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product productIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "order_id", insertable = false, updatable = false)
    private PurchaseOrder orderIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @jakarta.persistence.Basic(optional = false)
    @jakarta.persistence.Column(name = "order_id")
    private Long orderId;
    @jakarta.persistence.Basic(optional = false)
    @jakarta.persistence.Column(name = "product_id")
    private Long productId;
    @jakarta.persistence.Basic(optional = false)
    @jakarta.persistence.Column(name = "author_id")
    private Long authorId;
    @jakarta.persistence.Basic(optional = false)
    @jakarta.persistence.Column(name = "recipient_id")
    private Long recipientId;
    private int rating;
    @Column(length = 2000)
    private String content;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long value) {
        orderId = value;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long value) {
        productId = value;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long value) {
        authorId = value;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long value) {
        recipientId = value;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int value) {
        rating = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        content = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }
}
