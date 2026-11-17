package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@jakarta.persistence.Table(indexes = {@jakarta.persistence.Index(name = "idx_product_visible", columnList = "status,publishedAt,id"), @jakarta.persistence.Index(name = "idx_product_seller", columnList = "sellerId"), @jakarta.persistence.Index(name = "idx_product_category", columnList = "category,status")})
public class Product {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean negotiable;
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long listingRevision;
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long favoriteCount;
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long soldQuantity;
    @Column(nullable = false, columnDefinition = "double default 0")
    private double ratingScore;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int conditionLevel;
    private java.time.Instant publishedAt;
    @Column(length = 2000)
    private String extraImages;
    private String videoUrl;
    private String brand;
    private String modelName;
    private String usageYears;
    private String originPlace;
    private String contactChannel;
    private String shippingAddress;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean returnSupported;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "sellerId", insertable = false, updatable = false)
    private Account sellerIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @jakarta.persistence.Basic(optional = false)
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

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(long value) {
        favoriteCount = value;
    }

    public long getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(long value) {
        soldQuantity = value;
    }

    public double getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(double value) {
        ratingScore = value;
    }

    public int getConditionLevel() {
        return conditionLevel;
    }

    public void setConditionLevel(int value) {
        conditionLevel = value;
    }

    public java.time.Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(java.time.Instant value) {
        publishedAt = value;
    }

    public String getExtraImages() {
        return extraImages;
    }

    public void setExtraImages(String value) {
        extraImages = value;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String value) {
        videoUrl = value;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String value) {
        brand = value;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String value) {
        modelName = value;
    }

    public String getUsageYears() {
        return usageYears;
    }

    public void setUsageYears(String value) {
        usageYears = value;
    }

    public String getOriginPlace() {
        return originPlace;
    }

    public void setOriginPlace(String value) {
        originPlace = value;
    }

    public String getContactChannel() {
        return contactChannel;
    }

    public void setContactChannel(String value) {
        contactChannel = value;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String value) {
        shippingAddress = value;
    }

    public boolean getReturnSupported() {
        return returnSupported;
    }

    public void setReturnSupported(boolean value) {
        returnSupported = value;
    }
}
