package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CartDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.OrderDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "BuyAgain", value = "/buy-again")
public class BuyAgainServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập!\"}");
            return;
        }

        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));

            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
                session.setAttribute("cart", cart);
            }

            OrderDAO orderDAO = new OrderDAO();
            ComicDAO comicDAO = new ComicDAO();
            CartDAO cartDAO = new CartDAO();

            int userCartId = cartDAO.getOrCreateCartByUserId(currentUser.getId());

            List<OrderItem> orderItems = orderDAO.getOrderItems(orderId);

            int addedCount = 0;
            int outOfStockCount = 0;
            StringBuilder errorMsg = new StringBuilder();

            for (OrderItem item : orderItems) {
                Comic comic = comicDAO.getComicById(item.getComicId());

                if (comic == null) {
                    continue;
                }

                CartItem existingItem = cart.get(comic.getId());
                int currentQtyInCart = (existingItem != null) ? existingItem.getQuantity() : 0;
                int requestedQty = item.getQuantity();
                int totalQty = currentQtyInCart + requestedQty;

                int available = comic.getStockQuantity() - comic.getDamagedQuantity();
                if (available < totalQty) {
                    outOfStockCount++;
                    errorMsg.append(comic.getNameComics())
                            .append(" (chỉ còn ")
                            .append(comic.getStockQuantity())
                            .append("), ");
                    continue;
                }

                int result = cartDAO.addItem(userCartId, comic.getId(), requestedQty);
                if (result > 0) {
                    cart.addItem(comic, requestedQty);
                    addedCount++;
                } else {
                    outOfStockCount++;
                    errorMsg.append(comic.getNameComics()).append(", ");
                }
            }

            session.setAttribute("cart", cart);

            Map<String, Object> result = new HashMap<>();

            if (addedCount > 0) {
                result.put("success", true);
                result.put("addedCount", addedCount);
                result.put("totalItems", orderItems.size());

                if (outOfStockCount > 0) {
                    result.put("message", "Đã thêm " + addedCount + " sản phẩm vào giỏ hàng. " +
                            outOfStockCount + " sản phẩm không đủ hàng: " +
                            errorMsg.toString().replaceAll(", $", ""));
                } else {
                    result.put("message", "Đã thêm tất cả " + addedCount +
                            " sản phẩm vào giỏ hàng!");
                }
            } else {
                result.put("success", false);
                result.put("message", "Không thể thêm sản phẩm nào. Vui lòng kiểm tra lại!");
            }

            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(result));

        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu không hợp lệ!\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"Lỗi hệ thống: " +
                    e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/order-history");
    }
}