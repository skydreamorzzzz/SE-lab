package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

@jakarta.persistence.Table(uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"ownerId", "kind", "targetKey"}))
@Entity

public class PersonalEntry {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "ownerId", insertable = false, updatable = false)
    private Account ownerIdReference;
    @Id
    @GeneratedValue
    private Long id;
    private Long ownerId;
    @Column(length = 2000)
    private String kind;
    @Column(length = 2000)
    private String targetKey;
    private Long targetId;
    private int quantity;
    @Column(length = 2000)
    private String groupName;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long value) {
        ownerId = value;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String value) {
        kind = value;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String value) {
        targetKey = value;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long value) {
        targetId = value;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int value) {
        quantity = value;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String value) {
        groupName = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        updatedAt = value;
    }
}
