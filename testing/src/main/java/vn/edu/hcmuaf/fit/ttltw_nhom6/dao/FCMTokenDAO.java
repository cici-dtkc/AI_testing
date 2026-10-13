package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;

public class FCMTokenDAO {
    private static FCMTokenDAO instance;

    public static FCMTokenDAO getInstance() {
        if (instance == null) {
            instance = new FCMTokenDAO();
        }
        return instance;
    }

    // Lưu hoặc cập nhật token (nếu đã tồn tại thì update)
    public boolean saveOrUpdate(int userId, String token) {
        String sql = """
            INSERT INTO fcm_tokens (user_id, token) 
            VALUES (?, ?) 
            ON DUPLICATE KEY UPDATE 
            token = VALUES(token), 
            updated_at = CURRENT_TIMESTAMP
            """;

        return JdbiConnector.get().withHandle(handle ->
                handle.createUpdate(sql)
                        .bind(0, userId)
                        .bind(1, token)
                        .execute() > 0
        );
    }

    // Lấy token của user (dùng khi gửi thông báo)
    public String getToken(int userId) {
        String sql = "SELECT token FROM fcm_tokens WHERE user_id = ? ORDER BY updated_at DESC LIMIT 1";
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, userId)
                        .mapTo(String.class)
                        .findFirst()
                        .orElse(null)
        );
    }

    // Lấy tất cả token của user (nếu có nhiều thiết bị)
    public java.util.List<String> getAllTokens(int userId) {
        String sql = "SELECT token FROM fcm_tokens WHERE user_id = ?";
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, userId)
                        .mapTo(String.class)
                        .list()
        );
    }
}