package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Cart;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CartService;

import java.io.IOException;

/**
 * CartInitFilter — dam bao Cart object luon co trong session,
 * duoc nap tu DB dua tren user_id (da login) hoac session_id (guest).
 * Nho filter nay:
 * - Khi server restart, cart object duoc tai lai tu DB -> khong mat gio hang
 * - Guest cart ton tai theo session_id trong DB
 */
@WebFilter("/*")
public class CartInitFilter implements Filter {

    private CartService cartService;

    @Override
    public void init(FilterConfig filterConfig) {
        cartService = new CartService();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        String uri = httpRequest.getRequestURI();
//        if (isStaticResource(uri)) {
//            chain.doFilter(request, response);
//            return;
//        }
//        HttpSession session = httpRequest.getSession(false);
//        if (session != null) {
//            Cart cart = (Cart) session.getAttribute("cart");
//            if (cart == null) {
//                cart = new Cart();
//                User    user      = (User) session.getAttribute("currentUser");
//                Integer userId    = (user != null) ? user.getId() : null;
//                String  sessionId = session.getId();
//
//                cartService.getCart(cart, userId, sessionId);
//                session.setAttribute("cart", cart);
//            }
//        }
//
//        chain.doFilter(request, response);
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        if (isStaticResource(uri)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Cart cart = (Cart) session.getAttribute("cart");

            if (cart == null) {
                cart = new Cart();
                User    user   = (User) session.getAttribute("currentUser");
                Integer userId = (user != null) ? user.getId() : null;

                if (userId != null) {
                    cartService.getCart(cart, userId, null);
                }
                session.setAttribute("cart", cart);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isStaticResource(String uri) {
        return uri.endsWith(".css")  || uri.endsWith(".js")
                || uri.endsWith(".png")  || uri.endsWith(".jpg")
                || uri.endsWith(".jpeg") || uri.endsWith(".gif")
                || uri.endsWith(".svg")  || uri.endsWith(".ico")
                || uri.endsWith(".woff") || uri.endsWith(".woff2")
                || uri.endsWith(".ttf")  || uri.endsWith(".map")
                || uri.endsWith(".webp");
    }

    @Override
    public void destroy() {}
}