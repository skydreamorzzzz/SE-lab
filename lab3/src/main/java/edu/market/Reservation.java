package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
public class Reservation {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
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
