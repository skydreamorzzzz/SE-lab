package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
public class Product {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean negotiable;
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long listingRevision;
    @Id
    @GeneratedValue
    private Long id;
    private Long sellerId;
    private String name;
    private String category;
    private String conditionLabel;
    @Column(length = 2000)
    private String description;
    private String imageUrl;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int stock;
    private int reservationLimit;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private String reviewNote;

    public Long getId() {
        return id;
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

    public String getConditionLabel() {
        return conditionLabel;
    }

    public void setConditionLabel(String value) {
        conditionLabel = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        description = value;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String value) {
        imageUrl = value;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal value) {
        minPrice = value;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal value) {
        maxPrice = value;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int value) {
        stock = value;
    }

    public int getReservationLimit() {
        return reservationLimit;
    }

    public void setReservationLimit(int value) {
        reservationLimit = value;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus value) {
        status = value;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String value) {
        reviewNote = value;
    }

    public boolean getNegotiable() {
        return negotiable;
    }

    public void setNegotiable(boolean value) {
        negotiable = value;
    }

    public long getListingRevision() {
        return listingRevision;
    }

    public void setListingRevision(long value) {
        listingRevision = value;
    }
}
