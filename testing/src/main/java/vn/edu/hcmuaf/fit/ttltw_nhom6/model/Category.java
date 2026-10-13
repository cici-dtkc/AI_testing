package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import java.time.LocalDateTime;

public class Category {
    private Integer id;
    private String nameCategories;
    private String description;
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private int is_hidden;

    public Category(Integer id, String nameCategories, String description, Integer isDeleted, LocalDateTime deletedAt, LocalDateTime createdAt, int is_hidden) {
        this.id = id;
        this.nameCategories = nameCategories;
        this.description = description;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.is_hidden = is_hidden;
    }


    public Category() {
    }

    public Category(Integer id, String nameCategories, String description,
                    Integer isDeleted, LocalDateTime deletedAt, LocalDateTime createdAt) {
        this.id = id;
        this.nameCategories = nameCategories;
        this.description = description;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
    }

    public Category(Integer id, String nameCategories, String description, Integer isDeleted, LocalDateTime createdAt) {
        this.id = id;
        this.nameCategories = nameCategories;
        this.description = description;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameCategories() {
        return nameCategories;
    }

    public void setNameCategories(String nameCategories) {
        this.nameCategories = nameCategories;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIsDeleted() {  // Đổi tên getter
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {  // Đổi tên setter
        this.isDeleted = isDeleted;
    }

    public boolean isDeleted() {
        return isDeleted != null && isDeleted == 1;
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

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", nameCategories='" + nameCategories + '\'' +
                ", description='" + description + '\'' +
                ", isDeleted=" + isDeleted +
                ", deletedAt=" + deletedAt +
                ", createdAt=" + createdAt +
                '}';
    }

    public int getIs_hidden() {
        return is_hidden;
    }

    public void setIs_hidden(int is_hidden) {
        this.is_hidden = is_hidden;
    }
}