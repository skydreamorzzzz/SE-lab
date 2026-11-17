package edu.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean superAdmin;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean banned;
    @Column(unique = true)
    private String phone;
    private String recoveryHash;
    private String phoneChallengeHash;
    private String pendingPhone;
    private java.time.Instant phoneChallengeExpires;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int phoneAttempts;
    private String city;
    private String bio;
    private String avatarUrl;
    private java.time.Instant createdAt = java.time.Instant.now();
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String role;
    private String displayName;
    private String address;

    protected Account() {
    }

    public Account(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAddress() {
        return address;
    }

    public void updateProfile(String name, String deliveryAddress) {
        displayName = name;
        address = deliveryAddress;
    }

    public void changePassword(String hash) {
        passwordHash = hash;
    }

    public boolean getSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean value) {
        superAdmin = value;
    }

    public boolean getBanned() {
        return banned;
    }

    public void setBanned(boolean value) {
        banned = value;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String value) {
        phone = value;
    }

    public String getRecoveryHash() {
        return recoveryHash;
    }

    public void setRecoveryHash(String value) {
        recoveryHash = value;
    }

    public String getPhoneChallengeHash() {
        return phoneChallengeHash;
    }

    public void setPhoneChallengeHash(String value) {
        phoneChallengeHash = value;
    }

    public String getPendingPhone() {
        return pendingPhone;
    }

    public void setPendingPhone(String value) {
        pendingPhone = value;
    }

    public java.time.Instant getPhoneChallengeExpires() {
        return phoneChallengeExpires;
    }

    public void setPhoneChallengeExpires(java.time.Instant value) {
        phoneChallengeExpires = value;
    }

    public int getPhoneAttempts() {
        return phoneAttempts;
    }

    public void setPhoneAttempts(int value) {
        phoneAttempts = value;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        city = value;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String value) {
        bio = value;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String value) {
        avatarUrl = value;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant value) {
        createdAt = value;
    }
    public java.util.Set<String> getRoles() {
        return role.equals("ADMIN") ? java.util.Set.of("ADMIN") : java.util.Set.of("BUYER", "SELLER");
    }

    public boolean getTrader() {
        return !role.equals("ADMIN");
    }
}
