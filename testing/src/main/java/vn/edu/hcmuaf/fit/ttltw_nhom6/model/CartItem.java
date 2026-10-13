package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import java.time.LocalDateTime;

public class CartItem {
    private int            id;
    private int            cartId;
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;
    private Comic   comic;
    private int     quantity;
    private Double  priceAtPurchase;
    private Double  flashSalePrice;
    private Integer flashSaleId;

    public CartItem(Comic comic, int quantity) {
        this.comic           = comic;
        this.quantity        = quantity;
        this.priceAtPurchase = comic.getDiscountPrice();
        this.flashSaleId     = null;
        this.flashSalePrice  = null;
    }
    public CartItem(int id, int cartId, Comic comic, int quantity,
                    Double priceAtPurchase, Double flashSalePrice,
                    Integer flashSaleId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id             = id;
        this.cartId         = cartId;
        this.comic          = comic;
        this.quantity       = quantity;
        this.priceAtPurchase= priceAtPurchase;
        this.flashSalePrice = flashSalePrice;
        this.flashSaleId    = flashSaleId;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
    }
    public CartItem(Comic comic, int quantity, Integer flashSaleId, Double flashSalePrice) {
        this.comic          = comic;
        this.quantity       = quantity;
        this.flashSaleId    = flashSaleId;
        this.flashSalePrice = flashSalePrice;
        this.priceAtPurchase = comic.getPrice();
    }

    public CartItem(Comic comic, int quantity, Double priceAtPurchase,
                    Integer flashSaleId, Double flashSalePrice) {
        this.comic           = comic;
        this.quantity        = quantity;
        this.priceAtPurchase = priceAtPurchase;
        this.flashSaleId     = flashSaleId;
        this.flashSalePrice  = flashSalePrice;
    }

    public boolean isInFlashSale() {
        return flashSaleId != null && flashSalePrice != null;
    }
    public double getFinalPrice() {
        if (flashSalePrice != null) return flashSalePrice;
        if (priceAtPurchase != null) return priceAtPurchase;
        return comic.getDiscountPrice();
    }
    public double getTotalPrice() {
        return getFinalPrice() * quantity;
    }
    public double getSubtotal() {
        return getTotalPrice();
    }
    public void updateQuantity(int additionalQuantity) {
        this.quantity += additionalQuantity;
    }
    public void applyFlashSale(Integer flashSaleId, Double flashSalePrice) {
        this.flashSaleId    = flashSaleId;
        this.flashSalePrice = flashSalePrice;
    }
    public void removeFlashSale() {
        this.flashSaleId    = null;
        this.flashSalePrice = null;
    }

    public void updateFlashSale(Integer flashSaleId, Double flashSalePrice) {
        applyFlashSale(flashSaleId, flashSalePrice);
    }

    public double getAppliedPrice() {
        return getFinalPrice();
    }

    public void setAppliedPrice(double price) {
        this.priceAtPurchase = price;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }

    public Comic getComic() { return comic; }
    public void setComic(Comic comic) { this.comic = comic; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }

    public Double getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(Double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }

    public Double getFlashSalePrice() { return flashSalePrice; }
    public void setFlashSalePrice(Double flashSalePrice) {
        this.flashSalePrice = flashSalePrice;
    }

    public Integer getFlashSaleId() { return flashSaleId; }
    public void setFlashSaleId(Integer flashSaleId) {
        this.flashSaleId = flashSaleId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}