package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cart {
    private int id;
    private Integer userId;         // NULL nếu là guest
    private String sessionId;       // dùng cho guest
    private String status;          // 'active' , 'checked_out' , 'abandoned'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Map<Integer, CartItem> data;

    // Constructor uest hoặc user
    public Cart(Integer userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.status = "active";
        this.data = new HashMap<>();
    }

    public Cart() {
        this.data = new HashMap<>();
    }

    public Cart(int id, Integer userId, String sessionId, String status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.data = new HashMap<>();
    }

    /**
     * Thêm item không có flash sale
     */
    public void addItem(Comic comic, int quantity) {
        addItem(comic, quantity, null, null);
    }

    /**
     * Thêm item có flash sale
     */
    public void addItem(Comic comic, int quantity, Integer flashSaleId, Double flashSalePrice) {
        if (quantity <= 0) quantity = 1;

        CartItem existing = get(comic.getId());

        if (existing != null) {
            existing.updateQuantity(quantity);

            if (flashSaleId != null && flashSalePrice != null) {
                existing.applyFlashSale(flashSaleId, flashSalePrice);
            } else if (!existing.isInFlashSale()) {
                // giữ nguyên appliedPrice theo giá discount thường
                existing.setAppliedPrice(comic.getDiscountPrice());
            }
        } else {
            CartItem newItem = (flashSaleId != null && flashSalePrice != null)
                    ? new CartItem(comic, quantity, flashSaleId, flashSalePrice)
                    : new CartItem(comic, quantity);
            data.put(comic.getId(), newItem);
        }
    }

    public boolean updateItem(int comicId, int quantity) {
        CartItem item = get(comicId);
        if (item == null) return false;
        item.setQuantity(Math.max(quantity, 1));
        return true;
    }

    public CartItem removeItem(int comicId) {
        if (this.data == null) return null;
        return data.remove(comicId);
    }

    public List<CartItem> removeAllItems() {
        if (this.data == null) this.data = new HashMap<>();
        List<CartItem> all = new ArrayList<>(data.values());
        data.clear();
        return all;
    }

    public void setItems(List<CartItem> items) {
        if (this.data == null) this.data = new HashMap<>();
        data.clear();
        if (items != null) {
            items.forEach(item -> data.put(item.getComic().getId(), item));
        }
    }

    public List<CartItem> getItems() {
        if (this.data == null) this.data = new HashMap<>();
        return new ArrayList<>(data.values());
    }

    public CartItem get(int comicId) {
        if (this.data == null) this.data = new HashMap<>();
        return data.get(comicId);
    }

    public int totalQuantity() {
        if (this.data == null) return 0;
        return data.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double total() {
        if (this.data == null) return 0.0;
        return data.values().stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}