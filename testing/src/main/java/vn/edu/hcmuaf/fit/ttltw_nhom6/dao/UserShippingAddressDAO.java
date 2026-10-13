package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.UserShippingAddress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class UserShippingAddressDAO {
    private final Jdbi jdbi;

    public UserShippingAddressDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public UserShippingAddressDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Tạo địa chỉ giao hàng mới
     */
    public int createShippingAddress(UserShippingAddress address) {
        return jdbi.withHandle(handle -> {
            String sql = "INSERT INTO user_shipping_addresses " +
                    "(user_id, recipient_name, phone, province, district, ward, " +
                    "street_address, is_default, is_deleted, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            return handle.createUpdate(sql)
                    .bind(0, address.getUserId())
                    .bind(1, address.getRecipientName())
                    .bind(2, address.getPhone())
                    .bind(3, address.getProvince())
                    .bind(4, address.getDistrict())
                    .bind(5, address.getWard())
                    .bind(6, address.getStreetAddress())
                    .bind(7, address.isDefault())
                    .bind(8, false) // is_deleted = false
                    .bind(9, LocalDate.now()) // created_at
                    .bind(10, LocalDate.now()) // updated_at
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Integer.class)
                    .one();
        });
    }


    /**
     * Lấy tất cả địa chỉ của user
     */
    public List<UserShippingAddress> getAddressesByUserId(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM user_shipping_addresses " +
                                "WHERE user_id = ? AND is_deleted = false " +
                                "ORDER BY is_default DESC, created_at DESC")
                        .bind(0, userId)
                        .mapToBean(UserShippingAddress.class)
                        .list()
        );
    }

    /**
     * Lấy địa chỉ mặc định của user
     */
    public Optional<UserShippingAddress> getDefaultAddress(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM user_shipping_addresses " +
                                "WHERE user_id = ? AND is_default = true AND is_deleted = false")
                        .bind(0, userId)
                        .mapToBean(UserShippingAddress.class)
                        .findFirst()
        );
    }

    /**
     * Đặt địa chỉ làm mặc định (và bỏ mặc định các địa chỉ khác)
     */
    public boolean setDefaultAddress(int addressId, int userId) {
        return jdbi.inTransaction(handle -> {
            handle.createUpdate("UPDATE user_shipping_addresses SET is_default = false " +
                            "WHERE user_id = ? AND is_deleted = false")
                    .bind(0, userId)
                    .execute();

            int updated = handle.createUpdate("UPDATE user_shipping_addresses " +
                            "SET is_default = true, updated_at = ? " +
                            "WHERE id = ? AND user_id = ? AND is_deleted = false")
                    .bind(0, LocalDate.now())
                    .bind(1, addressId)
                    .bind(2, userId)
                    .execute();

            return updated > 0;
        });
    }

    public void unsetAllDefaultAddresses(int userId) {
        jdbi.useHandle(handle -> {
            handle.createUpdate("UPDATE user_shipping_addresses SET is_default = false " +
                            "WHERE user_id = ? AND is_deleted = false")
                    .bind(0, userId)
                    .execute();
        });
    }

    /**
     * Kiểm tra địa chỉ đã tồn tại chưa (dựa trên thông tin giống nhau)
     */
    public Optional<UserShippingAddress> findExistingAddress(UserShippingAddress address) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM user_shipping_addresses " +
                                "WHERE user_id = ? AND recipient_name = ? AND phone = ? " +
                                "AND province = ? AND district = ? AND ward = ? " +
                                "AND street_address = ? AND is_deleted = false")
                        .bind(0, address.getUserId())
                        .bind(1, address.getRecipientName())
                        .bind(2, address.getPhone())
                        .bind(3, address.getProvince())
                        .bind(4, address.getDistrict())
                        .bind(5, address.getWard())
                        .bind(6, address.getStreetAddress())
                        .mapToBean(UserShippingAddress.class)
                        .findFirst()
        );
    }

    /**
     * Đếm số địa chỉ của user
     */
    public int countAddressesByUserId(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM user_shipping_addresses " +
                                "WHERE user_id = ? AND is_deleted = false")
                        .bind(0, userId)
                        .mapTo(Integer.class)
                        .one()
        );
    }
}
