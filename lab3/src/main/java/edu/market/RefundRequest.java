package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity

public class RefundRequest {
    @Id
    @GeneratedValue
    private Long id;
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
