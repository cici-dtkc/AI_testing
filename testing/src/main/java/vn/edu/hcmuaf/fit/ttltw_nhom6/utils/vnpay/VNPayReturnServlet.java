package vn.edu.hcmuaf.fit.ttltw_nhom6.utils.vnpay;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet("/vnpay-return")
public class VNPayReturnServlet extends HttpServlet {
    private OrderDAO orderDAO;
    private UserDao userDAO;
    private UserShippingAddressDAO userShippingAddressDAO;
    private ComicDAO comicDAO;

    @Override
    public void init() throws ServletException {
        Jdbi jdbi = JdbiConnector.get();
        orderDAO = new OrderDAO(jdbi);
        userDAO = new UserDao(jdbi);
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
        if (!VNPayUtils.verifySignature(request.getParameterMap())) {
            session.setAttribute("orderError", "Chữ ký VNPay không hợp lệ!");
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }
        String responseCode = VNPayUtils.getParam(request.getParameterMap(), "vnp_ResponseCode");
        String transactionNo = VNPayUtils.getParam(request.getParameterMap(), "vnp_TransactionNo");
        if (!"00".equals(responseCode)) {
            session.setAttribute("orderError",
                    "Thanh toán VNPay không thành công! Mã lỗi: " + responseCode);
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }
        String recipientName = (String) session.getAttribute("pending_receiverName");
        String shippingPhone = (String) session.getAttribute("pending_receiverPhone");
        String province = (String) session.getAttribute("pending_province");
        String district = (String) session.getAttribute("pending_district");
        String ward = (String) session.getAttribute("pending_ward");
        String streetAddress = (String) session.getAttribute("pending_address");
        String shippingMethod = (String) session.getAttribute("pending_shipping");
        boolean usePoints = Boolean.TRUE.equals(session.getAttribute("pending_usePoints"));

        @SuppressWarnings("unchecked")
        List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("selectedItems");
        if (selectedItems == null || selectedItems.isEmpty()) {
            session.setAttribute("orderError", "Phiên đặt hàng đã hết hạn. Vui lòng thử lại.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }
        try {
            for (CartItem item : selectedItems) {
                int comicId = item.getComic().getId();
                int requestedQty = item.getQuantity();

                if (!comicDAO.hasEnoughStock(comicId, requestedQty)) {
                    int availableStock = comicDAO.getStockQuantity(comicId);
                    session.setAttribute("orderError",
                            "Sản phẩm '" + item.getComic().getNameComics() +
                                    "' không đủ hàng. Còn lại: " + availableStock +
                                    ", bạn yêu cầu: " + requestedQty);
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }
            double subtotal = (Double) session.getAttribute("checkoutSubtotal");
            Object shippingFeeObj = session.getAttribute("pending_shippingFee");
            double shippingFee = (shippingFeeObj != null)
                    ? ((Number) shippingFeeObj).doubleValue()
                    : ("express".equals(shippingMethod) ? 50000 : 25000);

            int pointsToUse = 0;
            double pointsDiscount = 0;
            if (usePoints && user.getPoints() > 0) {
                pointsToUse = user.getPoints();
                pointsDiscount = pointsToUse;
            }
            double totalAmount = subtotal + shippingFee - pointsDiscount;
            if (totalAmount < 0) totalAmount = 0;
            UserShippingAddress shippingAddress = new UserShippingAddress();
            shippingAddress.setUserId(user.getId());
            shippingAddress.setRecipientName(recipientName.trim());
            shippingAddress.setPhone(shippingPhone.trim());
            shippingAddress.setProvince(province.trim());
            shippingAddress.setDistrict(district != null ? district.trim() : "");
            shippingAddress.setWard(ward.trim());
            shippingAddress.setStreetAddress(streetAddress.trim());
            shippingAddress.setCreatedAt(LocalDate.now());
            shippingAddress.setUpdatedAt(LocalDate.now());

            int addressCount = userShippingAddressDAO.countAddressesByUserId(user.getId());
            shippingAddress.setDefault(addressCount == 0);
            Optional<UserShippingAddress> existingAddress = userShippingAddressDAO.findExistingAddress(shippingAddress);
            int shippingAddressId;
            if (existingAddress.isPresent()) {
                shippingAddressId = existingAddress.get().getId();
            } else {
                shippingAddressId = userShippingAddressDAO.createShippingAddress(shippingAddress);
                if (shippingAddressId <= 0) {
                    session.setAttribute("orderError", "Không thể lưu địa chỉ giao hàng");
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }
            String fullAddress = streetAddress.trim() + ", " + ward.trim();
            if (district != null && !district.trim().isEmpty()) {
                fullAddress += ", " + district.trim();
            }
            fullAddress += ", " + province.trim();
            Order order = new Order();
            order.setUserId(user.getId());
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("Pending");
            order.setTotalAmount(totalAmount);
            order.setShippingAddressId(shippingAddressId);
            order.setRecipientName(recipientName.trim());
            order.setShippingPhone(shippingPhone.trim());
            order.setShippingAddress(fullAddress);
            order.setShippingProvider(shippingMethod);
            order.setShippingFee(shippingFee);
            order.setPointUsed(pointsToUse);
            order.setCreatedAt(LocalDateTime.now());

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem item : selectedItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setComicId(item.getComic().getId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPriceAtPurchase(item.getFinalPrice());
                orderItems.add(orderItem);
            }
            int orderId = orderDAO.createOrderWithPayment(order, orderItems, "vnpay");

            if (orderId <= 0) {
                session.setAttribute("orderError", "Đặt hàng thất bại sau thanh toán VNPay!");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }
            PaymentDAO paymentDAO = new PaymentDAO(JdbiConnector.get());
            Optional<Payment> paymentOpt = paymentDAO.getPaymentByOrderId(orderId);
            if (paymentOpt.isPresent()) {
                paymentDAO.updatePaymentStatus(
                        paymentOpt.get().getId(),
                        "Completed",
                        transactionNo   //  Mã giao dịch thật từ VNPay
                );
                System.out.println(" VNPay payment completed. Transaction No: " + transactionNo);
            }
            if (usePoints && pointsToUse > 0) {
                int newPoints = user.getPoints() - pointsToUse;
                if (newPoints < 0) newPoints = 0;

                userDAO.updateUserPoints(user.getId(), newPoints);
                user.setPoints(newPoints);
                session.setAttribute("currentUser", user);

                PointTransactionDAO pointTransactionDAO = new PointTransactionDAO(JdbiConnector.get());
                PointTransaction transaction = new PointTransaction();
                transaction.setUserId(user.getId());
                transaction.setOrderId(orderId);
                transaction.setPoints(pointsToUse);
                transaction.setTransactionType("SPEND");
                transaction.setDescription("Sử dụng " + pointsToUse + " xu cho đơn hàng #" + orderId);
                transaction.setCreatedAt(LocalDateTime.now());

                pointTransactionDAO.createTransaction(transaction);
            }
            Cart cart = (Cart) session.getAttribute("cart");
            @SuppressWarnings("unchecked")
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");

            if (cart != null && cartItems != null) {
                List<Integer> purchasedComicIds = new ArrayList<>();
                for (CartItem item : selectedItems) {
                    purchasedComicIds.add(item.getComic().getId());
                }

                for (Integer comicId : purchasedComicIds) {
                    cart.removeItem(comicId);
                }
                cartItems.removeIf(item -> purchasedComicIds.contains(item.getComic().getId()));

                session.setAttribute("cart", cart);
                session.setAttribute("cartItems", cartItems);
                session.setAttribute("clearCartLocalStorage", Boolean.TRUE);
            }
            session.removeAttribute("pending_receiverName");
            session.removeAttribute("pending_receiverPhone");
            session.removeAttribute("pending_province");
            session.removeAttribute("pending_district");
            session.removeAttribute("pending_ward");
            session.removeAttribute("pending_address");
            session.removeAttribute("pending_shipping");
            session.removeAttribute("pending_usePoints");
            session.removeAttribute("selectedItems");
            session.removeAttribute("checkoutSubtotal");
            session.removeAttribute("shippingFee");
            session.removeAttribute("checkoutTotal");

            session.setAttribute("orderId", orderId);
            session.setAttribute("orderTotal", totalAmount);
            session.setAttribute("orderPaymentMethod", "vnpay");
            session.setAttribute("orderSuccess",
                    "Đặt hàng thành công! Mã giao dịch VNPay: " + transactionNo);

            response.sendRedirect(request.getContextPath() + "/order-success");
        } catch (RuntimeException e) {
            e.printStackTrace();
            session.setAttribute("orderError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/checkout");
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
