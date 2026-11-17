package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@jakarta.persistence.Table(indexes = {@jakarta.persistence.Index(name = "idx_refund_timeout", columnList = "status,createdAt")})

public class RefundRequest {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "orderId", insertable = false, updatable = false)
    private PurchaseOrder orderIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @jakarta.persistence.Basic(optional = false)
    private Long orderId;
    @Column(length = 2000)
    private String previousStatus;
    @Column(length = 2000)
    private String status;
    @Column(length = 2000)
    private String reason;
    @Column(length = 2000)
    private String decision;
    private boolean returnRequired;
    @Column(length = 2000)
    private String returnTracking;
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

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String value) {
        previousStatus = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        reason = value;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String value) {
        decision = value;
    }

    public boolean getReturnRequired() {
        return returnRequired;
    }

    public void setReturnRequired(boolean value) {
        returnRequired = value;
    }

    public String getReturnTracking() {
        return returnTracking;
    }

    public void setReturnTracking(String value) {
        returnTracking = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }
}
