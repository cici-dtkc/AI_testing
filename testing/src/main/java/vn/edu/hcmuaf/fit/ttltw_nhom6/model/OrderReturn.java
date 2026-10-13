package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import java.time.LocalDateTime;

public class OrderReturn {
    private int id;
    private int orderId;
    private int comicId;
    private int quantity;
    private String reason;
    private double refundAmount;
    private LocalDateTime createdAt;
    private String status;

    // Constructor mặc định
    public OrderReturn() {}

    // Constructor đầy đủ
    public OrderReturn(int id, int orderId, int comicId, int quantity, String reason,
                       double refundAmount, LocalDateTime createdAt, String status) {
        this.id = id;
        this.orderId = orderId;
        this.comicId = comicId;
        this.quantity = quantity;
        this.reason = reason;
        this.refundAmount = refundAmount;
        this.createdAt = createdAt;
        this.status = status;
    }

    // Getter & Setter
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

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OrderReturn{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", comicId=" + comicId +
                ", quantity=" + quantity +
                ", reason='" + reason + '\'' +
                ", refundAmount=" + refundAmount +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }
}
