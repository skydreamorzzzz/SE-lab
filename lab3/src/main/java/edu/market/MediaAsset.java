package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;

@Entity

public class MediaAsset {
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "ownerId", insertable = false, updatable = false)
    private Account ownerIdReference;
    @Id
    @GeneratedValue
    private Long id;
    @jakarta.persistence.Basic(optional = false)
    private Long ownerId;
    @Column(length = 2000)
    private String storedName;
    @Column(length = 2000)
    private String contentType;
    private long byteSize;

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long value) {
        ownerId = value;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String value) {
        storedName = value;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String value) {
        contentType = value;
    }

    public long getByteSize() {
        return byteSize;
    }

    public void setByteSize(long value) {
        byteSize = value;
    }
}
