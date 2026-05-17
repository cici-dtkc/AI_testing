package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Cart;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.CartItem;

import java.io.IOException;
import java.util.Map;

@WebFilter("/*")
public class CartFlashSaleUpdateFilter implements Filter {

    private FlashSaleComicsDAO flashSaleComicsDAO;
    private FlashSaleDAO       flashSaleDAO;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        flashSaleComicsDAO = new FlashSaleComicsDAO();
        flashSaleDAO       = new FlashSaleDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        if (uri.contains("/login") || uri.contains("/logout")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart != null && !cart.getItems().isEmpty()) {
                updateCartFlashSalePrices(cart);
                session.setAttribute("cart", cart);
            }
        }

        chain.doFilter(request, response);
    }

    private void updateCartFlashSalePrices(Cart cart) {
        flashSaleDAO.updateStatuses();

        for (CartItem item : cart.getItems()) {
            int comicId = item.getComic().getId();

            Map<String, Object> activeFlashSale =
                    flashSaleComicsDAO.getFlashSaleInfoByComicId(comicId);

            if (activeFlashSale != null) {
                Integer newFlashSaleId = (Integer) activeFlashSale.get("flashsale_id");
                Object  discountObj    = activeFlashSale.get("discount_percent");
                Double  discountPercent = (discountObj instanceof Number)
                        ? ((Number) discountObj).doubleValue() : null;

                if (discountPercent != null) {
                    double newFlashSalePrice =
                            item.getComic().getPrice() * (1 - discountPercent / 100.0);

                    boolean flashSaleChanged = !newFlashSaleId.equals(item.getFlashSaleId());
                    boolean priceChanged = item.getFlashSalePrice() == null
                            || Math.abs(item.getFlashSalePrice() - newFlashSalePrice) > 0.01;

                    if (flashSaleChanged || priceChanged) {
                        // SỬA: dùng đúng field flashSalePrice thay vì priceAtPurchase
                        item.setFlashSaleId(newFlashSaleId);
                        item.setFlashSalePrice(newFlashSalePrice);
                    }
                }
            } else {
                if (item.isInFlashSale()) {
                    item.removeFlashSale();
                }
            }
        }
    }

    @Override
    public void destroy() {
        System.out.println("CartFlashSaleUpdateFilter destroyed");
    }
}