package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import com.google.gson.annotations.SerializedName;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Permission {
    private int id;
    @ColumnName("permission_name")
    private String permissionName;
    private String description;
    private boolean fromRole;
    private String overrideStatus;

    public Permission() {
    }

    public Permission(int id, String permissionName, String description) {
        this.id = id;
        this.permissionName = permissionName;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public boolean isFromRole() {
        return fromRole;
    }

    public void setFromRole(boolean fromRole) {
        this.fromRole = fromRole;
    }

    public String getOverrideStatus() {
        return overrideStatus;
    }

    public void setOverrideStatus(String overrideStatus) {
        this.overrideStatus = overrideStatus;
    }
}
