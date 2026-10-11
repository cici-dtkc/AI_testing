package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.time.LocalDateTime;
import java.util.*;

public class UserDao {
    private static UserDao instance;
    private final Jdbi jdbi;

    public UserDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao(JdbiConnector.get());
        }
        return instance;
    }

    // Tìm user theo username hoặc email
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE (username = :ue OR email = :ue OR phone = :ue) AND is_deleted = 0")
                        .bind("ue", usernameOrEmail)
                        .mapToBean(User.class)
                        .findOne()
        );
    }

    public Optional<User> findByPhone(String phone) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE phone = :phone")
                        .bind("phone", phone)
                        .mapToBean(User.class)
                        .findOne()
        );
    }

    // Thêm user mới
    public void insert(User user) {
        jdbi.useHandle(handle ->
                handle.createUpdate("INSERT INTO users (username, email,phone, password_hash, full_name, role, role_id, created_at) " +
                                "VALUES (:username, :email,:phone, :passwordHash, :fullName, 'user', 6, NOW())")
                        .bindBean(user)
                        .execute()
        );
    }

    public void insertInactive(User user) {
        jdbi.useHandle(handle ->
                handle.createUpdate("INSERT INTO users (username, email, phone, password_hash, full_name, role, role_id, created_at, is_active) " +
                                "VALUES (:username, :email, :phone, :passwordHash, :fullName, 'user', 6, NOW(), 0)")
                        .bindBean(user)
                        .execute()
        );
    }

    public boolean activateUserByEmail(String email) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET is_active = 1, updated_at = NOW() WHERE email = :email")
                        .bind("email", email).execute() > 0);
    }

    public void deleteInactiveUser(String username, String email) {
        jdbi.useHandle(handle ->
                handle.createUpdate("DELETE FROM users WHERE (username = :username OR email = :email) AND is_active = 0")
                        .bind("username", username)
                        .bind("email", email)
                        .execute()
        );
    }
    //   2 cái này cho đăng kí nó trùng username hay mail nó khỏi error 500
    public Optional<User> findByUsername(String username) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE username = :username")
                        .bind("username", username)
                        .mapToBean(User.class)
                        .findOne()
        );
    }

    public Optional<User> findByEmail(String email) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE email = :email")
                        .bind("email", email)
                        .mapToBean(User.class)
                        .findOne()
        );
    }

    // Update mật khẩu theo email của authen
    public void updatePassword(String email, String newPasswordHash) {
        jdbi.useHandle(handle ->
                handle.createUpdate("UPDATE users SET password_hash = :passwordHash WHERE email = :email")
                        .bind("passwordHash", newPasswordHash)
                        .bind("email", email)
                        .execute()
        );
    }


    //cái này để chỉnh sủa thoogn tin user ở profile

    // Cập nhật thông tin user
    public boolean updateUser(User user) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET full_name = :fullName, phone = :phone, email = :email, gender = :gender, birthdate = :birthdate, address = :address, updated_at = :updatedAt WHERE id = :id")
                        .bind("fullName", user.getFullName())
                        .bind("phone", user.getPhone())
                        .bind("email", user.getEmail())
                        .bind("gender", user.getGender())
                        .bind("birthdate", user.getBirthdate())
                        .bind("address", user.getAddress())
                        .bind("updatedAt", LocalDateTime.now())
                        .bind("id", user.getId())
                        .execute() > 0
        );
    }

    // Lấy user theo ID
    public User getUserById(int id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE id = :id")
                        .bind("id", id)
                        .mapToBean(User.class)
                        .findOne()
                        .orElse(null)
        );
    }

    /**
     * Cap nhat diem thuong cua user
     */
    public boolean updateUserPoints(int userId, int newPoints) {
        return jdbi.withHandle(handle -> {
            int updated = handle.createUpdate(
                            "UPDATE users SET points = ? WHERE id = ?")
                    .bind(0, newPoints)
                    .bind(1, userId)
                    .execute();
            return updated > 0;
        });
    }


    public boolean updatePassword(int userId, String newPasswordHash) {
        return JdbiConnector.get().withHandle(handle ->
                handle.createUpdate("UPDATE users SET password_hash = :hash, updated_at = NOW() WHERE id = :id")
                        .bind("hash", newPasswordHash)
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    public Optional<User> findById(int id) {
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE id = :id AND is_deleted = 0")
                        .bind("id", id)
                        .mapToBean(User.class)
                        .findOne()
        );
    }

    /**
     * Lấy tất cả users (chỉ customer, không lấy admin)
     */
    public List<User> getAllCustomers() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM users WHERE role = 'user' " +
                                "AND is_deleted = 0 ORDER BY created_at DESC")
                        .mapToBean(User.class)
                        .list()
        );
    }

    /**
     * Tìm kiếm user theo tên hoặc email
     */
    public List<User> searchCustomers(String keyword) {
        return jdbi.withHandle(handle -> {
            String membershipLevel = convertVietnameseToLevel(keyword);

            StringBuilder sql = new StringBuilder(
                    "SELECT * FROM users WHERE role = 'user' AND is_deleted = 0 AND ("
            );

            sql.append("full_name LIKE :keyword OR email LIKE :keyword OR username LIKE :keyword");

            if (membershipLevel != null) {
                sql.append(" OR membership_level = :level");
            }

            sql.append(") ORDER BY created_at DESC");

            var query = handle.createQuery(sql.toString())
                    .bind("keyword", "%" + keyword + "%");

            if (membershipLevel != null) {
                query.bind("level", membershipLevel);
            }

            List<User> result = query.mapToBean(User.class).list();

            return result;
        });
    }

    private String convertVietnameseToLevel(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String lowerKeyword = keyword.toLowerCase().trim();

        if (lowerKeyword.contains("thường") || lowerKeyword.contains("thuong") ||
                lowerKeyword.equals("normal")) {
            return "Normal";
        } else if (lowerKeyword.contains("bạc") || lowerKeyword.contains("bac") ||
                lowerKeyword.equals("silver")) {
            return "Silver";
        } else if (lowerKeyword.contains("vàng") || lowerKeyword.contains("vang") ||
                lowerKeyword.equals("gold")) {
            return "Gold";
        } else if (lowerKeyword.contains("bạch kim") || lowerKeyword.contains("bach kim") ||
                lowerKeyword.equals("platinum") || lowerKeyword.contains("kim cương") ||
                lowerKeyword.contains("kim cuong")) {
            return "Platinum";
        }

        return null;
    }

    /**
     * Lọc user theo cấp độ thành viên
     */
    public List<User> filterCustomersByMembershipLevel(String membershipLevel) {
        return jdbi.withHandle(handle -> {
            List<User> users = handle.createQuery("SELECT * FROM users WHERE role = 'user' AND is_deleted = 0 " +
                            "AND membership_level = :level ORDER BY created_at DESC")
                    .bind("level", membershipLevel)
                    .mapToBean(User.class)
                    .list();

            return users;
        });
    }

    /**
     * Tìm kiếm và lọc kết hợp
     */
    public List<User> searchAndFilterCustomers(String keyword, String membershipLevel) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM users WHERE role = 'user' AND is_deleted = 0 "
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (full_name LIKE :keyword OR email LIKE :keyword OR username LIKE :keyword) ");
        }

        if (membershipLevel != null && !membershipLevel.trim().isEmpty()) {
            sql.append("AND membership_level = :level ");
        }

        sql.append("ORDER BY created_at DESC");

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString());

            if (keyword != null && !keyword.trim().isEmpty()) {
                query.bind("keyword", "%" + keyword + "%");
            }

            if (membershipLevel != null && !membershipLevel.trim().isEmpty()) {
                query.bind("level", membershipLevel);
            }

            return query.mapToBean(User.class).list();
        });
    }

    /**
     * Nâng cấp membership level của user
     */
    public boolean upgradeMembershipLevel(int userId, String newLevel) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET membership_level = :level, updated_at = NOW() WHERE id = :id")
                        .bind("level", newLevel)
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    /**
     * Khóa tài khoản user (soft delete)
     */
    public boolean lockUserAccount(int userId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET is_active = 0, " +
                                "deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    /**
     * Reset số lần đăng nhập thất bại về 0
     */
    public boolean resetFailedLoginAttempts(int userId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate(
                                "UPDATE users SET failed_login_attempts = 0, " +
                                        "updated_at = NOW() WHERE id = :id"
                        )
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    /**
     * Tăng số lần đăng nhập thất bại
     */
    public boolean incrementFailedLoginAttempts(int userId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate(
                                "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
                                        "updated_at = NOW() WHERE id = :id"
                        )
                        .bind("id", userId)
                        .execute() > 0
        );
    }


    /**
     * Đồng bộ total_spent từ orders đã completed
     */
    public boolean syncTotalSpentFromOrders(int userId) {
        return jdbi.withHandle(handle -> {
            int updated = handle.createUpdate(
                            "UPDATE users u " +
                                    "SET u.total_spent = (" +
                                    "    SELECT COALESCE(SUM(o.total_amount), 0) " +
                                    "    FROM orders o " +
                                    "    WHERE o.user_id = u.id AND o.status = 'Completed'" +
                                    "), " +
                                    "u.updated_at = NOW() " +
                                    "WHERE u.id = :userId"
                    )
                    .bind("userId", userId)
                    .execute();
            return updated > 0;
        });
    }

    /**
     * Đồng bộ total_spent cho TẤT CẢ users
     */
    public int syncAllUsersTotalSpent() {
        return jdbi.withHandle(handle ->
                handle.createUpdate(
                                "UPDATE users u " +
                                        "SET u.total_spent = (" +
                                        "    SELECT COALESCE(SUM(o.total_amount), 0) " +
                                        "    FROM orders o " +
                                        "    WHERE o.user_id = u.id AND o.status = 'Completed'" +
                                        "), " +
                                        "u.updated_at = NOW() " +
                                        "WHERE u.role = 'user'"
                        )
                        .execute()
        );
    }

    /**
     * Kiểm tra xem user có đủ điều kiện để nâng cấp lên level mới không
     */
    public Map<String, Object> checkUpgradeEligibility(int userId, String newLevel) {
        return jdbi.withHandle(handle -> {
            Map<String, Object> result = new HashMap<>();

            // Lấy thông tin user
            User user = handle.createQuery("SELECT * FROM users WHERE id = :id")
                    .bind("id", userId)
                    .mapToBean(User.class)
                    .findOne()
                    .orElse(null);

            if (user == null) {
                result.put("eligible", false);
                result.put("message", "Không tìm thấy user");
                return result;
            }

            double currentSpent = user.getTotalSpent() != null
                    ? user.getTotalSpent().doubleValue()
                    : 0.0;
            String currentLevel = user.getMembershipLevel() != null ? user.getMembershipLevel() : "Normal";

            // Xác định mức chi tiêu tối thiểu cho từng cấp
            double requiredSpent = getRequiredSpentForLevel(newLevel);

            result.put("currentLevel", currentLevel);
            result.put("currentSpent", currentSpent);
            result.put("requiredSpent", requiredSpent);

            // Kiểm tra điều kiện
            if (currentSpent >= requiredSpent) {
                result.put("eligible", true);
                result.put("message", "Đủ điều kiện nâng cấp");
            } else {
                result.put("eligible", false);
                double shortage = requiredSpent - currentSpent;
                result.put("shortage", shortage);
                result.put("message", String.format(
                        "Chưa đủ điều kiện! Cần chi tiêu thêm %s để đạt cấp %s (Hiện tại: %s / Yêu cầu: %s)",
                        formatCurrency(shortage),
                        getLevelName(newLevel),
                        formatCurrency(currentSpent),
                        formatCurrency(requiredSpent)
                ));
            }

            return result;
        });
    }

    /**
     * Lấy mức chi tiêu tối thiểu cho từng cấp
     */
    private double getRequiredSpentForLevel(String level) {
        switch (level) {
            case "Normal":
                return 0;
            case "Silver":
                return 500_000;
            case "Gold":
                return 1_000_000;
            case "Platinum":
                return 2_000_000;
            default:
                return 0;
        }
    }

    /**
     * Lấy tên tiếng Việt của cấp độ
     */
    private String getLevelName(String level) {
        switch (level) {
            case "Normal":
                return "Thường";
            case "Silver":
                return "Bạc";
            case "Gold":
                return "Vàng";
            case "Platinum":
                return "Kim cương";
            default:
                return level;
        }
    }

    /**
     * Format tiền Việt
     */
    private String formatCurrency(double amount) {
        return String.format("%,.0fđ", amount).replace(",", ".");
    }

    /**
     * Kiểm tra user có vi phạm quy định không
     */
    public Map<String, Object> checkUserViolation(int userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> violations = new ArrayList<>();

        // 1. Kiểm tra hủy đơn nhiều lần
        int cancelCount = OrderHistoryDAO.countUserCancelledOrdersLastHour(userId);
        if (cancelCount >= 5) {
            violations.add(String.format("Hủy %d đơn hàng trong 1 giờ", cancelCount));
        }

        // 2. Kiểm tra đăng nhập thất bại
        User userOpt = getUserById(userId);
        if (userOpt != null) {
            System.out.println("Failed attempts: " + userOpt.getFailedLoginAttempts());
            int failCount = userOpt.getFailedLoginAttempts();
            if (failCount >= 5) {
                violations.add(String.format("Đăng nhập thất bại %d lần liên tiếp", failCount));
            }
        }

        result.put("hasViolation", !violations.isEmpty());
        result.put("violations", violations);
        result.put("violationCount", violations.size());

        return result;
    }

    public boolean isUserActive(Optional<User> userOpt) {
        return userOpt
                .map(User::getIsActive) // hoặc User::isActive
                .orElse(false);
    }


    public int userPoint(int id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("select points from users where id = :ue")
                        .bind("ue", id)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public boolean updateAvatar(int userId, String avatarUrl) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET avatar_url = :avatar, updated_at = NOW() WHERE id = :id")
                        .bind("avatar", avatarUrl)
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    public boolean unlockUserAccount(int userId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET is_active = 1, locked_until = NULL, " +
                                "deleted_at = NULL, failed_login_attempts = 0, updated_at = NOW() WHERE id = :id")
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    public boolean lockUserTemporary(int userId, int minutes) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE users SET locked_until = DATE_ADD(NOW(), INTERVAL :minutes MINUTE), " +
                                "updated_at = NOW() WHERE id = :id")
                        .bind("minutes", minutes)
                        .bind("id", userId)
                        .execute() > 0
        );
    }

    public boolean insertStatusLoign(int idUser, String ipAdress, String userAgent ){
        String sql ="insert login_sessions(user_id, ip_address, user_agent, login_time, active) " +
                " values(:idUser, :ipAdress, :userAgent, NOW(), 1)";

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("idUser", idUser)
                        .bind("ipAdress", ipAdress)
                        .bind("userAgent", userAgent)
                        .execute()>0
        );
    }

    public boolean updateRole(int userId, int roleId) {
        String sql = """
            UPDATE users
            SET role_id = :roleId
            WHERE id = :userId
            """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("roleId", roleId)
                        .bind("userId", userId)
                        .execute() > 0
        );
    }

    public List<User> getAllUsers() {
        String sql = """
                SELECT u.*, r.role_name
                FROM users u
                LEFT JOIN roles r ON u.role_id = r.id
        """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(User.class)
                        .list());
    }
}