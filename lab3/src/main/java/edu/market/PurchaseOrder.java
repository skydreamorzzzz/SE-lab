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
public class PurchaseOrder {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Long buyerId;
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
}
