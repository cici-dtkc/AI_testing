package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoucherDao {
    private final Jdbi jdbi;


    public VoucherDao() {
        this.jdbi = JdbiConnector.get();
    }

    public boolean addVoucher(Voucher voucher) {
        String sql ="insert into vouchers (code, discount_type, discount_value, discount_target, " +
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
                        .execute() >0
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

    public List<String> getCategories(){
        String sql = "Select name_categories from categories";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                   .mapTo(String.class)
                    .list()
                );
    }
    public boolean updateVoucher(String code, BigDecimal minOrderAmount, int quantity,
                              LocalDateTime startDate,  LocalDateTime endDate, boolean isSingleUse) {

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
                        .execute() >0
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

    public List<Voucher> getOutVouchers(){
        String sql = "SELECT * from vouchers Where quantity = used_count AND end_date > NOW() order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }
    public List<Voucher> getRunningVouchers(){
        String sql = "SELECT * from vouchers Where start_date < NOw() AND end_date > NOW() AND  quantity -used_count != 0  order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }
    public List<Voucher> getExpredVouchers(){
        String sql = "SELECT * from vouchers Where end_date <= NOW() order by id desc";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Voucher.class)
                        .list());
    }

    public List<Voucher> searchVoucherById(String id){
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
        WHERE uv.user_id = :userId AND uv.is_active = 1 AND uv.is_used = 0
        AND v.discount_target != 'Vận chuyển' AND v.end_date >= NOW()
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
        WHERE uv.user_id = :userId AND uv.is_active = 1 AND uv.is_used = 0
        AND v.discount_target = 'Vận chuyển' AND v.end_date >= NOW()
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
}
