package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoucherDao {
    private final Jdbi jdbi;


    public VoucherDao() {
        this.jdbi = JdbiConnector.get();
    }

    public boolean addVoucher(Voucher voucher) {
        String sql = "insert into vouchers (code, discount_type, discount_value, discount_target, " +
                " apply_scope, quantity, start_date, end_date, min_order_amount, is_single_use, used_count)" +
                " values ( :code, :discount_type, :discount_value, :discount_target, :apply_scope, :quantity, " +
                " :start_date, :end_date, :min_order_amount, :is_single_use, :used_count)";


        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("code", voucher.getCode())
                        .bind("discount_type", voucher.getDiscountType())
                        .bind("discount_value", voucher.getDiscountValue())
                        .bind("discount_target", voucher.getDiscountTarget())
                        .bind("apply_scope", voucher.getApplyScope())
                        .bind("quantity", voucher.getQuantity())
                        .bind("start_date", voucher.getStartDate())
                        .bind("end_date", voucher.getEndDate())
                        .bind("min_order_amount", voucher.getMinOrderAmount())
                        .bind("is_single_use", voucher.isSingleUse())
                        .bind("used_count", voucher.getUsedCount())
                        .execute() > 0
        );


    }

    public List<Voucher> getAllVouchers() {
        String sql = "select * from vouchers order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public boolean isExistCode(String code) {
        String sql = "select count(*) from vouchers where code = :code";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("code", code)
                        .mapTo(Integer.class)
                        .one() > 0
        );

    }

    public boolean deleteByCode(String code) {
        String sql = "DELETE FROM vouchers WHERE code = :code";
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("code", code)
                        .execute() > 0
        );
    }

    public List<String> getCategories() {
        String sql = "Select name_categories from categories";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapTo(String.class)
                        .list()
        );
    }

    public boolean updateVoucher(String code, BigDecimal minOrderAmount, int quantity,
                                 LocalDateTime startDate, LocalDateTime endDate, boolean isSingleUse) {

        return jdbi.withHandle(handle ->
                handle.createUpdate("""
                                    UPDATE vouchers
                                    SET 
                                        min_order_amount = :minOrderAmount,
                                        quantity = :quantity,
                                        start_date = :startDate,
                                        end_date = :endDate,
                                        is_single_use = :isSingleUse
                                    WHERE code = :code
                                """)
                        .bind("minOrderAmount", minOrderAmount)
                        .bind("quantity", quantity)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .bind("isSingleUse", isSingleUse)
                        .bind("code", code)
                        .execute() > 0
        );
    }

    public List<Voucher> getVouchersExpiringTomorrow() {
        String sql = """
                SELECT * FROM vouchers
                WHERE DATE(end_date) = DATE(DATE_ADD(NOW(), INTERVAL 1 DAY))
                  AND quantity > used_count
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Voucher> getOutVouchers() {
        String sql = "SELECT * from vouchers Where quantity = used_count AND end_date > NOW() order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }

    public List<Voucher> getRunningVouchers() {
        String sql = "SELECT * from vouchers Where start_date < NOw() AND end_date > NOW() AND  quantity -used_count != 0  order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }

    public List<Voucher> getExpredVouchers() {
        String sql = "SELECT * from vouchers Where end_date <= NOW() order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }

    public List<Voucher> searchVoucherById(String id) {
        String sql = "SELECT * FROM vouchers WHERE CAST(code AS CHAR) LIKE :id";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", "%" + id + "%")
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Voucher> getAllActiveVouchers() {
        String sql = "SELECT * FROM vouchers WHERE start_date <= NOW() AND end_date > NOW() AND quantity > used_count ORDER BY discount_target, end_date";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Voucher> getProductVouchersByUser(int userId) {
        String sql = """
        SELECT v.* FROM vouchers v
        JOIN user_vouchers uv ON v.id = uv.promo_id
        WHERE uv.user_id = :userId
          AND v.discount_target != 'Vận chuyển'
          AND v.end_date >= NOW()
          AND v.quantity > v.used_count
          AND (v.is_single_use = 0 OR (v.is_single_use = 1 AND uv.is_used = 0))
        ORDER BY v.end_date ASC
    """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Voucher> getShippingVouchersByUser(int userId) {
        String sql = """
        SELECT v.* FROM vouchers v
        JOIN user_vouchers uv ON v.id = uv.promo_id
        WHERE uv.user_id = :userId
          AND v.discount_target = 'Vận chuyển'
          AND v.end_date >= NOW()
          AND v.quantity > v.used_count
          AND (v.is_single_use = 0 OR (v.is_single_use = 1 AND uv.is_used = 0))
        ORDER BY v.end_date ASC
    """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public boolean saveVoucherForUser(int userId, int promoId, String code) {
        String checkSql = "SELECT COUNT(*) FROM user_vouchers WHERE user_id = :userId AND promo_id = :promoId";
        int count = jdbi.withHandle(handle ->
                handle.createQuery(checkSql)
                        .bind("userId", userId)
                        .bind("promoId", promoId)
                        .mapTo(Integer.class)
                        .one()
        );
        if (count > 0) return false;

        String sql = """
                    INSERT INTO user_vouchers (user_id, promo_id, code, acquired_at, is_active, is_used)
                    VALUES (:userId, :promoId, :code, NOW(), 1, 0)
                """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("userId", userId)
                        .bind("promoId", promoId)
                        .bind("code", code)
                        .execute() > 0
        );
    }

    public List<Voucher> getVouchersByUserId(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                                    SELECT v.* FROM vouchers v
                                    JOIN user_vouchers uv ON v.id = uv.promo_id
                                    WHERE uv.user_id = :userId AND v.end_date >= NOW()
                                    AND v.quantity > v.used_count 
                                    AND (v.is_single_use = 0 OR (v.is_single_use = 1 AND uv.is_used = 0))
                                """)
                        .bind("userId", userId)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public boolean hasUserUsedVoucher(int userId, int voucherId) {
        String sql = """
                    SELECT COUNT(*) FROM user_vouchers
                    WHERE user_id = :userId AND promo_id = :voucherId AND is_used = 1
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .bind("voucherId", voucherId)
                        .mapTo(Integer.class)
                        .one() > 0
        );
    }

    public Voucher getVoucherByCodeAndUser(String code, int userId) {
        String sql = """
                    SELECT v.* FROM vouchers v
                    JOIN user_vouchers uv ON v.id = uv.promo_id
                    WHERE v.code = :code AND uv.user_id = :userId AND v.quantity > v.used_count
                    AND (v.is_single_use = 0 OR (v.is_single_use = 1 AND uv.is_used = 0))
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("code", code)
                        .bind("userId", userId)
                        .mapToBean(Voucher.class)
                        .findOne()
                        .orElse(null)
        );
    }

    public void markVoucherAsUsed(int userId, int voucherId, int orderId) {
        String sql1 = "UPDATE user_vouchers SET is_used = 1, used_at = NOW(), used_order_id = :orderId " +
                "WHERE user_id = :userId AND promo_id = :voucherId";
        jdbi.useHandle(handle ->
                handle.createUpdate(sql1)
                        .bind("orderId", orderId)
                        .bind("userId", userId)
                        .bind("voucherId", voucherId)
                        .execute()
        );

        String sql2 = "UPDATE vouchers SET used_count = used_count + 1 WHERE id = :voucherId";
        jdbi.useHandle(handle ->
                handle.createUpdate(sql2)
                        .bind("voucherId", voucherId)
                        .execute()
        );
    }

    public boolean isVoucherApplicableForItems(String voucherCode, List<Integer> comicIds) {
        String scopeSql = "SELECT apply_scope FROM vouchers WHERE code = :code";
        String scope = jdbi.withHandle(handle ->
                handle.createQuery(scopeSql)
                        .bind("code", voucherCode)
                        .mapTo(String.class)
                        .findOne()
                        .orElse(null)
        );

        if (scope == null) return false;
        if ("Tất cả".equalsIgnoreCase(scope.trim())) return true;

        String checkSql = """
        SELECT COUNT(*) FROM comics c
        JOIN categories cat ON c.category_id = cat.id
        WHERE c.id IN (<comicIds>)
          AND cat.name_categories = :scope
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(checkSql.replace("<comicIds>",
                                comicIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))))
                        .bind("scope", scope.trim())
                        .mapTo(Integer.class)
                        .one() > 0
        );
    }

    public List<Voucher> getVouchersExpiringInOneHour() {
        String sql = """
        SELECT * FROM vouchers
        WHERE end_date BETWEEN DATE_ADD(NOW(), INTERVAL 55 MINUTE) 
                           AND DATE_ADD(NOW(), INTERVAL 65 MINUTE)
          AND quantity > used_count
        """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Voucher> get5TheMostUsedVouchers() {
        String sql = "select * from vouchers order by used_count DESC LIMIT 5";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list()
        );
    }

    public List<Map<String, Object>> getAllVouchersAsMap(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, code, discount_value, quantity, used_count, end_date " +
                "FROM vouchers WHERE start_date >= :startDate AND end_date <= :endDate " +
                "ORDER BY used_count DESC";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .map((rs, ctx) -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("code", rs.getString("code"));
                            m.put("discountValue", rs.getBigDecimal("discount_value"));
                            m.put("quantity", rs.getInt("quantity"));
                            m.put("usedCount", rs.getInt("used_count"));
                            java.sql.Timestamp endDate2 = rs.getTimestamp("end_date");
                            m.put("expired", endDate2 != null && endDate2.toLocalDateTime().isBefore(java.time.LocalDateTime.now()));
                            m.put("savedRevenue", 0);
                            return m;
                        })
                        .list()
        );
    }

    public List<Map<String, Object>> get5TheMostUsedVouchersAsMap(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT code, quantity, used_count " +
                "FROM vouchers WHERE start_date >= :startDate AND end_date <= :endDate " +
                "ORDER BY used_count DESC LIMIT 5";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .map((rs, ctx) -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("code", rs.getString("code"));
                            m.put("quantity", rs.getInt("quantity"));
                            m.put("usedCount", rs.getInt("used_count"));
                            return m;
                        })
                        .list()
        );
    }

}
