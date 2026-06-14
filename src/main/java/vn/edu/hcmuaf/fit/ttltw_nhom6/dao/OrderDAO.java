package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Order;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderDAO extends ADao {
    public final Jdbi jdbi;
    private ComicDAO comicDAO;
    private PaymentDAO paymentDAO;

    public OrderDAO() {
        this.jdbi = JdbiConnector.get();
        comicDAO = new ComicDAO(jdbi);
        paymentDAO = new PaymentDAO(jdbi);
    }

    public OrderDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.comicDAO = new ComicDAO(jdbi);
        this.paymentDAO = new PaymentDAO(jdbi);
    }


    public int createOrderWithPayment(Order order, List<OrderItem> orderItems, String paymentMethod) {
        return jdbi.inTransaction(handle -> {
            for (OrderItem item : orderItems) {
                int stockQuantity = handle.createQuery(
                                "SELECT stock_quantity FROM comics WHERE id = ?")
                        .bind(0, item.getComicId())
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(0);

                if (stockQuantity < item.getQuantity()) {
                    throw new RuntimeException(
                            "Sản phẩm ID " + item.getComicId() +
                                    " không đủ hàng. Còn lại: " + stockQuantity +
                                    ", yêu cầu: " + item.getQuantity()
                    );
                }
            }

            String insertOrderSql = "INSERT INTO orders " +
                    "(user_id, order_date, status, total_amount, shipping_address_id, " +
                    "recipient_name, shipping_phone, shipping_address, shipping_provider, " +
                    "shipping_fee, points_used, tracking_code, created_at, " +
                    "product_discount, shipping_discount) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int orderId = handle.createUpdate(insertOrderSql)
                    .bind(0, order.getUserId())
                    .bind(1, order.getOrderDate())
                    .bind(2, order.getStatus())
                    .bind(3, order.getTotalAmount())
                    .bind(4, order.getShippingAddressId())
                    .bind(5, order.getRecipientName())
                    .bind(6, order.getShippingPhone())
                    .bind(7, order.getShippingAddress())
                    .bind(8, order.getShippingProvider())
                    .bind(9, order.getShippingFee())
                    .bind(10, order.getPointUsed())
                    .bind(11, order.getTrackingCode())
                    .bind(12, order.getCreatedAt())
                    .bind(13, order.getProductDiscount())
                    .bind(14, order.getShippingDiscount())
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Integer.class)
                    .one();

            if (orderItems != null && !orderItems.isEmpty()) {
                String insertItemSql = "INSERT INTO order_items " +
                        "(order_id, comic_id, quantity, price_at_purchase, flashsale_id) " +
                        "VALUES (?, ?, ?, ?, ?)";

                var batch = handle.prepareBatch(insertItemSql);

                for (OrderItem item : orderItems) {
                    batch.bind(0, orderId)
                            .bind(1, item.getComicId())
                            .bind(2, item.getQuantity())
                            .bind(3, item.getPriceAtPurchase())
                            .bind(4, item.getFlashSaleId())
                            .add();

                    String updateStockSql = "UPDATE comics SET stock_quantity = stock_quantity - ? " +
                            "WHERE id = ? AND stock_quantity >= ?";

                    int updated = handle.createUpdate(updateStockSql)
                            .bind(0, item.getQuantity())
                            .bind(1, item.getComicId())
                            .bind(2, item.getQuantity())
                            .execute();

                    if (updated == 0) {
                        throw new RuntimeException(
                                "Không thể trừ tồn kho cho sản phẩm ID: " + item.getComicId()
                        );
                    }

                    handle.createUpdate("INSERT INTO inventory_transaction (comic_id, type, quantity, note, created_at) VALUES (?, 'EXPORT', ?, ?, NOW())")
                            .bind(0, item.getComicId())
                            .bind(1, item.getQuantity())
                            .bind(2, "Bán hàng qua đơn hàng #" + orderId)
                            .execute();
                }

                batch.execute();
            }

            String insertPaymentSql = "INSERT INTO payments " +
                    "(order_id, payment_method, payment_status, transaction_id, " +
                    "amount, payment_date, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            String paymentStatus = "COD".equals(paymentMethod) ? "Pending" : "Pending";
            String transactionId = null;
            LocalDateTime paymentDate = null;
            LocalDateTime now = LocalDateTime.now();

            handle.createUpdate(insertPaymentSql)
                    .bind(0, orderId)
                    .bind(1, paymentMethod)
                    .bind(2, paymentStatus)
                    .bind(3, transactionId)
                    .bind(4, order.getTotalAmount())
                    .bind(5, paymentDate)
                    .bind(6, now)
                    .bind(7, now)
                    .execute();

            return orderId;
        });
    }



    public Optional<Order> getOrderById(int orderId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM orders WHERE id = ?")
                        .bind(0, orderId)
                        .mapToBean(Order.class)
                        .findFirst()
        );
    }


    public List<Order> getOrdersByUserId(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC")
                        .bind(0, userId)
                        .mapToBean(Order.class)
                        .list()
        );
    }


    public List<OrderItem> getOrderItems(int orderId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM order_items WHERE order_id = ?")
                        .bind(0, orderId)
                        .mapToBean(OrderItem.class)
                        .list()
        );
    }


    public boolean updateOrderStatus(int orderId, String status) {
        return jdbi.withHandle(handle -> {
            String sql;
            switch (status) {
                case "Returned":
                    sql = "UPDATE orders SET status = ?, returned_at = NOW() WHERE id = ?";
                    break;
                case "Cancelled":
                    sql = "UPDATE orders SET status = ?, cancelled_at = NOW() WHERE id = ?";
                    break;
                default:
                    sql = "UPDATE orders SET status = ? WHERE id = ?";
            }
            int updated = handle.createUpdate(sql)
                    .bind(0, status)
                    .bind(1, orderId)
                    .execute();
            return updated > 0;
        });
    }


    public List<Order> getAllOrders() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM orders ORDER BY order_date DESC")
                        .mapToBean(Order.class)
                        .list()
        );
    }


    public List<Order> getOrdersByStatus(String status) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM orders WHERE status = ? ORDER BY order_date DESC")
                        .bind(0, status)
                        .mapToBean(Order.class)
                        .list()
        );
    }


    public double getTotalRevenue() {
        return jdbi.withHandle(handle ->
                handle.createQuery(
                                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'Completed'")
                        .mapTo(Double.class)
                        .one()
        );
    }


    public boolean updateOrderStatusWithPoints(int orderId, String newStatus) {
        return jdbi.inTransaction(handle -> {
            Order order = handle.createQuery("SELECT * FROM orders WHERE id = ?")
                    .bind(0, orderId)
                    .mapToBean(Order.class)
                    .findOne()
                    .orElse(null);

            if (order == null) {
                return false;
            }

            String oldStatus = order.getStatus();
            int userId = order.getUserId();
            double totalAmount = order.getTotalAmount();

            String updateSql;
            switch (newStatus) {
                case "AwaitingPickup":
                    updateSql = "UPDATE orders SET status = ?, confirmed_at = NOW() WHERE id = ?";
                    break;
                case "Shipping":
                    updateSql = "UPDATE orders SET status = ?, shipping_at = NOW() WHERE id = ?";
                    break;
                case "Completed":
                    updateSql = """
        UPDATE orders
        SET status = ?,
            delivered_at = COALESCE(delivered_at, NOW()),
            completed_at = NOW()
        WHERE id = ?
        """;
                    break;
                case "Cancelled":
                    updateSql = "UPDATE orders SET status = ?, cancelled_at = NOW() WHERE id = ?";
                    break;
                case "Returned":
                    updateSql = "UPDATE orders SET status = ?, returned_at = NOW() WHERE id = ?";
                    break;
                default:
                    updateSql = "UPDATE orders SET status = ? WHERE id = ?";
            }

            int updated = handle.createUpdate(updateSql)
                    .bind(0, newStatus)
                    .bind(1, orderId)
                    .execute();

            if (updated == 0) {
                return false;
            }

            if ("Completed".equalsIgnoreCase(newStatus) &&
                    !"Completed".equalsIgnoreCase(oldStatus)) {

                int earnedPoints = 200;

                handle.createUpdate(
                                "UPDATE users SET " +
                                        "total_spent = total_spent + ?, " +
                                        "points = points + ?, " +
                                        "updated_at = NOW() " +
                                        "WHERE id = ?"
                        )
                        .bind(0, totalAmount)
                        .bind(1, earnedPoints)
                        .bind(2, userId)
                        .execute();

                // Ghi log giao dịch xu
                String insertTransactionSql = "INSERT INTO PointTransactions " +
                        "(user_id, order_id, points, transaction_type, description, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                handle.createUpdate(insertTransactionSql)
                        .bind(0, userId)
                        .bind(1, orderId)
                        .bind(2, earnedPoints)
                        .bind(3, "EARN")
                        .bind(4, "Nhận " + earnedPoints + " xu từ đơn hàng #" + orderId +
                                " (Giá trị đơn: " + String.format("%,.0f", totalAmount) + "đ)")
                        .bind(5, LocalDateTime.now())
                        .execute();
            }

            if ("Cancelled".equalsIgnoreCase(newStatus) &&
                    !"Cancelled".equalsIgnoreCase(oldStatus)) {

                if (order.getPointUsed() != 0 && order.getPointUsed() > 0) {
                    handle.createUpdate("UPDATE users SET points = points + ?, updated_at = NOW() WHERE id = ?")
                            .bind(0, order.getPointUsed())
                            .bind(1, userId)
                            .execute();
                    String insertTransactionSql = "INSERT INTO PointTransactions " +
                            "(user_id, order_id, points, transaction_type, description, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";

                    handle.createUpdate(insertTransactionSql)
                            .bind(0, userId)
                            .bind(1, orderId)
                            .bind(2, order.getPointUsed())
                            .bind(3, "REFUND")
                            .bind(4, "Hoàn " + order.getPointUsed() +
                                    " xu do hủy đơn hàng #" + orderId)
                            .bind(5, LocalDateTime.now())
                            .execute();
                }

                // Hoàn lại tồn kho
                List<OrderItem> orderItems = handle.createQuery(
                                "SELECT * FROM order_items WHERE order_id = ?")
                        .bind(0, orderId)
                        .mapToBean(OrderItem.class)
                        .list();

                for (OrderItem item : orderItems) {
                    handle.createUpdate(
                                    "UPDATE comics SET stock_quantity = stock_quantity + ? WHERE id = ?")
                            .bind(0, item.getQuantity())
                            .bind(1, item.getComicId())
                            .execute();

                    handle.createUpdate("INSERT INTO inventory_transaction (comic_id, type, quantity, note, created_at) VALUES (?, 'IMPORT', ?, ?, NOW())")
                            .bind(0, item.getComicId())
                            .bind(1, item.getQuantity())
                            .bind(2, "Hoàn kho từ đơn hàng bị hủy #" + orderId)
                            .execute();
                }
            }

            if ("Returned".equalsIgnoreCase(newStatus) &&
                    !"Returned".equalsIgnoreCase(oldStatus)) {

                if (order.getPointUsed() != 0 && order.getPointUsed() > 0) {
                    handle.createUpdate("UPDATE users SET points = points + ?, updated_at = NOW() WHERE id = ?")
                            .bind(0, order.getPointUsed())
                            .bind(1, userId)
                            .execute();

                }

                // Hoàn tồn kho
                List<OrderItem> orderItems = handle.createQuery(
                                "SELECT * FROM order_items WHERE order_id = ?")
                        .bind(0, orderId)
                        .mapToBean(OrderItem.class)
                        .list();

                for (OrderItem item : orderItems) {
                    handle.createUpdate(
                                    "UPDATE comics SET stock_quantity = stock_quantity + ? WHERE id = ?")
                            .bind(0, item.getQuantity())
                            .bind(1, item.getComicId())
                            .execute();

                    handle.createUpdate("INSERT INTO inventory_transaction (comic_id, type, quantity, note, created_at) VALUES (?, 'IMPORT', ?, ?, NOW())")
                            .bind(0, item.getComicId())
                            .bind(1, item.getQuantity())
                            .bind(2, "Hoàn kho từ đơn hàng bị trả #" + orderId)
                            .execute();
                }
            }

            return true;
        });
    }

    /**
     * Cập nhật đơn vị vận chuyển
     */
    public boolean updateShippingProvider(int orderId, String shippingProvider) {
        return jdbi.withHandle(handle -> {
            int updated = handle.createUpdate(
                            "UPDATE orders SET shipping_provider = ? WHERE id = ?")
                    .bind(0, shippingProvider)
                    .bind(1, orderId)
                    .execute();
            return updated > 0;
        });
    }

    public List<Map<String, Object>> searchOrders(String keyword, String status) {
        boolean isNumber = keyword != null && keyword.matches("\\d+");

        String sqlById = """
                    SELECT 
                        o.id,
                        o.user_id,
                        o.order_date,
                        o.total_amount,
                        o.recipient_name,
                        o.shipping_phone,
                        o.shipping_address,
                        o.shipping_provider,
                        o.status,
                        p.payment_method,
                        p.payment_status
                    FROM orders o
                    LEFT JOIN payments p ON o.id = p.order_id
                    WHERE o.status = :status
                      AND o.id = :orderId
                    ORDER BY o.order_date DESC
                """;

        String sqlByName = """
                    SELECT 
                        o.id,
                        o.user_id,
                        o.order_date,
                        o.total_amount,
                        o.recipient_name,
                        o.shipping_phone,
                        o.shipping_address,
                        o.shipping_provider,
                        o.status,
                        p.payment_method,
                        p.payment_status
                    FROM orders o
                    LEFT JOIN payments p ON o.id = p.order_id
                    WHERE o.status = :status
                      AND LOWER(o.recipient_name) LIKE LOWER(:name)
                    ORDER BY o.order_date DESC
                """;

        return jdbi.withHandle(handle -> {
            if (isNumber) {
                return handle.createQuery(sqlById)
                        .bind("status", status)
                        .bind("orderId", Integer.parseInt(keyword))
                        .mapToMap()
                        .list();
            } else {
                return handle.createQuery(sqlByName)
                        .bind("status", status)
                        .bind("name", "%" + keyword + "%")
                        .mapToMap()
                        .list();
            }
        });
    }


    public List<Map<String, Object>> searchOrdersByStatus(String keyword, String status) {
        if (keyword == null) keyword = "";
        if (status == null) status = "";

        String trimmedKeyword = keyword.trim();
        boolean isNumber = trimmedKeyword.matches("\\d+");

        String sqlById = """
                SELECT 
                    o.id, o.user_id, o.order_date, o.total_amount,
                    o.recipient_name, o.shipping_phone, o.shipping_address,
                    o.shipping_provider, o.shipping_fee, o.points_used, o.status,
                    p.payment_method, p.payment_status, p.transaction_id
                FROM orders o
                LEFT JOIN payments p ON o.id = p.order_id
                WHERE o.status = ? AND o.id = ?
                ORDER BY o.order_date DESC
            """;

        String sqlByName = """
                SELECT 
                    o.id, o.user_id, o.order_date, o.total_amount,
                    o.recipient_name, o.shipping_phone, o.shipping_address,
                    o.shipping_provider, o.shipping_fee, o.points_used, o.status,
                    p.payment_method, p.payment_status, p.transaction_id
                FROM orders o
                LEFT JOIN payments p ON o.id = p.order_id
                WHERE o.status = ? 
                  AND LOWER(o.recipient_name) LIKE LOWER(?)
                ORDER BY o.order_date DESC
            """;

        String sqlAll = """
                SELECT 
                    o.id, o.user_id, o.order_date, o.total_amount,
                    o.recipient_name, o.shipping_phone, o.shipping_address,
                    o.shipping_provider, o.shipping_fee, o.points_used, o.status,
                    p.payment_method, p.payment_status, p.transaction_id
                FROM orders o
                LEFT JOIN payments p ON o.id = p.order_id
                WHERE o.status = ?
                ORDER BY o.order_date DESC
            """;

        String finalStatus = status;
        return jdbi.withHandle(handle -> {
            if (isNumber && !trimmedKeyword.isEmpty()) {
                return handle.createQuery(sqlById)
                        .bind(0, finalStatus)
                        .bind(1, Integer.parseInt(trimmedKeyword))
                        .mapToMap()
                        .list();
            } else if (!trimmedKeyword.isEmpty()) {
                return handle.createQuery(sqlByName)
                        .bind(0, finalStatus)
                        .bind(1, "%" + trimmedKeyword + "%")
                        .mapToMap()
                        .list();
            } else {
                return handle.createQuery(sqlAll)
                        .bind(0, finalStatus)
                        .mapToMap()
                        .list();
            }
        });
    }

    public List<Map<String, Object>> searchCancelledOrders(String keyword) {

        if (keyword == null) keyword = "";

        String trimmedKeyword = keyword.trim();
        boolean isNumber = trimmedKeyword.matches("\\d+");


        // SQL tìm kiếm đơn bị hủy theo ID
        String sqlById = """
                    SELECT 
                        o.id, o.user_id, o.order_date, o.total_amount,
                        o.recipient_name, o.shipping_phone, o.shipping_address, o.status,
                        oh.reason as cancellation_reason,
                        oh.changed_by as cancelled_by,
                        oh.changed_at as cancelled_at
                    FROM orders o
                    LEFT JOIN order_history oh ON o.id = oh.order_id 
                        AND oh.status_to = 'Cancelled'
                    WHERE o.status = 'Cancelled' AND o.id = :orderId
                    ORDER BY o.order_date DESC
                """;

        // SQL tìm kiếm đơn bị hủy theo tên
        String sqlByName = """
                    SELECT 
                        o.id, o.user_id, o.order_date, o.total_amount, o.recipient_name,
                        o.shipping_phone, o.shipping_address, o.status,
                        oh.reason as cancellation_reason,
                        oh.changed_by as cancelled_by,
                        oh.changed_at as cancelled_at
                    FROM orders o
                    LEFT JOIN order_history oh ON o.id = oh.order_id 
                        AND oh.status_to = 'Cancelled'
                    WHERE o.status = 'Cancelled'
                      AND LOWER(o.recipient_name) LIKE LOWER(:keyword)
                    ORDER BY o.order_date DESC
                """;

        String sqlAll = """
                    SELECT 
                        o.id, o.user_id, o.order_date, o.total_amount,
                        o.recipient_name, o.shipping_phone, o.shipping_address, o.status,
                        oh.reason as cancellation_reason,
                        oh.changed_by as cancelled_by,
                        oh.changed_at as cancelled_at
                    FROM orders o
                    LEFT JOIN order_history oh ON o.id = oh.order_id 
                        AND oh.status_to = 'Cancelled'
                    WHERE o.status = 'Cancelled'
                    ORDER BY o.order_date DESC
                """;

        List<Map<String, Object>> result = jdbi.withHandle(handle -> {
            if (isNumber && !trimmedKeyword.isEmpty()) {
                return handle.createQuery(sqlById)
                        .bind("orderId", Integer.parseInt(trimmedKeyword))
                        .mapToMap()
                        .list();
            } else if (!trimmedKeyword.isEmpty()) {
                return handle.createQuery(sqlByName)
                        .bind("keyword", "%" + trimmedKeyword + "%")
                        .mapToMap()
                        .list();
            } else {
                return handle.createQuery(sqlAll)
                        .mapToMap()
                        .list();
            }
        });

        return result;
    }


    public boolean cancelOrderWithHistory(int orderId, int userId, String reason) {
        return jdbi.inTransaction(handle -> {
            // Lấy thông tin đơn hàng
            Order order = handle.createQuery("SELECT * FROM orders WHERE id = ?")
                    .bind(0, orderId)
                    .mapToBean(Order.class)
                    .findOne()
                    .orElse(null);

            if (order == null) {
                return false;
            }

            String oldStatus = order.getStatus();

            if (!"Pending".equalsIgnoreCase(oldStatus)) {
                return false;
            }

            // Cập nhật trạng thái đơn hàng
            int updated = handle.createUpdate("UPDATE orders SET status = ?, cancelled_at = NOW() WHERE id = ?")
                    .bind(0, "Cancelled")
                    .bind(1, orderId)
                    .execute();

            if (updated == 0) {
                return false;
            }

            // Lưu vào order_history
            String insertHistorySql = """
            INSERT INTO order_history (order_id, status_from, status_to, changed_by, reason, changed_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

            handle.createUpdate(insertHistorySql)
                    .bind(0, orderId)
                    .bind(1, oldStatus)
                    .bind(2, "Cancelled")
                    .bind(3, userId)
                    .bind(4, reason)
                    .bind(5, LocalDateTime.now())
                    .execute();

            // Hoàn xu nếu đã sử dụng
            if (order.getPointUsed() > 0) {
                handle.createUpdate("UPDATE users SET points = points + ? WHERE id = ?")
                        .bind(0, order.getPointUsed())
                        .bind(1, order.getUserId())
                        .execute();

                String insertTransactionSql = """
                INSERT INTO PointTransactions 
                (user_id, order_id, points, transaction_type, description, created_at) 
                VALUES (?, ?, ?, ?, ?, ?)
            """;

                handle.createUpdate(insertTransactionSql)
                        .bind(0, order.getUserId())
                        .bind(1, orderId)
                        .bind(2, order.getPointUsed())
                        .bind(3, "REFUND")
                        .bind(4, "Hoàn " + order.getPointUsed() + " xu do hủy đơn hàng #" + orderId)
                        .bind(5, LocalDateTime.now())
                        .execute();
            }

            // Hoàn lại tồn kho
            List<OrderItem> orderItems = handle.createQuery(
                            "SELECT * FROM order_items WHERE order_id = ?")
                    .bind(0, orderId)
                    .mapToBean(OrderItem.class)
                    .list();

            for (OrderItem item : orderItems) {
                handle.createUpdate(
                                "UPDATE comics SET stock_quantity = stock_quantity + ? WHERE id = ?")
                        .bind(0, item.getQuantity())
                        .bind(1, item.getComicId())
                        .execute();

                handle.createUpdate("INSERT INTO inventory_transaction (comic_id, type, quantity, note, created_at) VALUES (?, 'IMPORT', ?, ?, NOW())")
                        .bind(0, item.getComicId())
                        .bind(1, item.getQuantity())
                        .bind(2, "Hoàn kho từ đơn hàng bị hủy #" + orderId)
                        .execute();
            }

            return true;
        });
    }

    /**
     * Tìm kiếm đơn hàng đã giao với thông tin đánh giá
     */
    public List<Map<String, Object>> searchCompletedOrdersWithRating(String keyword) {

        if (keyword == null) keyword = "";

        String trimmedKeyword = keyword.trim();
        boolean isNumber = trimmedKeyword.matches("\\d+");

        String baseSelect = """
        SELECT
            o.id,
            o.user_id,
            o.order_date,
            o.completed_at,
            o.delivered_at,
            o.total_amount,
            o.recipient_name,
            o.shipping_phone,
            o.shipping_address,
            o.shipping_provider,
            o.shipping_fee,
            o.points_used,
            o.status,

            p.payment_method,
            p.payment_status,
            p.transaction_id,

            AVG(r.rating) AS average_rating,
            COUNT(r.id) AS review_count

        FROM orders o
        LEFT JOIN payments p
            ON o.id = p.order_id
        LEFT JOIN reviews r
            ON o.id = r.order_id
        """;

        String groupOrder = """
        GROUP BY
            o.id,
            o.user_id,
            o.order_date,
            o.completed_at,
            o.delivered_at,
            o.total_amount,
            o.recipient_name,
            o.shipping_phone,
            o.shipping_address,
            o.shipping_provider,
            o.shipping_fee,
            o.points_used,
            o.status,
            p.payment_method,
            p.payment_status,
            p.transaction_id

        ORDER BY COALESCE(o.completed_at, o.order_date) DESC
        """;

        String sqlById =
                baseSelect +
                        """
                        WHERE o.status = 'Completed'
                          AND o.id = ?
                        """
                        + groupOrder;

        String sqlByName =
                baseSelect +
                        """
                        WHERE o.status = 'Completed'
                          AND LOWER(o.recipient_name) LIKE LOWER(?)
                        """
                        + groupOrder;

        String sqlAll =
                baseSelect +
                        """
                        WHERE o.status = 'Completed'
                        """
                        + groupOrder;

        return jdbi.withHandle(handle -> {

            if (isNumber && !trimmedKeyword.isEmpty()) {

                return handle.createQuery(sqlById)
                        .bind(0, Integer.parseInt(trimmedKeyword))
                        .mapToMap()
                        .list();

            } else if (!trimmedKeyword.isEmpty()) {

                return handle.createQuery(sqlByName)
                        .bind(0, "%" + trimmedKeyword + "%")
                        .mapToMap()
                        .list();

            } else {

                return handle.createQuery(sqlAll)
                        .mapToMap()
                        .list();
            }
        });
    }


}