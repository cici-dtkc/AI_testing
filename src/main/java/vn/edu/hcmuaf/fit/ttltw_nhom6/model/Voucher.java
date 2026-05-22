package vn.edu.hcmuaf.fit.ttltw_nhom6.model;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Voucher implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("code")
    private String code;

    @SerializedName("discount_type")
    private String discountType;

    @SerializedName("discount_value")
    private BigDecimal discountValue;

    @SerializedName("discount_target")
    private String discountTarget;

    @SerializedName("apply_scope")
    private String applyScope;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("start_date")
    private LocalDateTime startDate;

    @SerializedName("end_date")
    private LocalDateTime endDate;

    @SerializedName("min_order_amount")
    private BigDecimal minOrderAmount;

    @SerializedName("is_single_use")
    private boolean isSingleUse;



    @SerializedName("used_count")
    private int usedCount;

    public Voucher() {
    }

    public Voucher( String code, String discountType, int usedCount, BigDecimal discountValue,
                   String discountTarget, String applyScope, int quantity, LocalDateTime startDate,
                   LocalDateTime endDate, BigDecimal minOrderAmount, boolean isSingleUse) {
        this.code = code;
        this.discountType = discountType;
        this.usedCount = usedCount;
        this.discountValue = discountValue;
        this.discountTarget = discountTarget;
        this.applyScope = applyScope;
        this.quantity = quantity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minOrderAmount = minOrderAmount;
        this.isSingleUse = isSingleUse;
    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }


    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }


    public String getDiscountTarget() {
        return discountTarget;
    }

    public void setDiscountTarget(String discountTarget) {
        this.discountTarget = discountTarget;
    }


    public String getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(String applyScope) {
        this.applyScope = applyScope;
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }


    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }


    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }


    public boolean isSingleUse() {
        return isSingleUse;
    }

    public void setSingleUse(boolean singleUse) {
        isSingleUse = singleUse;
    }


    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isExpired() {
        if (endDate == null) return false;
        return endDate.isBefore(LocalDateTime.now());
    }

    public String getEndDateFormatted() {
        if (endDate == null) return "";
        return endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}