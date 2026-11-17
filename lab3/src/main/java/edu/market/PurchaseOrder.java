package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@jakarta.persistence.Table(indexes = {@jakarta.persistence.Index(name = "idx_order_timeout", columnList = "status,createdAt"), @jakarta.persistence.Index(name = "idx_order_buyer", columnList = "buyerId"), @jakarta.persistence.Index(name = "idx_order_seller", columnList = "sellerId"), @jakarta.persistence.Index(name = "idx_order_rank", columnList = "status,completedAt,productId")})
public class PurchaseOrder {
    private java.time.Instant completedAt;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "buyerId", insertable = false, updatable = false)
    private Account buyerIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "sellerId", insertable = false, updatable = false)
    private Account sellerIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product productIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @jakarta.persistence.Basic(optional = false)
    private Long productId;
    @jakarta.persistence.Basic(optional = false)
    private Long buyerId;
    @jakarta.persistence.Basic(optional = false)
    private Long sellerId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private String address;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Instant createdAt;
    private Instant signedAt;
    private String paymentReference;
    private String trackingNumber;
    @Column(unique = true)
    private String requestKey;

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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String value) {
        productName = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int value) {
        quantity = value;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal value) {
        unitPrice = value;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal value) {
        total = value;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        address = value;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus value) {
        status = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant value) {
        signedAt = value;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String value) {
        paymentReference = value;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String value) {
        trackingNumber = value;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String value) {
        requestKey = value;
    }

    public java.time.Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(java.time.Instant value) {
        completedAt = value;
    }
}
