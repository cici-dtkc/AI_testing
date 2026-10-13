package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.CurrencyFormatter;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ghn.GHNConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OrderService {
    private final Jdbi jdbi;
    private OrderDAO orderDAO;
    private PaymentDAO paymentDAO;
    private ComicDAO comicDAO;
    private UserDao userDAO;
    private OrderHistoryDAO orderHistoryDAO;
    private OrderReturnDAO orderReturnDAO;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public OrderService() {
        this.jdbi = JdbiConnector.get();
        this.orderDAO = new OrderDAO(jdbi);
        this.paymentDAO = new PaymentDAO();
        this.comicDAO = new ComicDAO();
        this.userDAO = new UserDao(jdbi);
        this.orderHistoryDAO = new OrderHistoryDAO();
        this.orderReturnDAO = new OrderReturnDAO();
    }

    /**
     * Lấy tất cả đơn hàng kèm thông tin đầy đủ
     */
    public Map<String, Object> getAllOrdersWithDetails() {
        Map<String, Object> result = new HashMap<>();

        try {
//            // Lấy tất cả đơn hàng
//            List<Order> allOrders = orderDAO.getAllOrders();

            // Phân loại theo trạng thái
            Map<String, List<Map<String, Object>>> ordersByStatus = new HashMap<>();
            ordersByStatus.put("Pending", getOrdersWithDetailsByStatus("Pending"));
            ordersByStatus.put("AwaitingPickup", getOrdersWithDetailsByStatus("AwaitingPickup"));
            ordersByStatus.put("Shipping", getOrdersWithDetailsByStatus("Shipping"));
            ordersByStatus.put("Completed", searchCompletedOrders(""));
            ordersByStatus.put("Returned", getOrdersWithDetailsByStatus("Returned"));
            ordersByStatus.put("Cancelled", getOrdersWithDetailsByStatus("Cancelled"));

            result.put("ordersByStatus", ordersByStatus);
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Lấy danh sách đơn hàng theo trạng thái kèm chi tiết
     */
    public List<Map<String, Object>> getOrdersWithDetailsByStatus(String status) {
        List<Order> orders = orderDAO.getOrdersByStatus(status);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderData = buildOrderData(order);
            result.add(orderData);
        }

        return result;
    }

    /**
     * Lấy chi tiết một đơn hàng
     */
    public Map<String, Object> getOrderDetail(int orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderDAO.getOrderById(orderId).orElse(null);

            if (order == null) {
                result.put("success", false);
                result.put("error", "Không tìm thấy đơn hàng");
                return result;
            }

            Map<String, Object> orderData = buildOrderData(order);
            result.put("success", true);
            result.put("order", orderData);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Xây dựng dữ liệu đơn hàng đầy đủ
     */
    private Map<String, Object> buildOrderData(Order order) {
        Map<String, Object> data = new HashMap<>();

        // Thông tin cơ bản của đơn hàng
        data.put("id", order.getId());
        data.put("orderCode", order.getId());
        data.put("userId", order.getUserId());


        try {
            Object orderDate = order.getOrderDate();
            String formattedDate = "";

            if (orderDate == null) {
                formattedDate = "N/A";
                data.put("orderDate", new Date());
            } else if (orderDate instanceof java.time.LocalDate) {
                java.time.LocalDate localDate = (java.time.LocalDate) orderDate;
                formattedDate = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                data.put("orderDate", Date.from(
                        localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                ));
            } else if (orderDate instanceof LocalDateTime) {
                LocalDateTime localDateTime = (LocalDateTime) orderDate;
                formattedDate = localDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                data.put("orderDate", Date.from(
                        localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
                ));
            } else if (orderDate instanceof java.sql.Timestamp) {
                java.sql.Timestamp timestamp = (java.sql.Timestamp) orderDate;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                formattedDate = sdf.format(timestamp);
                data.put("orderDate", timestamp);
            } else if (orderDate instanceof Date) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                formattedDate = sdf.format(orderDate);
                data.put("orderDate", orderDate);
            } else {
                formattedDate = "N/A";
                data.put("orderDate", new Date());
            }

            data.put("orderDateFormatted", formattedDate);

        } catch (Exception e) {
            e.printStackTrace();
            data.put("orderDate", new Date());
            data.put("orderDateFormatted", "N/A");
        }

        data.put("status", order.getStatus());
        data.put("totalAmount", order.getTotalAmount());
        data.put("formattedAmount", CurrencyFormatter.format(order.getTotalAmount()));

        data.put("recipientName", order.getRecipientName());
        data.put("shippingPhone", order.getShippingPhone());
        data.put("shippingAddress", order.getShippingAddress());
        data.put("fullAddress", order.getShippingAddress() + " (" + order.getShippingPhone() + ")");

        // Format hiển thị đơn vị vận chuyển
        String shippingProvider = order.getShippingProvider();
        String shippingProviderDisplay = "—";
        if (shippingProvider != null) {
            if ("standard".equalsIgnoreCase(shippingProvider)) {
                shippingProviderDisplay = "Tiêu chuẩn";
            } else if ("express".equalsIgnoreCase(shippingProvider)) {
                shippingProviderDisplay = "Hỏa tốc";
            } else {
                shippingProviderDisplay = shippingProvider;
            }
        }

        data.put("shippingProvider", shippingProviderDisplay);
        data.put("shippingFee", order.getShippingFee());
        data.put("formattedShippingFee", CurrencyFormatter.format(order.getShippingFee()));
        data.put("pointUsed", order.getPointUsed());

        // LẤY LÝ DO HỦY TỪ ORDER_HISTORY (NẾU ĐƠN BỊ HỦY)
        if ("Cancelled".equals(order.getStatus())) {
            try {
                Optional<OrderHistory> history = orderHistoryDAO.getCancellationReason(order.getId());
                if (history.isPresent()) {
                    data.put("cancellationReason", history.get().getReason() != null ? history.get().getReason() : "Không có lý do");
                    data.put("cancelledBy", history.get().getChangedBy());
                    LocalDateTime cancelledAt = history.get().getChangedAt();
                    if (cancelledAt != null) {
                        Date cancelledDate = Date.from(
                                cancelledAt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                        );
                        data.put("cancelledAt", cancelledDate);
                    }
                } else {
                    data.put("cancellationReason", "Không có lý do");
                    data.put("cancelledBy", "N/A");
                    data.put("cancelledAt", null);
                }
                enrichSingleCancelledBy(data);
            } catch (Exception e) {
                data.put("cancellationReason", "Không có lý do");
                data.put("cancelledBy", "N/A");
                data.put("cancelledAt", null);
            }
        }

        data.put("userName", order.getRecipientName());
        try {
            User user = userDAO.getUserById(order.getUserId());
            if (user != null) {
                data.put("userEmail", user.getEmail());
            } else {
                data.put("userEmail", "");
            }
        } catch (Exception e) {
            data.put("userEmail", "");
        }

        // Lấy thông tin payment
        Payment payment = paymentDAO.getPaymentByOrderId(order.getId()).orElse(null);
        if (payment != null) {
            data.put("paymentMethod", payment.getPaymentMethod());
            data.put("paymentStatus", payment.getPaymentStatus());
            data.put("transactionId", payment.getTransactionId());

            if ("COD".equals(payment.getPaymentMethod())) {
                data.put("paymentMethodDisplay", "COD");
            } else if ("Completed".equals(payment.getPaymentStatus())) {
                data.put("paymentMethodDisplay", "Đã thanh toán");
            } else {
                data.put("paymentMethodDisplay", payment.getPaymentMethod());
            }

            switch (payment.getPaymentStatus()) {
                case "Completed":
                    data.put("paymentStatusDisplay", "Đã thanh toán");
                    break;
                case "Pending":
                    data.put("paymentStatusDisplay", "Chờ thanh toán");
                    break;
                case "Failed":
                    data.put("paymentStatusDisplay", "Thất bại");
                    break;
                default:
                    data.put("paymentStatusDisplay", payment.getPaymentStatus());
            }
        } else {
            data.put("paymentMethod", "");
            data.put("paymentMethodDisplay", "—");
            data.put("paymentStatusDisplay", "—");
        }


        List<OrderItem> orderItems = orderDAO.getOrderItems(order.getId());
        List<Map<String, Object>> itemsData = new ArrayList<>();
        StringBuilder productSummary = new StringBuilder();

        FlashSaleComicsDAO flashSaleComicsDAO = new FlashSaleComicsDAO();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            Map<String, Object> itemData = new HashMap<>();

            itemData.put("id", item.getId());
            itemData.put("comicId", item.getComicId());
            itemData.put("quantity", item.getQuantity());

            itemData.put("priceAtPurchase", item.getPriceAtPurchase());
            itemData.put("formattedPriceAtPurchase", CurrencyFormatter.format(item.getPriceAtPurchase()));

            // Lấy thông tin comic
            try {
                Comic comic = comicDAO.getComicById(item.getComicId());
                if (comic != null) {
                    itemData.put("comicName", comic.getNameComics());
                    itemData.put("comicImage", comic.getThumbnailUrl());

                    Map<String, Object> flashSaleInfo = flashSaleComicsDAO.getFlashSaleInfoByComicId(item.getComicId());

                    double displayPrice = item.getPriceAtPurchase(); // Mặc định là giá đã mua
                    boolean hasActiveFlashSale = false;

                    if (flashSaleInfo != null) {
                        hasActiveFlashSale = true;

                        Object discountObj = flashSaleInfo.get("discount_percent");
                        if (discountObj instanceof Number) {
                            double discountPercent = ((Number) discountObj).doubleValue();
                            // Tính giá Flash Sale hiện tại từ GIÁ GỐC
                            displayPrice = comic.getPrice() * (1 - discountPercent / 100.0);

                            itemData.put("flashSaleName", flashSaleInfo.get("flashsale_name"));
                            itemData.put("flashSaleDiscount", discountPercent);
                        }
                    } else {
                        if (comic.hasDiscount()) {
                            displayPrice = comic.getDiscountPrice();
                        } else {
                            displayPrice = comic.getPrice(); // Giá gốc
                        }
                    }

                    itemData.put("currentDisplayPrice", displayPrice);
                    itemData.put("formattedDisplayPrice", CurrencyFormatter.format(displayPrice));
                    itemData.put("hasActiveFlashSale", hasActiveFlashSale);

                    boolean priceChanged = Math.abs(displayPrice - item.getPriceAtPurchase()) > 0.01;
                    itemData.put("priceChanged", priceChanged);

                    // Thêm vào product summary
                    if (i > 0) productSummary.append(", ");
                    productSummary.append(comic.getNameComics())
                            .append(" (")
                            .append(item.getQuantity())
                            .append(")");
                } else {
                    itemData.put("comicName", "Sản phẩm #" + item.getComicId());
                    itemData.put("comicImage", "");
                    itemData.put("hasActiveFlashSale", false);
                    itemData.put("priceChanged", false);
                    itemData.put("currentDisplayPrice", item.getPriceAtPurchase());
                    itemData.put("formattedDisplayPrice", CurrencyFormatter.format(item.getPriceAtPurchase()));
                }
            } catch (Exception e) {
                itemData.put("comicName", "Sản phẩm #" + item.getComicId());
                itemData.put("comicImage", "");
                itemData.put("hasActiveFlashSale", false);
                itemData.put("priceChanged", false);
                itemData.put("currentDisplayPrice", item.getPriceAtPurchase());
                itemData.put("formattedDisplayPrice", CurrencyFormatter.format(item.getPriceAtPurchase()));
            }

            itemsData.add(itemData);
        }

        data.put("items", itemsData);
        data.put("productSummary", productSummary.toString());
        data.put("itemCount", orderItems.size());

        return data;
    }

    /**
     * Xác nhận đơn hàng
     */
    public Map<String, Object> confirmOrder(int orderId, String shippingProvider) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderDAO.getOrderById(orderId).orElse(null);
            if (order == null) {
                return Map.of("success", false, "message", "Không tìm thấy đơn hàng");
            }
            String oldStatus = order.getStatus();

            // Cập nhật đơn vị vận chuyển nếu có
            if (shippingProvider != null && !shippingProvider.trim().isEmpty()) {
                orderDAO.updateShippingProvider(orderId, shippingProvider);
            }

            // Chuyển sang trạng thái AwaitingPickup
            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, "AwaitingPickup");

            if (success) {
                OrderHistory history = new OrderHistory(
                        orderId,
                        oldStatus,
                        "AwaitingPickup",
                        "Admin",
                        null  // Không có lý do khi xác nhận
                );
                orderHistoryDAO.addHistory(history);
                result.put("success", true);
                result.put("message", "Đã xác nhận đơn hàng thành công");
            } else {
                result.put("success", false);
                result.put("message", "Không thể xác nhận đơn hàng");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Hủy đơn hàng
     */
    public Map<String, Object> cancelOrder(int orderId, String reason) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy thông tin đơn hàng trước khi hủy
            Order order = orderDAO.getOrderById(orderId).orElse(null);
            if (order == null) {
                return Map.of("success", false, "message", "Không tìm thấy đơn hàng");
            }

            String oldStatus = order.getStatus();

            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, "Cancelled");

            if (success) {
                OrderHistory history = new OrderHistory(
                        orderId,
                        oldStatus,
                        "Cancelled",
                        "Admin",  // Hoặc lấy từ session
                        reason != null ? reason : "Không có lý do"
                );

                orderHistoryDAO.addHistory(history);
                result.put("success", true);
                result.put("message", "Đã hủy đơn hàng. Xu và tồn kho đã được hoàn lại.");
            } else {
                result.put("success", false);
                result.put("message", "Không thể hủy đơn hàng");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Xác nhận đã giao cho đơn vị vận chuyển
     */
    public Map<String, Object> confirmShipped(int orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderDAO.getOrderById(orderId).orElse(null);
            String ghnOrderCode = GHNConfig.createOrder(order);
            if (ghnOrderCode == null) {
                throw new RuntimeException("Không tạo được đơn GHN");
            }
            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, "Shipping");
            if (success) {
                result.put("success", true);
                result.put("message", "Đã xác nhận giao cho ĐVVC");
            } else {
                result.put("success", false);
                result.put("message", "Không thể cập nhật trạng thái");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Xác nhận đã giao hàng thành công
     */
    public Map<String, Object> confirmDelivered(int orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, "Completed");

            if (success) {
                result.put("success", true);
                result.put("message", "Đã xác nhận giao hàng thành công. Khách hàng đã được cộng xu.");
            } else {
                result.put("success", false);
                result.put("message", "Không thể cập nhật trạng thái");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    public Map<String, Object> updateOrderStatus(int orderId, String newStatus) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Sử dụng updateOrderStatusWithPoints để tự động xử lý xu
            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, newStatus);

            if (success) {
                result.put("success", true);
                result.put("message", "Đã cập nhật trạng thái đơn hàng");
            } else {
                result.put("success", false);
                result.put("message", "Không thể cập nhật trạng thái");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Xử lý yêu cầu hoàn tiền
     */
    public Map<String, Object> processRefund(int orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Chuyển trạng thái sang Returned và hoàn xu, tồn kho
            boolean success = orderDAO.updateOrderStatusWithPoints(orderId, "Returned");

            if (success) {
                result.put("success", true);
                result.put("message", "Đã xác nhận hoàn tiền. Xu và tồn kho đã được hoàn lại.");
            } else {
                result.put("success", false);
                result.put("message", "Không thể xử lý hoàn tiền");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Lấy thống kê đơn hàng
     */
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalOrders", orderDAO.getAllOrders().size());
            stats.put("pendingCount", orderDAO.getOrdersByStatus("Pending").size());
            stats.put("awaitingPickupCount", orderDAO.getOrdersByStatus("AwaitingPickup").size());
            stats.put("shippingCount", orderDAO.getOrdersByStatus("Shipping").size());
            stats.put("completedCount", orderDAO.getOrdersByStatus("Completed").size());
            stats.put("returnedCount", orderDAO.getOrdersByStatus("Returned").size());
            stats.put("cancelledCount", orderDAO.getOrdersByStatus("Cancelled").size());
            stats.put("totalRevenue", orderDAO.getTotalRevenue());

        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    public List<Map<String, Object>> searchOrders(String keyword, String status) {
        if (keyword == null) keyword = "";
        if (status == null || status.isEmpty()) status = "Pending";

        List<Map<String, Object>> orders = orderDAO.searchOrders(keyword.trim(), status);

        // ✅ SỬ DỤNG buildOrderData ĐỂ FORMAT ĐỒNG NHẤT
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> orderMap : orders) {
            int orderId = ((Number) orderMap.get("id")).intValue();

            // ✅ LẤY ORDER OBJECT ĐẦY ĐỦ
            Order order = orderDAO.getOrderById(orderId).orElse(null);

            if (order != null) {
                // ✅ DÙNG buildOrderData ĐỂ FORMAT GIỐNG HỆT NHƯ LÚC LOAD TRANG
                Map<String, Object> formattedOrder = buildOrderData(order);
                result.add(formattedOrder);
            }
        }

        return result;
    }


    /**
     * Tìm kiếm đơn hàng theo tab cụ thể
     */
    public List<Map<String, Object>> searchOrdersByTab(String keyword, String status) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            List<Map<String, Object>> orders;

            if ("Cancelled".equalsIgnoreCase(status)) {
                orders = orderDAO.searchCancelledOrders(keyword);
            } else {
                orders = orderDAO.searchOrdersByStatus(keyword, status);
            }

            for (Map<String, Object> orderMap : orders) {
                int orderId = ((Number) orderMap.get("id")).intValue();

                Order order = orderDAO.getOrderById(orderId).orElse(null);

                if (order != null) {
                    Map<String, Object> formattedOrder = buildOrderData(order);
                    result.add(formattedOrder);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in searchOrdersByTab: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tìm kiếm đơn hàng cho tab CHỜ XÁC NHẬN
     */
    public List<Map<String, Object>> searchPendingOrders(String keyword) {
        return searchOrdersByTab(keyword, "Pending");
    }

    /**
     * Tìm kiếm đơn hàng cho tab CHỜ LẤY HÀNG
     */
    public List<Map<String, Object>> searchAwaitingPickupOrders(String keyword) {
        return searchOrdersByTab(keyword, "AwaitingPickup");
    }

    /**
     * Tìm kiếm đơn hàng cho tab ĐANG GIAO
     */
    public List<Map<String, Object>> searchShippingOrders(String keyword) {
        return searchOrdersByTab(keyword, "Shipping");
    }

    /**
     * Tìm kiếm đơn hàng cho tab TRẢ HÀNG/HOÀN TIỀN
     */
    public List<Map<String, Object>> searchReturnedOrders(String keyword) {
        try {
            List<Map<String, Object>> returns = orderReturnDAO.searchReturns(keyword);
            for (Map<String, Object> returnOrder : returns) {
                formatReturnOrderData(returnOrder);
            }
            return returns;
        } catch (Exception e) {
            System.err.println("Error in searchReturnedOrders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Tìm kiếm đơn hàng cho tab ĐƠN BỊ HỦY
     */
    public List<Map<String, Object>> searchCancelledOrders(String keyword) {
        List<Map<String, Object>> orders = searchOrdersByTab(keyword, "Cancelled");
        enrichCancelledByName(orders);
        return orders;
    }

    private void enrichCancelledByName(List<Map<String, Object>> orders) {
        for (Map<String, Object> order : orders) {
            enrichSingleCancelledBy(order);
        }
    }

    private void enrichSingleCancelledBy(Map<String, Object> order) {
        Object cancelledByObj = order.get("cancelledBy");
        if (cancelledByObj == null) return;
        String cancelledBy = cancelledByObj.toString().trim();
        if ("Admin".equalsIgnoreCase(cancelledBy)) {
            order.put("cancelledBy", "Admin");
            return;
        }

        try {
            int userId = -1;

            if (cancelledBy.matches("\\d+")) {
                userId = Integer.parseInt(cancelledBy);
            } else if ("Customer".equalsIgnoreCase(cancelledBy)) {
                Object userIdObj = order.get("userId");
                if (userIdObj != null) {
                    userId = ((Number) userIdObj).intValue();
                }
            }

            if (userId > 0) {
                User user = userDAO.getUserById(userId);
                if (user != null) {
                    String displayName = user.getFullName() != null
                            ? user.getFullName()
                            : user.getEmail();
                    order.put("cancelledBy", "Customer");
                    order.put("cancelledByName", displayName);
                } else {
                    order.put("cancelledBy", "Customer");
                    order.put("cancelledByName", "Khách hàng");
                }
            }
        } catch (Exception e) {
            order.put("cancelledBy", "Customer");
            order.put("cancelledByName", "Khách hàng");
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng và tự động cập nhật total_spent
     */
    public boolean updateOrderStatusAndSync(int orderId, String newStatus) {
        // Cập nhật trạng thái đơn hàng
        boolean orderUpdated = orderDAO.updateOrderStatusWithPoints(orderId, newStatus);

//        // Nếu trạng thái mới là "completed", tự động cập nhật total_spent
//        if ("Completed".equalsIgnoreCase(newStatus)) {
//            Optional<Order> order = orderDAO.getOrderById(orderId);
//            if (order.isPresent() && order.get().getUserId() > 0) {
//                // Đồng bộ total_spent cho user
//                userDAO.syncTotalSpentFromOrders(order.get().getUserId());
//                System.out.println("Đã tự động cập nhật total_spent cho user ID: " + order.get().getUserId());
//            }
//        }
        return orderUpdated;
    }

    /**
     * Tìm kiếm đơn hàng đã giao với thông tin đánh giá
     */
    public List<Map<String, Object>> searchCompletedOrders(String keyword) {
        try {
            // Lấy danh sách đơn hàng đã giao kèm rating từ DAO
            List<Map<String, Object>> orders = orderDAO.searchCompletedOrdersWithRating(keyword);

            // Format dữ liệu cho từng đơn hàng
            for (Map<String, Object> order : orders) {

                Object totalAmountObj = order.get("total_amount");
                if (totalAmountObj != null) {
                    double totalAmount = 0.0;
                    if (totalAmountObj instanceof Number) {
                        totalAmount = ((Number) totalAmountObj).doubleValue();
                    }
                    order.put("formattedAmount", CurrencyFormatter.format(totalAmount));
                }

                String paymentMethod = (String) order.get("payment_method");

                String paymentMethodDisplay = "COD";
                if ("COD".equalsIgnoreCase(paymentMethod)) {
                    paymentMethodDisplay = "COD";
                } else if ("VNPay".equalsIgnoreCase(paymentMethod)) {
                    paymentMethodDisplay = "VNPay";
                } else if (paymentMethod != null) {
                    paymentMethodDisplay = paymentMethod;
                }
                order.put("paymentMethodDisplay", paymentMethodDisplay);

                Object transactionIdObj = order.get("transaction_id");
                if (transactionIdObj != null && !transactionIdObj.toString().isEmpty()) {
                    order.put("transactionId", transactionIdObj.toString());
                    order.put("hasTransaction", true);
                } else {
                    order.put("transactionId", null);
                    order.put("hasTransaction", false);
                }

                Object avgRatingObj = order.get("average_rating");
                Object reviewCountObj = order.get("review_count");

                int reviewCount = reviewCountObj != null ?
                        ((Number) reviewCountObj).intValue() : 0;

                boolean hasReview = reviewCount > 0;
                order.put("hasReview", hasReview);

                if (hasReview && avgRatingObj != null) {
                    double avgRating = ((Number) avgRatingObj).doubleValue();
                    order.put("averageRating", avgRating);
                    order.put("formattedRating", String.format("%.1f", avgRating));

                    // Tính số sao đầy và sao nửa
                    int fullStars = (int) avgRating;
                    boolean hasHalfStar = (avgRating - fullStars) >= 0.5;

                    order.put("fullStars", fullStars);
                    order.put("hasHalfStar", hasHalfStar);
                    order.put("emptyStars", 5 - fullStars - (hasHalfStar ? 1 : 0));
                } else {
                    order.put("averageRating", null);
                    order.put("formattedRating", "-");
                    order.put("fullStars", 0);
                    order.put("hasHalfStar", false);
                    order.put("emptyStars", 0);
                }
            }

            return orders;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả yêu cầu hoàn trả với thông tin chi tiết
     */
    public List<Map<String, Object>> getAllReturnsWithDetails() {
        try {
            List<Map<String, Object>> returns = orderReturnDAO.getAllReturnsWithDetails();

            for (Map<String, Object> returnOrder : returns) {
                formatReturnOrderData(returnOrder);
            }

            return returns;

        } catch (Exception e) {
            System.err.println("Error in getAllReturnsWithDetails: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Tìm kiếm đơn hoàn trả theo keyword
     */
    public List<Map<String, Object>> searchReturns(String keyword) {
        try {
            List<Map<String, Object>> returns = orderReturnDAO.searchReturns(keyword);

            // Format dữ liệu
            for (Map<String, Object> returnOrder : returns) {
                formatReturnOrderData(returnOrder);
            }

            return returns;

        } catch (Exception e) {
            System.err.println("Error in searchReturns: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Xác nhận hoàn tiền
     */
    public Map<String, Object> confirmRefund(int returnId) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean success = orderReturnDAO.confirmRefund(returnId);

            if (success) {
                result.put("success", true);
                result.put("message", "Đã xác nhận hoàn tiền thành công");
            } else {
                result.put("success", false);
                result.put("message", "Không thể xác nhận hoàn tiền");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Từ chối yêu cầu hoàn tiền
     */
    public Map<String, Object> rejectRefund(int returnId, String rejectReason) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Vui lòng nhập lý do từ chối");
                return result;
            }

            boolean success = orderReturnDAO.rejectRefund(returnId, rejectReason);

            if (success) {
                result.put("success", true);
                result.put("message", "Đã từ chối yêu cầu hoàn tiền");
            } else {
                result.put("success", false);
                result.put("message", "Không thể từ chối yêu cầu");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Lấy chi tiết đơn hoàn trả
     */
    public Map<String, Object> getReturnDetail(int returnId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> returnDetail = orderReturnDAO.getReturnDetailsById(returnId);

            if (returnDetail != null) {
                formatReturnOrderData(returnDetail);

                List<OrderReturnImage> images = orderReturnDAO.getReturnImages(returnId);
                List<Map<String, Object>> imageData = new ArrayList<>();

                for (OrderReturnImage img : images) {
                    Map<String, Object> imgMap = new HashMap<>();
                    imgMap.put("id", img.getId());
                    imgMap.put("urlImg", img.getUrlImg());

                    if (img.getCreatedAt() != null) {
                        imgMap.put("createdAt", img.getCreatedAt().format(DATE_TIME_FORMATTER));
                    }

                    imageData.add(imgMap);
                }

                returnDetail.put("proof_images", imageData);

                result.put("success", true);
                result.put("return", returnDetail);
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy yêu cầu hoàn trả");
            }
        } catch (Exception e) {
            System.err.println("Error in getReturnDetail: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Format dữ liệu đơn hoàn trả
     */
    private void formatReturnOrderData(Map<String, Object> returnOrder) {
        try {
            // Lấy danh sách ảnh minh chứng và convert sang Map
            Object returnIdObj = returnOrder.get("return_id");
            if (returnIdObj != null) {
                int returnId = ((Number) returnIdObj).intValue();
                List<OrderReturnImage> images = orderReturnDAO.getReturnImages(returnId);

                List<Map<String, Object>> imageData = new ArrayList<>();
                for (OrderReturnImage img : images) {
                    Map<String, Object> imgMap = new HashMap<>();
                    imgMap.put("id", img.getId());
                    imgMap.put("urlImg", img.getUrlImg());

                    if (img.getCreatedAt() != null) {
                        imgMap.put("createdAt", img.getCreatedAt().format(DATE_TIME_FORMATTER));
                    }
                    imageData.add(imgMap);
                }

                returnOrder.put("proof_images", imageData);
            }

            String returnStatus = (String) returnOrder.get("return_status");
            returnOrder.put("status_class", getReturnStatusClass(returnStatus));
            returnOrder.put("status_display", getReturnStatusDisplay(returnStatus));

            Object returnDateObj = returnOrder.get("return_date");
            if (returnDateObj != null) {
                String formattedDate = null;

                if (returnDateObj instanceof LocalDateTime) {
                    LocalDateTime localDateTime = (LocalDateTime) returnDateObj;
                    formattedDate = localDateTime.format(DATE_TIME_FORMATTER);
                } else if (returnDateObj instanceof java.sql.Timestamp) {
                    java.sql.Timestamp timestamp = (java.sql.Timestamp) returnDateObj;
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                    formattedDate = sdf.format(timestamp);
                }

                if (formattedDate != null) {
                    returnOrder.put("return_date", formattedDate);
                    returnOrder.put("formatted_return_date", formattedDate);
                }
            }

            Object refundAmountObj = returnOrder.get("refund_amount");
            if (refundAmountObj != null) {
                double refundAmount = ((Number) refundAmountObj).doubleValue();
                returnOrder.put("formatted_refund_amount", CurrencyFormatter.format(refundAmount));
            }

        } catch (Exception e) {
            System.err.println("Error formatting return order data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy CSS class cho status hoàn trả
     */
    private String getReturnStatusClass(String status) {
        if (status == null) return "yellow";

        switch (status) {
            case "Pending":
                return "yellow";
            case "Refunded":
                return "green";
            case "Rejected":
                return "red";
            default:
                return "yellow";
        }
    }

    /**
     * Lấy text hiển thị cho status hoàn trả
     */
    private String getReturnStatusDisplay(String status) {
        if (status == null) return "Đang xem xét";

        switch (status) {
            case "Pending":
                return "Đang xem xét";
            case "Refunded":
                return "Đã hoàn tiền";
            case "Rejected":
                return "Đã từ chối";
            default:
                return status;
        }
    }

    /**
     * Lấy thông tin cơ bản của đơn hàng để gửi thông báo
     */
    public Map<String, Object> getOrderBasicInfo(int orderId) {
        String sql = """
        SELECT 
            o.user_id,
            o.id as ordercode, 
            o.shipping_provider
        FROM orders o
        WHERE o.id = ?
    """;

        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, orderId)
                        .mapToMap()
                        .findFirst()
                        .orElse(null)
        );
    }

    /**
     * Lấy thông tin cơ bản của đơn hoàn để gửi thông báo
     */
    public Map<String, Object> getReturnBasicInfo(int returnId) {
        String sql = """
        SELECT 
            o.user_id AS userId,
            o.id AS orderCode,
            r.refund_amount AS refundAmount
        FROM order_returns r
        JOIN orders o ON r.order_id = o.id
        WHERE r.id = ?
    """;

        try {
            Map<String, Object> result = JdbiConnector.get().withHandle(handle ->
                    handle.createQuery(sql)
                            .bind(0, returnId)
                            .mapToMap()
                            .findFirst()
                            .orElse(null)
            );

            if (result != null && result.get("refundAmount") != null) {
                double amount = ((Number) result.get("refundAmount")).doubleValue();
                result.put("formattedRefundAmount", CurrencyFormatter.format(amount));
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error getting return basic info: " + e.getMessage());
            return null;
        }
    }
}