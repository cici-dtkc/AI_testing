package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;

public class OrderReturnImage {
    private int id;
    private int orderReturnId;
    @ColumnName("url_img")
    private String urlImg;
    private LocalDateTime createdAt;

    // Constructor mặc định
    public OrderReturnImage() {}

    // Constructor đầy đủ
    public OrderReturnImage(int id, int orderReturnId, String urlImg, LocalDateTime createdAt) {
        this.id = id;
        this.orderReturnId = orderReturnId;
        this.urlImg = urlImg;
        this.createdAt = createdAt;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderReturnId() {
        return orderReturnId;
    }

    public void setOrderReturnId(int orderReturnId) {
        this.orderReturnId = orderReturnId;
    }

    public String getUrlImg() {
        return urlImg;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "OrderReturnImage{" +
                "id=" + id +
                ", orderReturnId=" + orderReturnId +
                ", urlImg='" + urlImg + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
