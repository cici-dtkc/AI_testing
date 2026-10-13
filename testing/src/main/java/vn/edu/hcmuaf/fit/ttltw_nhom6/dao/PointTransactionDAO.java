package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.PointTransaction;

import java.time.LocalDateTime;
import java.util.List;

public class PointTransactionDAO {
    private final Jdbi jdbi;

    public PointTransactionDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Tạo giao dịch điểm mới
     */
    public int createTransaction(PointTransaction transaction) {
        return jdbi.withHandle(handle -> {
            String sql = "INSERT INTO pointtransactions " +
                    "(user_id, order_id, points, transaction_type, description, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            return handle.createUpdate(sql)
                    .bind(0, transaction.getUserId())
                    .bind(1, transaction.getOrderId())
                    .bind(2, transaction.getPoints())
                    .bind(3, transaction.getTransactionType())
                    .bind(4, transaction.getDescription())
                    .bind(5, transaction.getCreatedAt() != null ?
                            transaction.getCreatedAt() : LocalDateTime.now())
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Integer.class)
                    .one();
        });
    }

    /**
     * Lấy lịch sử giao dịch điểm của user
     */
    public List<PointTransaction> getTransactionsByUserId(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM pointtransactions " +
                                "WHERE user_id = ? ORDER BY created_at DESC")
                        .bind(0, userId)
                        .mapToBean(PointTransaction.class)
                        .list()
        );
    }
}