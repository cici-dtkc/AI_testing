package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CartDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Cart;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.CartItem;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.cache.CacheService;

import java.util.List;
import java.util.Optional;

public class CartService {

private final CartDAO cartDAO;

    public CartService() {
        this.cartDAO = new CartDAO();
    }

    public List<CartItem> getCart(Cart cart, Integer userId, String sessionId) {

        Optional<Integer> cartIdOpt = resolveCartId(userId, sessionId, false);
        if (cartIdOpt.isEmpty()) {
            cart.removeAllItems();
            return List.of();
        }
        int cartId = cartIdOpt.get();
        cart.setId(cartId);

        List<CartItem> items = cartDAO.getCartItems(cartId);
        cart.setItems(items != null ? items : List.of());
        return items != null ? items : List.of();
    }

    public String addToCart(Cart cart, Integer userId, String sessionId,
                            int comicId, int quantity) {
        int cartId = resolveOrCreateCartId(userId, sessionId);
        cart.setId(cartId);
        System.out.println("[CartService] addToCart: cartId=" + cartId
                + " userId=" + userId + " sessionId=" + sessionId
                + " comicId=" + comicId + " qty=" + quantity);
        int affected = cartDAO.addItem(cartId, comicId, quantity);
        System.out.println("[CartService] addItem returned: " + affected);
        if (affected ==-1) return "not_found";
        if (affected == -2) return "out_of_stock";
        refreshCart(cart, cartId);
        return "success";
    }

    public String updateCart(Cart cart, Integer userId, String sessionId,
                             int comicId, int quantity) {
        Optional<Integer> cartIdOpt = resolveCartId(userId, sessionId, false);
        if (cartIdOpt.isEmpty()) return "not_found";

        int cartId = cartIdOpt.get();

        CartItem existing = cart.get(comicId);
        if (existing == null) return "not_found";

        int affected = cartDAO.updateQuantity(cartId, comicId, quantity);
        if (affected == 0) return "not_found";

        refreshCart(cart, cartId);

        CartItem updated = cart.get(comicId);
        if (updated != null && updated.getQuantity() < quantity) {
            return "out_of_stock";
        }
        return "success";
    }

    public boolean updateQuantity(Cart cart, Integer userId, String sessionId,
                                  int comicId, int newQuantity) {
        return "success".equals(updateCart(cart, userId, sessionId, comicId, newQuantity));
    }

    public boolean removeItem(Cart cart, Integer userId, String sessionId, int comicId) {
        Optional<Integer> cartIdOpt = resolveCartId(userId, sessionId, false);
        if (cartIdOpt.isEmpty()) return false;

        int cartId   = cartIdOpt.get();
        int affected = cartDAO.removeItem(cartId, comicId);
        if (affected > 0) {
            refreshCart(cart, cartId);
            return true;
        }
        return false;
    }

    public void clearCart(Cart cart, Integer userId, String sessionId) {
        Optional<Integer> cartIdOpt = resolveCartId(userId, sessionId, false);
        if (cartIdOpt.isEmpty()) return;
        CacheService.cartCache.remove(cartIdOpt.get());
        cartDAO.clearCartItems(cartIdOpt.get());
        cart.removeAllItems();
    }

    private Optional<Integer> resolveCartId(Integer userId, String sessionId,
                                            boolean createIfAbsent) {
            Optional<Integer> opt = cartDAO.findCartIdByUserId(userId);
            if (opt.isPresent() || !createIfAbsent) return opt;
            return Optional.of(cartDAO.getOrCreateCartByUserId(userId));
    }

    private int resolveOrCreateCartId(Integer userId, String sessionId) {
        return cartDAO.getOrCreateCartByUserId(userId);
    }

    private void refreshCart(Cart cart, int cartId) {
        cart.setId(cartId);
        List<CartItem> items = cartDAO.getCartItems(cartId);
        cart.setItems(items != null ? items : List.of());
    }

    public void mergeSessionCartToDb(Cart userCart, int userId, Cart guestCart) {
        for (CartItem guestItem  : guestCart.getItems()) {
            int comicId = guestItem.getComic().getId();
            int quantity = guestItem.getQuantity();
            addToCart(userCart,userId, null, comicId, quantity);
        }
        getCart(userCart, userId, null);
    }
}