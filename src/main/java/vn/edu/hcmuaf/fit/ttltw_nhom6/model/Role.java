package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Role {
    @ColumnName("id")
    private int id;
    @ColumnName("role_name")
    private String roleName;
    @ColumnName("description")
    private String description;
    @ColumnName("level")
    private int level;

    public Role() {
    }

    public Role(int id, String roleName, String description) {
        this.id = id;
        this.roleName = roleName;
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

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
