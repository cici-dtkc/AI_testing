package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FlashSale {
    private int id;
    private String name;
    @ColumnName("discount_percent")
    private Double discountPercent;

    @ColumnName("start_time")
    private LocalDateTime startTime;

    @ColumnName("end_time")
    private LocalDateTime endTime;
    private String status;
    @ColumnName("created_at")
    private LocalDateTime createdAt;
    @ColumnName("is_deleted")
    private boolean isDeleted;

    @ColumnName("notified_created")
    private boolean notifiedCreated;

    @ColumnName("notified_started")
    private boolean notifiedStarted;

    public FlashSale() {
    }

    public FlashSale(String name, Double discountPercent, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.discountPercent = discountPercent;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "scheduled";
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Kiểm tra Flash Sale có đang active không
     */
    public boolean isActive() {
        return "active".equals(status);
    }

    /**
     * Kiểm tra Flash Sale có đang scheduled không
     */
    public boolean isScheduled() {
        return "scheduled".equals(status);
    }

    /**
     * Kiểm tra Flash Sale đã kết thúc chưa
     */
    public boolean isEnded() {
        return "ended".equals(status);
    }

    /**
     * Lấy label hiển thị cho status
     */
    public String getStatusLabel() {
        return switch (status) {
            case "active" -> "Đang bán";
            case "scheduled" -> "Sắp diễn ra";
            case "ended" -> "Đã kết thúc";
            default -> status;
        };
    }

    @Override
    public String toString() {
        return "FlashSale{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", discountPercent=" + discountPercent +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }

    public String getStartTimeFormatted() {
        if (startTime == null) return "";
        return startTime.format(FORMATTER);
    }

    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return endTime.format(FORMATTER);
    }

    public boolean isNotifiedCreated() { return notifiedCreated; }
    public void setNotifiedCreated(boolean notifiedCreated) { this.notifiedCreated = notifiedCreated; }

    public boolean isNotifiedStarted() { return notifiedStarted; }
    public void setNotifiedStarted(boolean notifiedStarted) { this.notifiedStarted = notifiedStarted; }
}
