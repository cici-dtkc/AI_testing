package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

@WebServlet("/order-detail")
public class OrderDetailServlet extends HttpServlet {

    private OrderDAO orderDAO;
    private ComicDAO comicDAO;
    private PaymentDAO paymentDAO;
    private FlashSaleComicsDAO flashSaleComicsDAO;
    private ReviewDAO reviewDAO;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        comicDAO = new ComicDAO();
        paymentDAO = new PaymentDAO();
        flashSaleComicsDAO = new FlashSaleComicsDAO();
        reviewDAO = new ReviewDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("currentUser") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String orderIdParam = request.getParameter("orderId");
        if (orderIdParam == null || orderIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/order-history");
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(orderIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/order-history");
            return;
        }

        Order order = orderDAO.getOrderById(orderId).orElse(null);
        if (order == null || order.getUserId() != user.getId()) {
            response.sendRedirect(request.getContextPath() + "/order-history");
            return;
        }

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        List<OrderItem> rawItems = orderDAO.getOrderItems(orderId);
        List<Map<String, Object>> itemsWithComics = new ArrayList<>();

        for (OrderItem item : rawItems) {
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
                            ? ((Number) discountObj).doubleValue() : null;

                    if (discountPercent != null) {
                        double flashSalePrice = comic.getPrice() * (1 - discountPercent / 100.0);
                        itemData.put("hasActiveFlashSale", true);
                        itemData.put("flashSaleDiscount", discountPercent.intValue());
                        itemData.put("formattedDisplayPrice", currencyFormat.format(flashSalePrice));
                        itemData.put("priceChanged", Math.abs(priceAtPurchase - flashSalePrice) > 0.01);
                    } else {
                        itemData.put("hasActiveFlashSale", false);
                        itemData.put("formattedDisplayPrice", currencyFormat.format(currentPrice));
                        itemData.put("priceChanged", false);
                    }
                } else {
                    itemData.put("hasActiveFlashSale", false);
                    itemData.put("formattedDisplayPrice", currencyFormat.format(currentPrice));
                    itemData.put("priceChanged", Math.abs(priceAtPurchase - currentPrice) > 0.01);
                }
            }

            itemsWithComics.add(itemData);
        }

        String paymentMethod = paymentDAO.getPaymentByOrderId(orderId)
                .map(Payment::getPaymentMethod)
                .orElse("COD");

        request.setAttribute("order", order);
        request.setAttribute("items", itemsWithComics);
        request.setAttribute("paymentMethod", paymentMethod);

        boolean hasReviewed = reviewDAO.hasUserReviewedOrder(user.getId(), orderId);
        request.setAttribute("hasReviewed", hasReviewed);
        request.getRequestDispatcher("/frontend/nguoiB/orderDetail.jsp")
                .forward(request, response);
    }
}