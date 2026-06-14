package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order {
    private int id;
    @ColumnName("user_id")
    private int userId;
    @ColumnName("order_date")
    private LocalDateTime orderDate;
    private String status;
    @ColumnName("total_amount")
    private double totalAmount;
    @ColumnName("shipping_address_id")
    private int shippingAddressId;
    @ColumnName("recipient_name")
    private String recipientName;
    @ColumnName("shipping_phone")
    private String shippingPhone;
    @ColumnName("shipping_address")
    private String shippingAddress;
    @ColumnName("shipping_provider")
    private String shippingProvider;
    @ColumnName("shipping_fee")
    private double shippingFee;
    @ColumnName("points_used")
    private int pointUsed;
    @ColumnName("created_at")
    private LocalDateTime createdAt;
    @ColumnName("tracking_code")
    private String trackingCode;
    @ColumnName("confirmed_at")
    private LocalDateTime confirmedAt;
    @ColumnName("shipping_at")
    private LocalDateTime shippingAt;
    @ColumnName("delivered_at")
    private LocalDateTime deliveredAt;
    @ColumnName("completed_at")
    private LocalDateTime completedAt;
    @ColumnName("cancelled_at")
    private LocalDateTime cancelledAt;
    @ColumnName("returned_at")
    private LocalDateTime returnedAt;
    @ColumnName("product_discount")
    private double productDiscount;
    @ColumnName("shipping_discount")
    private double shippingDiscount;
    @ColumnName("user_name")
    private String userName;
    private Double averageRating;
    private Integer reviewCount;



    public Order() {}

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public int getPointUsed() {
        return pointUsed;
    }

    public void setPointUsed(int pointUsed) {
        this.pointUsed = pointUsed;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public int getShippingAddressId() {
        return shippingAddressId;
    }

    public void setShippingAddressId(int shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(String shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public LocalDateTime getShippingAt() {
        return shippingAt;
    }

    public void setShippingAt(LocalDateTime shippingAt) {
        this.shippingAt = shippingAt;
    }

    public double getProductDiscount() {
        return productDiscount;
    }

    public void setProductDiscount(double productDiscount) {
        this.productDiscount = productDiscount;
    }

    public double getShippingDiscount() {
        return shippingDiscount;
    }

    public void setShippingDiscount(double shippingDiscount) {
        this.shippingDiscount = shippingDiscount;
    }

    public String getOrderDateFormatted() {
        return orderDate != null
                ? orderDate.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }
    public String getConfirmedAtFormatted() {
        return confirmedAt != null
                ? confirmedAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }
    public String getShippingAtFormatted() {
        return shippingAt != null
                ? shippingAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }
    public String getCompletedAtFormatted() {
        return completedAt != null
                ? completedAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }
    public String getCancelledAtFormatted() {
        return cancelledAt != null
                ? cancelledAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }
    public String getReturnedAtFormatted() {
        return returnedAt != null
                ? returnedAt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : "—";
    }

    public double getOriginalItemTotal() {
        return totalAmount - shippingFee + shippingDiscount + productDiscount + pointUsed;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    public boolean getHasReview() {
        return reviewCount != null && reviewCount > 0;
    }

    public int getFullStars() {
        if (averageRating == null) return 0;
        return (int) averageRating.doubleValue();
    }

    public boolean getHasHalfStar() {
        if (averageRating == null) return false;

        double decimal = averageRating - averageRating.intValue();
        return decimal >= 0.5;
    }

    public int getEmptyStars() {
        return 5 - getFullStars() - (getHasHalfStar() ? 1 : 0);
    }

    public String getFormattedRating() {
        if (averageRating == null) return "0.0";
        return String.format("%.1f", averageRating);
    }
}
