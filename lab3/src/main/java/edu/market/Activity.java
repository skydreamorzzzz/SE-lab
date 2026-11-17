package edu.market;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Activity {
    @Id
    @GeneratedValue
    private Long id;
    private Instant occurredAt;
    private String actor;
    private String objectType;
    private Long objectId;
    private String operation;
    @Column(length = 500)
    private String outcome;

    protected Activity() {
    }

    public Activity(String actor, String objectType, Long objectId, String operation, String outcome) {
        occurredAt = Instant.now();
        this.actor = actor;
        this.objectType = objectType;
        this.objectId = objectId;
        this.operation = operation;
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public String getObjectType() {
        return objectType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public String getOperation() {
        return operation;
    }

    public String getOutcome() {
        return outcome;
    }
}
