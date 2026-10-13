package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.momo.MoMoUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet("/momo-return")
public class MoMoReturnServlet extends HttpServlet {
    private OrderDAO orderDAO;
    private UserDao userDAO;
    private UserShippingAddressDAO userShippingAddressDAO;
    private ComicDAO comicDAO;

    @Override
    public void init() throws ServletException {
        Jdbi jdbi = JdbiConnector.get();
        orderDAO = new OrderDAO(jdbi);
        userDAO  = new UserDao(jdbi);
        userShippingAddressDAO = new UserShippingAddressDAO(jdbi);
        comicDAO = new ComicDAO(jdbi);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String resultCode  = MoMoUtils.getParam(request.getParameterMap(), "resultCode");
        String transId     = MoMoUtils.getParam(request.getParameterMap(), "transId");
        String momoOrderId = MoMoUtils.getParam(request.getParameterMap(), "orderId");

        if (!"0".equals(resultCode)) {
            session.setAttribute("orderError", "Thanh toán MoMo không thành công! Mã lỗi: " + resultCode);
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }

        String recipientName  = (String) session.getAttribute("pending_receiverName");
        String shippingPhone  = (String) session.getAttribute("pending_receiverPhone");
        String province       = (String) session.getAttribute("pending_province");
        String district       = (String) session.getAttribute("pending_district");
        String ward           = (String) session.getAttribute("pending_ward");
        String streetAddress  = (String) session.getAttribute("pending_address");
        String shippingMethod = (String) session.getAttribute("pending_shipping");
        boolean usePoints     = Boolean.TRUE.equals(session.getAttribute("pending_usePoints"));



        @SuppressWarnings("unchecked")
        List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("selectedItems");
        if (selectedItems == null || selectedItems.isEmpty()) {
            session.setAttribute("orderError", "Phiên đặt hàng đã hết hạn.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        try {
            for (CartItem item : selectedItems) {
                if (!comicDAO.hasEnoughStock(item.getComic().getId(), item.getQuantity())) {
                    int stock = comicDAO.getStockQuantity(item.getComic().getId());
                    session.setAttribute("orderError",
                            "Sản phẩm '" + item.getComic().getNameComics() +
                                    "' không đủ hàng. Còn: " + stock);
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }

            double subtotal    = (Double) session.getAttribute("checkoutSubtotal");

            Object shippingFeeObj = session.getAttribute("pending_shippingFee");
            double shippingFee = (shippingFeeObj != null)
                    ? ((Number) shippingFeeObj).doubleValue()
                    : ("express".equals(shippingMethod) ? 50000 : 25000);

            int    pointsToUse    = 0;
            double pointsDiscount = 0;
            if (usePoints && user.getPoints() > 0) {
                pointsToUse    = user.getPoints();
                pointsDiscount = pointsToUse;
            }

            double totalAmount = Math.max(subtotal + shippingFee - pointsDiscount, 0);

            UserShippingAddress addr = new UserShippingAddress();
            addr.setUserId(user.getId());
            addr.setRecipientName(recipientName.trim());
            addr.setPhone(shippingPhone.trim());
            addr.setProvince(province.trim());
            addr.setDistrict(district != null ? district.trim() : "");
            addr.setWard(ward.trim());
            addr.setStreetAddress(streetAddress.trim());
            addr.setCreatedAt(LocalDate.now());
            addr.setUpdatedAt(LocalDate.now());
            addr.setDefault(userShippingAddressDAO.countAddressesByUserId(user.getId()) == 0);

            Optional<UserShippingAddress> existing = userShippingAddressDAO.findExistingAddress(addr);
            int addressId = existing.isPresent()
                    ? existing.get().getId()
                    : userShippingAddressDAO.createShippingAddress(addr);

            if (addressId <= 0) {
                session.setAttribute("orderError", "Không thể lưu địa chỉ giao hàng");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }

            String fullAddress = streetAddress.trim() + ", " + ward.trim()
                    + (district != null && !district.trim().isEmpty() ? ", " + district.trim() : "")
                    + ", " + province.trim();

            Order order = new Order();
            order.setUserId(user.getId());
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("Pending");
            order.setTotalAmount(totalAmount);
            order.setShippingAddressId(addressId);
            order.setRecipientName(recipientName.trim());
            order.setShippingPhone(shippingPhone.trim());
            order.setShippingAddress(fullAddress);
            order.setShippingProvider(shippingMethod);
            order.setShippingFee(shippingFee);
            order.setPointUsed(pointsToUse);
            order.setCreatedAt(LocalDateTime.now());

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem item : selectedItems) {
                OrderItem oi = new OrderItem();
                oi.setComicId(item.getComic().getId());
                oi.setQuantity(item.getQuantity());
                oi.setPriceAtPurchase(item.getFinalPrice());
                orderItems.add(oi);
            }

            int orderId = orderDAO.createOrderWithPayment(order, orderItems, "momo");
            if (orderId <= 0) {
                session.setAttribute("orderError", "Đặt hàng thất bại sau thanh toán MoMo!");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }

            PaymentDAO paymentDAO = new PaymentDAO(JdbiConnector.get());
            Optional<Payment> paymentOpt = paymentDAO.getPaymentByOrderId(orderId);
            if (paymentOpt.isPresent()) {
                paymentDAO.updatePaymentStatus(paymentOpt.get().getId(), "Completed", transId);
            }

            if (usePoints && pointsToUse > 0) {
                int newPoints = Math.max(user.getPoints() - pointsToUse, 0);
                userDAO.updateUserPoints(user.getId(), newPoints);
                user.setPoints(newPoints);
                session.setAttribute("currentUser", user);

                PointTransactionDAO ptDAO = new PointTransactionDAO(JdbiConnector.get());
                PointTransaction pt = new PointTransaction();
                pt.setUserId(user.getId());
                pt.setOrderId(orderId);
                pt.setPoints(pointsToUse);
                pt.setTransactionType("SPEND");
                pt.setDescription("Sử dụng " + pointsToUse + " xu cho đơn hàng #" + orderId);
                pt.setCreatedAt(LocalDateTime.now());
                ptDAO.createTransaction(pt);
            }

            Cart cart = (Cart) session.getAttribute("cart");
            @SuppressWarnings("unchecked")
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cart != null) {
                List<Integer> ids = new ArrayList<>();
                for (CartItem item : selectedItems) ids.add(item.getComic().getId());
                ids.forEach(cart::removeItem);
                if (cartItems != null) cartItems.removeIf(i -> ids.contains(i.getComic().getId()));
                session.setAttribute("cart", cart);
                session.setAttribute("cartItems", cartItems);
            }

            for (String key : new String[]{"pending_receiverName","pending_receiverPhone",
                    "pending_province","pending_district","pending_ward","pending_address",
                    "pending_shipping","pending_usePoints","selectedItems",
                    "checkoutSubtotal","shippingFee","checkoutTotal"}) {
                session.removeAttribute(key);
            }

            session.setAttribute("orderSuccess", "Đặt hàng thành công! Mã GD: " + transId);
            response.sendRedirect(request.getContextPath() + "/order-success");

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("orderError", "Có lỗi xảy ra: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/checkout");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}