package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDateTime;

public class Comic {
    private int id;

    @ColumnName("name_comics")
    private String nameComics;
    private String author;
    private String publisher;

    @ColumnName("description")
    private String description;
    private double price;

    @ColumnName("stock_quantity")
    private int stockQuantity;
    private String status;

    @ColumnName("thumbnail_url")
    private String thumbnailUrl;

    @ColumnName("category_id")
    private Integer categoryId;
    private Integer volume;

    @ColumnName("series_id")
    private Integer seriesId;

    @ColumnName("is_deleted")
    private boolean isDeleted;

    @ColumnName("deleted_at")
    private LocalDateTime deletedAt;

    @ColumnName("created_at")
    private LocalDateTime createdAt;

    @ColumnName("updated_at")
    private LocalDateTime updatedAt;
    @ColumnName("totalSold")
    private int totalSold; // lượt bán trong tuần
    @ColumnName("discount_percent")
    private Double discountPercent;
    @ColumnName("series_name")
    private String seriesName;
    @ColumnName("name_categories")
    private String categoryName;

    private String authorName;

    private String publisherName;
    @ColumnName("is_hidden")
    private int isHidden;

    @ColumnName("flash_sale_id")
    private Integer flashSaleId;

    @ColumnName("flash_sale_name")
    private String flashSaleName;

    @ColumnName("flash_sale_discount")
    private Double flashSaleDiscount;

    @ColumnName("flash_sale_price")
    private Double flashSalePrice;

    @ColumnName("has_flash_sale")
    private boolean hasFlashSale;
    @ColumnName("damaged_quantity")
    private Integer damagedQuantity;

    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }

    public Integer getFlashSaleId() {
        return flashSaleId;
    }

    public void setFlashSaleId(Integer flashSaleId) {
        this.flashSaleId = flashSaleId;
    }

    public String getFlashSaleName() {
        return flashSaleName;
    }

    public void setFlashSaleName(String flashSaleName) {
        this.flashSaleName = flashSaleName;
    }

    public Double getFlashSaleDiscount() {
        return flashSaleDiscount;
    }

    public void setFlashSaleDiscount(Double flashSaleDiscount) {
        this.flashSaleDiscount = flashSaleDiscount;
    }

    public Double getFlashSalePrice() {
        return flashSalePrice;
    }

    public void setFlashSalePrice(Double flashSalePrice) {
        this.flashSalePrice = flashSalePrice;
    }

    public boolean isHasFlashSale() {
        return hasFlashSale;
    }

    public void setHasFlashSale(boolean hasFlashSale) {
        this.hasFlashSale = hasFlashSale;
    }

    public Comic() {
    }

    public int getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(int isHidden) {
        this.isHidden = isHidden;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
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

    public String getNameComics() {
        return nameComics;
    }

    public void setNameComics(String nameComics) {
        this.nameComics = nameComics;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Integer seriesId) {
        this.seriesId = seriesId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public int getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(int totalSold) {
        this.totalSold = totalSold;
    }


    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getDiscountPrice() {
        if (discountPercent == null || discountPercent == 0) {
            return price;
        }
        return price * (100.0 - discountPercent) / 100.0;
    }

    public boolean hasDiscount() {
        return discountPercent != null && discountPercent > 0;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    /**
     * lay gia cuoi cung
     *
     * @return
     */
    public double getFinalPrice() {
        if (hasFlashSale && flashSalePrice != null) {
            return flashSalePrice;
        }
        return getDiscountPrice();
    }

    /**
     * lay phan tram giam gia cuoi cung
     */
    public double getFinalDiscountPercent() {
        if (hasFlashSale && flashSaleDiscount != null) {
            return flashSaleDiscount;
        }
        if (discountPercent != null) {
            return discountPercent;
        }
        return 0;
    }

    /**
     * Kiểm tra có giảm giá không (Flash Sale hoặc discount thường)
     */

    public boolean hasAnyDiscount() {
        return hasFlashSale || hasDiscount();
    }


}