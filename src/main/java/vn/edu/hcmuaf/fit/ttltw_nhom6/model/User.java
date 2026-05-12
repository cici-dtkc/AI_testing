package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class User implements Serializable {

    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String gender;
    private String phone;
    private String fullName;
    private LocalDate birthdate;
    private String address;
    private String avatarUrl;
    private String role;
    private int points;
    private BigDecimal totalSpent;
    private String membershipLevel;
    @ColumnName("is_active")
    private boolean isActive;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @ColumnName("failed_login_attempts")
    private Integer failedLoginAttempts;
    @ColumnName("locked_until")
    private LocalDateTime lockedUntil;

    public User() {
    }

    public User(int id, String username, String email, String passwordHash,
                String gender, String phone, String fullName, LocalDate birthdate,
                String address, String avatarUrl, String role, int points,
                BigDecimal totalSpent, String membershipLevel, boolean isActive,
                boolean isDeleted, LocalDateTime deletedAt, LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.gender = gender;
        this.phone = phone;
        this.fullName = fullName;
        this.birthdate = birthdate;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.points = points;
        this.totalSpent = totalSpent;
        this.membershipLevel = membershipLevel;
        this.isActive = isActive;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getMembershipLevel() {
        return membershipLevel;
    }

    public void setMembershipLevel(String membershipLevel) {
        this.membershipLevel = membershipLevel;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {  // ✅ Đổi tên parameter
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;  // Đã sửa đúng
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString để in ra đẹp khi test
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", birthdate=" + birthdate +
                ", isActive=" + isActive +
                ", points=" + points +
                ", totalSpent=" + totalSpent +
                ", createdAt=" + createdAt +
                '}';
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isTemporarilyLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

}