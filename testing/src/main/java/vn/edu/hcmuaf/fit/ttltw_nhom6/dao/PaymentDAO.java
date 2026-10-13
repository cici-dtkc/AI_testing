package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Payment;

import java.time.LocalDateTime;
import java.util.Optional;

public class PaymentDAO {
    private final Jdbi jdbi;

    public PaymentDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public PaymentDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<Payment> getPaymentByOrderId(int orderId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM payments WHERE order_id = ?")
                        .bind(0, orderId)
                        .mapToBean(Payment.class)
                        .findFirst()
        );
    }


    public boolean updatePaymentStatus(int paymentId, String status, String transactionId) {
        return jdbi.withHandle(handle -> {
            String sql = "UPDATE payments SET payment_status = ?, transaction_id = ?, " +
                    "payment_date = ?, updated_at = ? WHERE id = ?";

            int updated = handle.createUpdate(sql)
                    .bind(0, status)
                    .bind(1, transactionId)
                    .bind(2, LocalDateTime.now())
                    .bind(3, LocalDateTime.now())
                    .bind(4, paymentId)
                    .execute();

            return updated > 0;
        });
    }
}
