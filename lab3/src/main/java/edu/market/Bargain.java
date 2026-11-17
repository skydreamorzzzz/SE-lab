package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@jakarta.persistence.Table(indexes = {@jakarta.persistence.Index(name = "idx_bargain_expiry", columnList = "status,expiresAt")})

public class Bargain {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "buyerId", insertable = false, updatable = false)
    private Account buyerIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "sellerId", insertable = false, updatable = false)
    private Account sellerIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product productIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "orderId", insertable = false, updatable = false)
    private PurchaseOrder orderIdReference;
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private int quantity;
    private BigDecimal price;
    @Column(length = 2000)
    private String status;
    private Instant createdAt;
    private Instant expiresAt;
    private long listingRevision;
    private Long orderId;

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long value) {
        productId = value;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long value) {
        buyerId = value;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long value) {
        sellerId = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int value) {
        quantity = value;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal value) {
        price = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant value) {
        expiresAt = value;
    }

    public long getListingRevision() {
        return listingRevision;
    }

    public void setListingRevision(long value) {
        listingRevision = value;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long value) {
        orderId = value;
    }
}
