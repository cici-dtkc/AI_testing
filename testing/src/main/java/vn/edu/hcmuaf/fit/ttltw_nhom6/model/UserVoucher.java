package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import com.google.gson.annotations.SerializedName;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserVoucher implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    @ColumnName("user_id")
    private int userId;

    @SerializedName("promo_id")
    @ColumnName("promo_id")
    private int promoId;

    @SerializedName("code")
    private String code;

    @SerializedName("acquired_at")
    @ColumnName("acquired_at")
    private LocalDateTime acquiredAt;

    @SerializedName("used_at")
    @ColumnName("used_at")
    private LocalDateTime usedAt;

    @SerializedName("used_order_id")
    @ColumnName("used_order_id")
    private Integer usedOrderId;

    @SerializedName("is_active")
    @ColumnName("is_active")
    private boolean isActive;

    @SerializedName("is_used")
    @ColumnName("is_used")
    private boolean isUsed;

    public UserVoucher() {
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


    public int getPromoId() {
        return promoId;
    }

    public void setPromoId(int promoId) {
        this.promoId = promoId;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public LocalDateTime getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(LocalDateTime acquiredAt) {
        this.acquiredAt = acquiredAt;
    }


    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }


    public Integer getUsedOrderId() {
        return usedOrderId;
    }

    public void setUsedOrderId(Integer usedOrderId) {
        this.usedOrderId = usedOrderId;
    }


    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }


    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}