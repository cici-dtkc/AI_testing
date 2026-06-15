package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

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
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.momo.MoMoUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.vnpay.VNPayUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet("/order")
public class OrderServlet extends HttpServlet {
    private OrderDAO orderDAO;
    private UserDao userDAO;
    private UserShippingAddressDAO userShippingAddressDAO;
    private ComicDAO comicDAO;
    private VoucherDao voucherDao;

    @Override
    public void init() throws ServletException {
        Jdbi jdbi = JdbiConnector.get();
        orderDAO = new OrderDAO(jdbi);
        userDAO = new UserDao(jdbi);
        userShippingAddressDAO = new UserShippingAddressDAO(jdbi);
        comicDAO = new ComicDAO(jdbi);
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        // Kiểm tra đăng nhập
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String recipientName = request.getParameter("receiverName");
            String shippingPhone = request.getParameter("receiverPhone");
            String province = request.getParameter("province");
            String district = request.getParameter("district");
            String ward = request.getParameter("ward");
            String streetAddress = request.getParameter("address");
            String shippingMethod = request.getParameter("shipping"); // standard hoặc express
            String paymentMethod = request.getParameter("payment"); // COD hoặc ewallet

            session.setAttribute("pending_receiverName", request.getParameter("receiverName"));
            session.setAttribute("pending_receiverPhone", request.getParameter("receiverPhone"));
            session.setAttribute("pending_province", request.getParameter("province"));
            session.setAttribute("pending_district", request.getParameter("district"));
            session.setAttribute("pending_ward", request.getParameter("ward"));
            session.setAttribute("pending_address", request.getParameter("address"));
            session.setAttribute("pending_shipping", request.getParameter("shipping"));
            String upParam = request.getParameter("usePoints");
            session.setAttribute("pending_usePoints", "on".equals(upParam) || "true".equals(upParam));

            try {
                String feeParam = request.getParameter("shippingFee");
                double fee = (feeParam != null && !feeParam.isEmpty())
                        ? Double.parseDouble(feeParam)
                        : ("express".equals(request.getParameter("shipping")) ? 50000 : 25000);
                session.setAttribute("pending_shippingFee", fee);
            } catch (NumberFormatException e) {
                session.setAttribute("pending_shippingFee",
                        "express".equals(request.getParameter("shipping")) ? 50000.0 : 25000.0);
            }

            String usePointsStr = request.getParameter("usePoints");

            if ("vnpay".equals(paymentMethod)) {
                double subtotal = (Double) session.getAttribute("checkoutSubtotal");

                double shippingFee;
                try {
                    String shippingFeeParam = request.getParameter("shippingFee");
                    shippingFee = (shippingFeeParam != null && !shippingFeeParam.isEmpty())
                            ? Double.parseDouble(shippingFeeParam)
                            : ("express".equals(shippingMethod) ? 50000 : 25000);
                } catch (NumberFormatException e) {
                    shippingFee = "express".equals(shippingMethod) ? 50000 : 25000;
                }

                String upStr = request.getParameter("usePoints");
                boolean usePoints = "on".equals(upStr) || "true".equals(upStr);
                double discount = (usePoints && user.getPoints() > 0) ? user.getPoints() : 0;
                long totalAmount = (long) Math.max(subtotal + shippingFee - discount, 0);

//                String returnUrl = request.getScheme() + "://" + request.getServerName()
//                        + ":" + request.getServerPort()
//                        + request.getContextPath() + "/vnpay-return";
//                String returnUrl = "https://nonpunctual-lissa-tychistic.ngrok-free.dev"
//                        + request.getContextPath() + "/vnpay-return";
                String returnUrl = "http://localhost:8080/TTLTW_Nhom6/vnpay-return";


                String payUrl = VNPayUtils.createPaymentUrl(
                        0, totalAmount,
                        "Thanh toan don hang Comic Store",
                        returnUrl,
                        request.getRemoteAddr()
                );
                response.sendRedirect(payUrl);
                return;
            }

            if ("momo".equals(paymentMethod)) {
                double subtotal = (Double) session.getAttribute("checkoutSubtotal");
                double shippingFee;
                try {
                    String fp = request.getParameter("shippingFee");
                    shippingFee = (fp != null && !fp.isEmpty())
                            ? Double.parseDouble(fp)
                            : ("express".equals(shippingMethod) ? 50000 : 25000);
                } catch (NumberFormatException e) {
                    shippingFee = "express".equals(shippingMethod) ? 50000 : 25000;
                }

                String usePointsParam = request.getParameter("usePoints");
                boolean usePoints = "on".equals(usePointsParam) || "true".equals(usePointsParam);
                session.setAttribute("pending_usePoints", usePoints);

                double discount = (usePoints && user.getPoints() > 0) ? user.getPoints() : 0;
                long totalAmount = (long) Math.max(subtotal + shippingFee - discount, 0);

                try {
                    String payUrl = MoMoUtils.createPaymentUrl(0, totalAmount,
                            "Thanh toan don hang Comic Store");
                    response.sendRedirect(payUrl);
                } catch (Exception e) {
                    session.setAttribute("orderError", "Không thể kết nối MoMo: " + e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/checkout");
                }
                return;
            }

            // Validate thông tin bắt buộc
            if (recipientName == null || recipientName.trim().isEmpty() ||
                    shippingPhone == null || shippingPhone.trim().isEmpty() ||
                    province == null || province.trim().isEmpty() ||
                    ward == null || ward.trim().isEmpty() ||
                    streetAddress == null || streetAddress.trim().isEmpty() ||
                    shippingMethod == null || shippingMethod.trim().isEmpty() ||
                    paymentMethod == null || paymentMethod.trim().isEmpty()) {

                session.setAttribute("orderError", "Vui lòng điền đầy đủ thông tin bắt buộc");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }

            boolean usePoints = "on".equals(usePointsStr) || "true".equals(usePointsStr);
            if ("vnpay".equals(paymentMethod)) {
                session.setAttribute("pending_receiverName", recipientName);
                session.setAttribute("pending_receiverPhone", shippingPhone);
                session.setAttribute("pending_province", province);
                session.setAttribute("pending_district", district);
                session.setAttribute("pending_ward", ward);
                session.setAttribute("pending_address", streetAddress);
                session.setAttribute("pending_shipping", shippingMethod);
                session.setAttribute("pending_usePoints", usePoints);
//                Tinh tong tien de gui sang vnpay
                double subtotalVnpay = (Double) session.getAttribute("checkoutSubtotal");
                double shippingFeeVnpay;
                try {
                    String shippingFeeParam = request.getParameter("shippingFee");
                    shippingFeeVnpay = (shippingFeeParam != null && !shippingFeeParam.isEmpty())
                            ? Double.parseDouble(shippingFeeParam)
                            : ("express".equals(shippingMethod) ? 50000 : 25000);
                } catch (NumberFormatException e) {
                    shippingFeeVnpay = "express".equals(shippingMethod) ? 50000 : 25000;
                }

                double pointsDiscountVnpay = 0;
                if (usePoints && user.getPoints() > 0) {
                    pointsDiscountVnpay = user.getPoints();
                }
                long totalVnpay = (long) Math.max(subtotalVnpay + shippingFeeVnpay - pointsDiscountVnpay, 0);
                String ipAddress = request.getHeader("X-FORWARDED-FOR");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
//                String returnURL = request.getScheme() + "://"
//                        + request.getServerName() + ":"
//                        + request.getServerPort()
//                        + request.getContextPath()
//                        + "/vnpay-return";

                String returnURL = "https://nonpunctual-lissa-tychistic.ngrok-free.dev"
                        + request.getContextPath() + "/vnpay-return";
                String orderInfo = "Thanh toan don hang " + user.getId();
                String paymentUrl = VNPayUtils.createPaymentUrl(
                        user.getId(), totalVnpay, orderInfo, returnURL, ipAddress
                );

                response.sendRedirect(paymentUrl);
                return;
            }

            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("selectedItems");

            if (selectedItems == null || selectedItems.isEmpty()) {
                session.setAttribute("orderError", "Không có sản phẩm nào được chọn");
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }

            for (CartItem item : selectedItems) {
                int comicId = item.getComic().getId();
                int requestedQty = item.getQuantity();

                if (!comicDAO.hasEnoughStock(comicId, requestedQty)) {
                    int availableStock = comicDAO.getStockQuantity(comicId);
                    session.setAttribute("orderError",
                            "Sản phẩm '" + item.getComic().getNameComics() +
                                    "' không đủ hàng. Còn lại: " + availableStock + ", bạn yêu cầu: " + requestedQty);
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }
            for (CartItem item : selectedItems) {
                int comicId = item.getComic().getId();
                System.out.println("available " + comicDAO.available(comicId));
                int available = comicDAO.available(comicId);

                if (available < item.getQuantity()) {
                    session.setAttribute("orderError", item.getComic().getNameComics() + " hiện không có sẳn");

                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }

            Object subtotalObj = session.getAttribute("checkoutSubtotal");
            double subtotal = subtotalObj != null ? ((Double) subtotalObj) : 0.0;
            double shippingFee;
            try {
                String shippingFeeParam = request.getParameter("shippingFee");
                shippingFee = (shippingFeeParam != null && !shippingFeeParam.isEmpty())
                        ? Double.parseDouble(shippingFeeParam)
                        : ("express".equals(shippingMethod) ? 50000 : 25000);
            } catch (NumberFormatException e) {
                shippingFee = "express".equals(shippingMethod) ? 50000 : 25000;
            }

            int pointsToUse = 0;
            double pointsDiscount = 0;
            if (usePoints && user.getPoints() > 0) {
                pointsToUse = user.getPoints();
                pointsDiscount = pointsToUse;
            }

            // MMMMMMMMM
            Voucher appliedProductVoucher = null;
            String voucherCode = request.getParameter("voucherCode");
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                appliedProductVoucher = validateAndLoadVoucher(
                        voucherCode.trim(), user, selectedItems, shippingFee,
                        subtotal, request, session, voucherDao, comicDAO);
                if (appliedProductVoucher == null) {
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }

            Voucher appliedShippingVoucher = null;
            String voucherShippingCode = request.getParameter("voucherShippingCode");
            if (voucherShippingCode != null && !voucherShippingCode.trim().isEmpty()) {
                appliedShippingVoucher = validateAndLoadVoucher(
                        voucherShippingCode.trim(), user, selectedItems, shippingFee,
                        subtotal, request, session, voucherDao, comicDAO);
                if (appliedShippingVoucher == null) {
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }

            double voucherProductDiscount = 0;
            if (appliedProductVoucher != null) {
                voucherProductDiscount = calcVoucherDiscount(
                        appliedProductVoucher, selectedItems, subtotal, shippingFee, comicDAO);
            }

            double voucherShippingDiscount = 0;
            if (appliedShippingVoucher != null) {
                voucherShippingDiscount = calcVoucherDiscount(
                        appliedShippingVoucher, selectedItems, subtotal, shippingFee, comicDAO);
                voucherShippingDiscount = Math.min(voucherShippingDiscount, shippingFee);
            }

            double totalAmount = subtotal + shippingFee - pointsDiscount - voucherProductDiscount - voucherShippingDiscount;
/// EEEEEEE
            if (totalAmount < 0) {
                totalAmount = 0;
            }

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

            boolean setAsDefault = "true".equals(request.getParameter("setDefaultAddress"));

            int addressCount = userShippingAddressDAO.countAddressesByUserId(user.getId());
            boolean shouldSetDefault = setAsDefault || addressCount == 0;
            shippingAddress.setDefault(shouldSetDefault);

            Optional<UserShippingAddress> existingAddress =
                    userShippingAddressDAO.findExistingAddress(shippingAddress);

            int shippingAddressId;
            if (existingAddress.isPresent()) {
                shippingAddressId = existingAddress.get().getId();
                if (shouldSetDefault) {
                    userShippingAddressDAO.setDefaultAddress(shippingAddressId, user.getId());
                }
            } else {
                if (shouldSetDefault) {
                    userShippingAddressDAO.unsetAllDefaultAddresses(user.getId());
                }
                shippingAddressId = userShippingAddressDAO.createShippingAddress(shippingAddress);

                if (shippingAddressId <= 0) {
                    session.setAttribute("orderError", "Không thể lưu địa chỉ giao hàng");
                    response.sendRedirect(request.getContextPath() + "/checkout");
                    return;
                }
            }


            Order order = new Order();
            order.setUserId(user.getId());
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("Pending");
            order.setTotalAmount(totalAmount);
            order.setShippingAddressId(shippingAddressId);
            order.setRecipientName(recipientName.trim());
            order.setShippingPhone(shippingPhone.trim());
            String trackingCode = generateTrackingCode(shippingMethod);
            order.setTrackingCode(trackingCode);

            String fullAddress = streetAddress.trim() + ", " + ward.trim();
            if (district != null && !district.trim().isEmpty()) {
                fullAddress += ", " + district.trim();
            }
            fullAddress += ", " + province.trim();
            order.setShippingAddress(fullAddress);

            order.setShippingProvider(shippingMethod);
            order.setShippingFee(shippingFee);
            order.setPointUsed(pointsToUse);
            order.setProductDiscount(voucherProductDiscount);
            order.setShippingDiscount(voucherShippingDiscount);
            order.setCreatedAt(LocalDateTime.now());


            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem item : selectedItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setComicId(item.getComic().getId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPriceAtPurchase(item.getFinalPrice());
                orderItem.setFlashSaleId(item.getFlashSaleId());
                orderItems.add(orderItem);
            }


            int orderId = orderDAO.createOrderWithPayment(order, orderItems, paymentMethod);

            if (orderId > 0) {
                CartDAO cartDAOClean = new CartDAO();
                int userCartId = cartDAOClean.getOrCreateCartByUserId(user.getId());
                for (CartItem item : selectedItems) {
                    cartDAOClean.removeItem(userCartId, item.getComic().getId());
                }

                if (appliedProductVoucher != null) {
                    voucherDao.markVoucherAsUsed(user.getId(), appliedProductVoucher.getId(), orderId);
                }
                if (appliedShippingVoucher != null) {
                    voucherDao.markVoucherAsUsed(user.getId(), appliedShippingVoucher.getId(), orderId);
                }

                if ("ewallet".equals(paymentMethod)) {
                    PaymentDAO paymentDAO = new PaymentDAO(JdbiConnector.get());

                    String transactionId = generateTransactionId();

                    Optional<Payment> paymentOpt = paymentDAO.getPaymentByOrderId(orderId);
                    if (paymentOpt.isPresent()) {
                        Payment payment = paymentOpt.get();
                        paymentDAO.updatePaymentStatus(
                                payment.getId(),
                                "Completed",
                                transactionId
                        );
                    }
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

                session.removeAttribute("selectedItems");
                session.removeAttribute("checkoutSubtotal");
                session.removeAttribute("shippingFee");
                session.removeAttribute("checkoutTotal");

                session.setAttribute("orderId", orderId);
                session.setAttribute("orderTotal", totalAmount);
                session.setAttribute("orderPaymentMethod", paymentMethod);
                session.setAttribute("orderSuccess", "Đặt hàng thành công!");

                response.sendRedirect(request.getContextPath() + "/order-success");
            } else {
                session.setAttribute("orderError", "Đặt hàng thất bại. Vui lòng thử lại!");
                response.sendRedirect(request.getContextPath() + "/checkout");
            }

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


    private String generateTransactionId() {
        String prefix = "TXN";
        long timestamp = System.currentTimeMillis() / 1000;

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 6; i++) {
            randomPart.append(characters.charAt(random.nextInt(characters.length())));
        }
        return prefix + timestamp + randomPart.toString();
    }

    private String generateTrackingCode(String shippingProvider) {
        String prefix = "express".equalsIgnoreCase(shippingProvider) ? "EXP" : "STD";
        String datePart = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = java.util.UUID.randomUUID()
                .toString().substring(0, 6).toUpperCase();
        return prefix + "-" + datePart + "-" + randomPart;
    }

    private double calcScopeSubtotal(Voucher voucher, List<CartItem> selectedItems, ComicDAO comicDAO) {
        String scope = voucher.getApplyScope();
        if (scope == null || scope.trim().isEmpty() || "Tất cả".equals(scope.trim())) {
            return selectedItems.stream()
                    .mapToDouble(i -> i.getFinalPrice() * i.getQuantity())
                    .sum();
        }
        return selectedItems.stream()
                .filter(item -> {
                    String cat = comicDAO.getCategoryName(item.getComic().getId());
                    return cat != null && cat.trim().equalsIgnoreCase(scope.trim());
                })
                .mapToDouble(i -> i.getFinalPrice() * i.getQuantity())
                .sum();
    }

    private Voucher validateAndLoadVoucher(
            String code, User user, List<CartItem> selectedItems,
            double shippingFee, double subtotal,
            HttpServletRequest request, HttpSession session,
            VoucherDao voucherDao, ComicDAO comicDAO) {

        Voucher voucher = voucherDao.getVoucherByCodeAndUser(code, user.getId());
        if (voucher == null || voucher.isExpired()) {
            session.setAttribute("orderError", "Voucher \"" + code + "\" không hợp lệ hoặc đã hết hạn.");
            return null;
        }

        if (voucher.isSingleUse() && voucherDao.hasUserUsedVoucher(user.getId(), voucher.getId())) {
            session.setAttribute("orderError", "Voucher \"" + code + "\" chỉ được sử dụng 1 lần và bạn đã dùng rồi.");
            return null;
        }

        if (voucher.getMinOrderAmount() != null && voucher.getMinOrderAmount().doubleValue() > 0) {
            boolean isShippingVoucher = "Vận chuyển".equals(voucher.getDiscountTarget());
            if (isShippingVoucher) {
                double baseForShip = calcScopeSubtotal(voucher, selectedItems, comicDAO);
                if (baseForShip < voucher.getMinOrderAmount().doubleValue()) {
                    String scopeLabel = (voucher.getApplyScope() != null && !"Tất cả".equals(voucher.getApplyScope()))
                            ? "sản phẩm thể loại \"" + voucher.getApplyScope() + "\""
                            : "giá trị đơn hàng";
                    session.setAttribute("orderError", "Voucher \"" + code + "\" yêu cầu " + scopeLabel
                            + " tối thiểu " + voucher.getMinOrderAmount().toPlainString() + "đ.");
                    return null;
                }
            } else {
                double baseAmount = calcScopeSubtotal(voucher, selectedItems, comicDAO);
                if (baseAmount < voucher.getMinOrderAmount().doubleValue()) {
                    String scopeLabel = (voucher.getApplyScope() != null && !"Tất cả".equals(voucher.getApplyScope()))
                            ? "sản phẩm thể loại \"" + voucher.getApplyScope() + "\""
                            : "giá trị đơn hàng";
                    session.setAttribute("orderError", "Voucher \"" + code + "\" yêu cầu " + scopeLabel
                            + " tối thiểu " + voucher.getMinOrderAmount().toPlainString() + "đ.");
                    return null;
                }
            }
        }

        List<Integer> comicIds = selectedItems.stream()
                .map(item -> item.getComic().getId())
                .collect(java.util.stream.Collectors.toList());
        if (!voucherDao.isVoucherApplicableForItems(code, comicIds)) {
            session.setAttribute("orderError", "Voucher \"" + code + "\" không áp dụng được cho các sản phẩm trong đơn hàng này.");
            return null;
        }

        return voucher;
    }

    private double calcVoucherDiscount(Voucher voucher, List<CartItem> selectedItems,
                                       double subtotal, double shippingFee, ComicDAO comicDAO) {
        boolean isShipping = "Vận chuyển".equals(voucher.getDiscountTarget());
        double base;
        if (isShipping) {
            base = shippingFee;
        } else {
            base = calcScopeSubtotal(voucher, selectedItems, comicDAO);
        }

        double discount;
        if ("percent".equals(voucher.getDiscountType())) {
            discount = Math.floor(base * voucher.getDiscountValue().doubleValue() / 100.0);
        } else {
            discount = voucher.getDiscountValue() != null ? voucher.getDiscountValue().doubleValue() : 0;
        }

        if (isShipping) {
            discount = Math.min(discount, shippingFee);
        } else {
            discount = Math.min(discount, base);
        }
        return discount;
    }
}
