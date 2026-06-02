package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;

public class UserPermissonOverride {
    private int id;
    @ColumnName("user_id")
    private int userId;
    @ColumnName("permission_id")
    private int permissionId;
    @ColumnName("is_granted")
    private boolean isGranted;
    @ColumnName("created_at")
    private LocalDateTime createdAt;
    @ColumnName("permission_name")
    private String permissionName;

    public UserPermissonOverride() {
    }

    public UserPermissonOverride(int userId, int permissionId, boolean isGranted) {
        this.userId = userId;
        this.permissionId = permissionId;
        this.isGranted = isGranted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public boolean isGranted() {
        return isGranted;
    }

    public void setGranted(boolean granted) {
        isGranted = granted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
}
