package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
@jakarta.persistence.Table(indexes = {@jakarta.persistence.Index(name = "idx_reservation_queue", columnList = "productId,cancelled,confirmed,id")})
public class Reservation {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "buyerId", insertable = false, updatable = false)
    private Account buyerIdReference;
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
    private int quantity;
    private int allocated;
    private boolean cancelled;
    private boolean confirmed;
    private Instant createdAt;

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int value) {
        quantity = value;
    }

    public int getAllocated() {
        return allocated;
    }

    public void setAllocated(int value) {
        allocated = value;
    }

    public boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }

    public boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean value) {
        confirmed = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }
}
