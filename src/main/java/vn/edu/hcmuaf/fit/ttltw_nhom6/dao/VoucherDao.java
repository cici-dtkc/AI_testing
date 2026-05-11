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
}
