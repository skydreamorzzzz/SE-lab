package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity

public class AuditSnapshot {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Long sellerId;
    @Column(length = 2000)
    private String name;
    @Column(length = 2000)
    private String category;
    @Column(length = 2000)
    private String description;
    @Column(length = 2000)
    private String conditionLabel;
    @Column(length = 2000)
    private String imageUrl;
    private BigDecimal price;
    private int quantity;
    @Column(length = 2000)
    private String status;
    @Column(length = 2000)
    private String note;
    private Instant reviewedAt;

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long value) {
        productId = value;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long value) {
        sellerId = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        category = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public void setConditionLabel(String value) {
        conditionLabel = value;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String value) {
        imageUrl = value;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal value) {
        price = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int value) {
        quantity = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        note = value;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant value) {
        reviewedAt = value;
    }
}
