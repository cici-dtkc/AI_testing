package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Order;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.OrderItem;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.OrderViolationService;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

@WebServlet("/order-history")
public class OrderHistoryServlet extends HttpServlet {
    private OrderDAO orderDAO;
    private ComicDAO comicDAO;
    private ReviewDAO reviewDAO;
    private FlashSaleComicsDAO flashSaleComicsDAO;
    private UserDao userDAO;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        comicDAO = new ComicDAO();
        reviewDAO = new ReviewDAO();
        flashSaleComicsDAO = new FlashSaleComicsDAO();
        userDAO = UserDao.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Lấy filter từ parameter
        String filter = request.getParameter("filter");
        if (filter == null || filter.isEmpty()) {
            filter = "completed"; // Mặc định
        }
        System.out.println("Filter: " + filter);

        // Map filter sang status
        String dbStatus = mapFilterToStatus(filter);
        System.out.println("DB Status: " + dbStatus);

        // Lấy tất cả đơn hàng của user
        List<Order> allOrders = orderDAO.getOrdersByUserId(user.getId());
        System.out.println("Total orders: " + allOrders.size());

        // Filter theo status
        List<Order> filteredOrders;
        if ("all".equals(filter)) {
            filteredOrders = allOrders;
        } else {
            filteredOrders = allOrders.stream()
                    .filter(o -> o.getStatus().equals(dbStatus))
                    .collect(java.util.stream.Collectors.toList());
        }

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Lấy thông tin chi tiết
        List<Map<String, Object>> orderDetails = new ArrayList<>();

        for (Order order : filteredOrders) {

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("order", order);

            // Kiểm tra đã review chưa
            boolean hasReviewed = reviewDAO.hasUserReviewedOrder(user.getId(), order.getId());
            orderData.put("hasReviewed", hasReviewed);

            // Lấy order items
            List<OrderItem> items = orderDAO.getOrderItems(order.getId());

            List<Map<String, Object>> itemsWithComics = new ArrayList<>();

            for (OrderItem item : items) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("item", item);

                Comic comic = comicDAO.getComicById(item.getComicId());

                if (comic != null) {
                    itemData.put("comic", comic);

                    double priceAtPurchase = item.getPriceAtPurchase();
                    double currentPrice = comic.getDiscountPrice();

                    itemData.put("formattedPriceAtPurchase", currencyFormat.format(priceAtPurchase));

                    Map<String, Object> flashSaleInfo = flashSaleComicsDAO.getFlashSaleInfoByComicId(comic.getId());

                    if (flashSaleInfo != null) {
                        Object discountObj = flashSaleInfo.get("discount_percent");
                        Double discountPercent = (discountObj instanceof Number)
                                ? ((Number) discountObj).doubleValue()
                                : null;

                        if (discountPercent != null) {
                            double flashSalePrice = comic.getPrice() * (1 - discountPercent / 100.0);

                            itemData.put("hasActiveFlashSale", true);
                            itemData.put("flashSaleDiscount", discountPercent.intValue());
                            itemData.put("formattedDisplayPrice", currencyFormat.format(flashSalePrice));
                            itemData.put("priceChanged", Math.abs(priceAtPurchase - flashSalePrice) > 0.01);
                        }
                    } else {
                        itemData.put("hasActiveFlashSale", false);
                        itemData.put("formattedDisplayPrice", currencyFormat.format(currentPrice));
                        itemData.put("priceChanged", Math.abs(priceAtPurchase - currentPrice) > 0.01);
                    }
                }

                itemsWithComics.add(itemData);
            }

            orderData.put("items", itemsWithComics);
            orderDetails.add(orderData);
        }

        request.setAttribute("orderDetails", orderDetails);
        request.setAttribute("currentFilter", filter);
        request.getRequestDispatcher("/frontend/nguoiB/orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        boolean success = false;
        String message = "";
        try {
            String action = request.getParameter("action");
            int orderId = Integer.parseInt(request.getParameter("orderId"));

            switch (action) {
                case "cancel":
                    String cancelReason = request.getParameter("reason");
                    success = orderDAO.cancelOrderWithHistory(orderId, user.getId(), cancelReason);

                    if (success) {
                        OrderViolationService.getInstance().checkCancelViolation(user.getId());
                        User updatedUser = userDAO.getUserById(user.getId());
                        if (updatedUser != null && updatedUser.getLockedUntil() != null
                                && updatedUser.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                            session.invalidate();
                            message = "Hủy đơn thành công. Tài khoản của bạn đã bị khóa 24 giờ do hủy quá nhiều đơn hàng!";
                        } else {
                            message = "Không thể hủy đơn hàng";
                        }
                    } else {
                        message = "Không thể hủy đơn hàng";
                    }
                    break;

                case "receive":
                    success = orderDAO.updateOrderStatusWithPoints(orderId, "Completed");
                    if (success) {
                        User updatedUser = userDAO.getUserById(user.getId());
                        if (updatedUser != null) {
                            session.setAttribute("currentUser", updatedUser);
                            int pointsEarned = Math.max(updatedUser.getPoints() - user.getPoints(), 0);
                            response.getWriter().write(String.format(
                                    "{\"success\":true,\"message\":\"Đã xác nhận nhận hàng\"," +
                                            "\"newPoints\":%d,\"pointsEarned\":%d}",
                                    updatedUser.getPoints(), pointsEarned
                            ));
                            return;
                        }
                        message = "Đã xác nhận nhận hàng";
                    } else {
                        message = "Không thể cập nhật";
                    }
                    break;

                case "return":
                    success = orderDAO.updateOrderStatus(orderId, "Returned");
                    message = success ? "Đã yêu cầu trả hàng" : "Không thể trả hàng";
                    break;

                default:
                    message = "Hành động không hợp lệ";
                    System.out.println("Unknown action: " + action);
                    break;
            }
        } catch (NumberFormatException e) {
            success = false;
            message = "Mã đơn hàng không hợp lệ";
            e.printStackTrace();
        } catch (Exception e) {
            success = false;
            message = "Lỗi: " + e.getMessage();
            e.printStackTrace();
        }

        String jsonResponse = String.format(
                "{\"success\":%b,\"message\":\"%s\"}",
                success,
                message.replace("\"", "\\\"").replace("\n", " ")
        );

        response.getWriter().write(jsonResponse);
    }

    private String mapFilterToStatus(String filter) {
        switch (filter) {
            case "pending":
                return "Pending";
            case "shipping":
                return "AwaitingPickup";
            case "delivery":
                return "Shipping";
            case "completed":
                return "Completed";
            case "canceled":
                return "Cancelled";
            case "refund":
                return "Returned";
            default:
                return "Pending";
        }
    }
}