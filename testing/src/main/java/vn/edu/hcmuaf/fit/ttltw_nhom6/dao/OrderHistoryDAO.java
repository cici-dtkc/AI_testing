package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.OrderHistory;

import java.util.Optional;

public class OrderHistoryDAO {
    private final Jdbi jdbi;

    public OrderHistoryDAO() {
        this.jdbi = JdbiConnector.get();
    }

    /**
     * Thêm một bản ghi lịch sử mới
     */
    public boolean addHistory(OrderHistory history) {
        String sql = """
            INSERT INTO order_history (order_id, status_from, status_to, changed_by, reason)
            VALUES (:orderId, :statusFrom, :statusTo, :changedBy, :reason)
        """;

        try {

            int result = jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("orderId", history.getOrderId())
                            .bind("statusFrom", history.getStatusFrom())
                            .bind("statusTo", history.getStatusTo())
                            .bind("changedBy", history.getChangedBy())
                            .bind("reason", history.getReason())
                            .execute()
            );

            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy lý do hủy đơn hàng
     */
    public Optional<OrderHistory> getCancellationReason(int orderId) {
        String sql = """
            SELECT * FROM order_history
            WHERE order_id = ?
              AND status_to = 'Cancelled'
            ORDER BY changed_at DESC
            LIMIT 1
        """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind(0, orderId)
                            .mapToBean(OrderHistory.class)
                            .findFirst()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static int countUserCancelledOrdersLastHour(int userId) {
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(
                                """
                                SELECT COUNT(*)
                                FROM order_history oh
                                JOIN orders o ON oh.order_id = o.id
                                WHERE o.user_id = :userId
                                  AND oh.status_to = 'Cancelled'
                                  AND oh.changed_by = :userId
                                  AND oh.changed_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
                                """
                        )
                        .bind("userId", userId)
                        .mapTo(Integer.class)
                        .one()
        );
    }



}
