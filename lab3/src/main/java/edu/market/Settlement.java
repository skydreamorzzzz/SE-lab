package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity

public class Settlement {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "orderId", insertable = false, updatable = false)
    private PurchaseOrder orderIdReference;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "sellerId", insertable = false, updatable = false)
    private Account sellerIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private Long orderId;
    private Long sellerId;
    private BigDecimal grossAmount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private Instant createdAt;
    private boolean reversed;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long value) {
        orderId = value;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long value) {
        sellerId = value;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal value) {
        grossAmount = value;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal value) {
        fee = value;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal value) {
        netAmount = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public boolean getReversed() {
        return reversed;
    }

    public void setReversed(boolean value) {
        reversed = value;
    }
}
