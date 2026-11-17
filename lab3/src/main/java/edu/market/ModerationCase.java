package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import java.time.Instant;
import java.time.LocalDate;

@Entity

public class ModerationCase {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "author_id", insertable = false, updatable = false)
    private Account authorIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @Column(length = 2000)
    private String kind;
    @Column(length = 2000)
    private String targetType;
    @jakarta.persistence.Column(name = "target_id")
    private Long targetId;
    @jakarta.persistence.Basic(optional = false)
    @jakarta.persistence.Column(name = "author_id")
    private Long authorId;
    @Column(length = 2000)
    private String reason;
    @Column(length = 2000)
    private String imageUrl;
    @Column(length = 2000)
    private String status;
    @Column(length = 2000)
    private String feedback;
    private Instant createdAt;
    private LocalDate reportDate;

    public Long getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String value) {
        kind = value;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String value) {
        targetType = value;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long value) {
        targetId = value;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long value) {
        authorId = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        reason = value;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String value) {
        imageUrl = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String value) {
        feedback = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate value) {
        reportDate = value;
    }
}
