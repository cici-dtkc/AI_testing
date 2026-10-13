package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;

public class Series {
    private int id;
    @ColumnName("series_name")
    private String seriesName;
    private String description;
    @ColumnName("cover_url")
    private String coverUrl;
    @ColumnName("total_volumes")
    private int totalVolumes;
    private String status;
    @ColumnName("is_deleted")
    private boolean isDeleted;
    @ColumnName("is_hidden")
    private boolean isHidden;
    @ColumnName("deleted_at")
    private LocalDateTime deletedAt;
    @ColumnName("created_at")
    private LocalDateTime createdAt;
    @ColumnName("updated_at")
    private LocalDateTime updatedAt;

    public Series() {

    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalVolumes() {
        return totalVolumes;
    }

    public void setTotalVolumes(int totalVolumes) {
        this.totalVolumes = totalVolumes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isHidden() {
        return isHidden;
    }
    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}
