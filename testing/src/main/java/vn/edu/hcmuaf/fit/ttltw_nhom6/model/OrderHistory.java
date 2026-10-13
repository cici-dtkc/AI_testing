package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;

public class OrderHistory {
    private int id;

    @ColumnName("order_id")
    private int orderId;

    @ColumnName("status_from")
    private String statusFrom;

    @ColumnName("status_to")
    private String statusTo;

    @ColumnName("changed_by")
    private String changedBy;

    private String reason;

    @ColumnName("changed_at")
    private LocalDateTime changedAt;

    // Constructors
    public OrderHistory() {}

    public OrderHistory(int orderId, String statusFrom, String statusTo, String changedBy, String reason) {
        this.orderId = orderId;
        this.statusFrom = statusFrom;
        this.statusTo = statusTo;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getStatusFrom() {
        return statusFrom;
    }

    public void setStatusFrom(String statusFrom) {
        this.statusFrom = statusFrom;
    }

    public String getStatusTo() {
        return statusTo;
    }

    public void setStatusTo(String statusTo) {
        this.statusTo = statusTo;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
