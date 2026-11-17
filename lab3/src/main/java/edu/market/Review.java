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
    @Id
    @GeneratedValue
    private Long id;
    private Long orderId;
    private Long productId;
    private Long authorId;
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
