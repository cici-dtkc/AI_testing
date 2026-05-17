package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationDAO {
    private static NotificationDAO instance;
    private final Jdbi jdbi;
    private static final int ADMIN_ID = 1;

    public NotificationDAO(Jdbi jdbi) {
        this.jdbi = JdbiConnector.get();
    }

    public NotificationDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public static NotificationDAO getInstance() {
        if (instance == null) {
            instance = new NotificationDAO();
        }
        return instance;
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, userId)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public List<Notification> getRecent(int userId, int limit) {
        String sql = """
                SELECT id, user_id, message, type, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, userId)
                        .bind(1, limit)
                        .map(new NotificationRowMapper())
                        .collect(Collectors.toList())
        );
    }

    public List<Notification> getList(int userId, String typeFilter, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, user_id, message, type, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                """);
        if ("EVENT".equals(typeFilter)) {
            sql.append("""
             AND type IN (
                 'PROMOTION',
                 'FLASH_SALE_UPCOMING',
                 'FLASH_SALE_STARTED',
                 'VOUCHER_NEW',
                 'VOUCHER_EXPIRING'
             )
            """);
        } else if ("ORDER".equals(typeFilter)) {
            sql.append(" AND type LIKE 'ORDER_%'");
        } else if (typeFilter != null) {
            sql.append(" AND type = ?");
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        return JdbiConnector.get().withHandle(handle -> {
            var query = handle.createQuery(sql.toString()).bind(0, userId);
            int paramIndex = 1;
            if (typeFilter != null
                    && !"ORDER".equals(typeFilter)
                    && !"EVENT".equals(typeFilter)) {
                query.bind(paramIndex++, typeFilter);
            }
            query.bind(paramIndex++, pageSize);
            query.bind(paramIndex, (page - 1) * pageSize);
            return query.map(new NotificationRowMapper()).list();
        });
    }

    public void markAsread(int notiId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        JdbiConnector.get().useHandle(handle ->
                handle.createUpdate(sql).bind(0, notiId).execute());
    }

    public void markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        JdbiConnector.get().useHandle(handle ->
                handle.createUpdate(sql).bind(0, userId).execute());
    }

    public void insert(Notification noti) {
        String sql = """
                INSERT INTO notifications (user_id, message, type, is_read, created_at)
                VALUES (?, ?, ?, 0, NOW())
                """;
        JdbiConnector.get().useHandle(handle ->
                handle.createUpdate(sql)
                        .bind(0, noti.getUserId())
                        .bind(1, noti.getMessage())
                        .bind(2, noti.getType())
                        .execute()
        );
    }

    //THÔNG BÁO CỦA ADMIN VỀ VI PHẠM CỦA USER
    public int countUnreadForAdmin() {
        return countUnread(ADMIN_ID);
    }

    public List<Notification> getRecentForAdmin(int limit) {
        return getRecent(ADMIN_ID, limit);
    }

    public void insertForAdmin(Notification noti) {
        noti.setUserId(ADMIN_ID);
        insert(noti);
    }

    public String getFCMToken(int userId) {
        String sql = "SELECT token FROM fcm_tokens WHERE user_id = ? ORDER BY updated_at DESC LIMIT 1";

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind(0, userId)
                            .mapTo(String.class)
                            .findOne()
                            .orElse(null)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public void saveFCMToken(int userId, String token) {
        String sql = "INSERT INTO fcm_tokens (user_id, token) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP";

        try {
            jdbi.useHandle(handle ->
                    handle.createUpdate(sql)
                            .bind(0, userId)
                            .bind(1, token)
                            .execute()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Notification> getRecentForAdminViolationOnly(int limit) {
        String sql = """
            SELECT id, user_id, message, type, is_read, created_at
            FROM notifications
            WHERE user_id = ?
            AND type LIKE 'USER_VIOLATION%'
            ORDER BY created_at DESC
            LIMIT ?
            """;
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, ADMIN_ID)
                        .bind(1, limit)
                        .map(new NotificationRowMapper())
                        .collect(Collectors.toList())
        );
    }

    public int countUnreadViolationForAdmin() {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0 AND type LIKE 'USER_VIOLATION%'";
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, ADMIN_ID)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public List<Notification> getAdminViolationList(String typeFilter, int page, int pageSize) {
        String sql = """
            SELECT id, user_id, message, type, is_read, created_at
            FROM notifications
            WHERE user_id = ?
            AND type LIKE ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, ADMIN_ID)
                        .bind(1, typeFilter)
                        .bind(2, pageSize)
                        .bind(3, (page - 1) * pageSize)
                        .map(new NotificationRowMapper())
                        .list()
        );
    }

    public long countAdminViolation(String typeFilter) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type LIKE ?";
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, ADMIN_ID)
                        .bind(1, typeFilter)
                        .mapTo(Long.class)
                        .one()
        );
    }

    public boolean existsUnreadViolationForUser(int violatorUserId, String type) {
        String sql = """
        SELECT COUNT(*) FROM notifications
        WHERE user_id = :adminId
          AND is_read = 0
          AND type = :type
          AND message LIKE :pattern
        """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("adminId", ADMIN_ID)
                        .bind("type", type)
                        .bind("pattern", "%(ID: " + violatorUserId + ")%")
                        .mapTo(Integer.class)
                        .one()
        ) > 0;
    }

    private static class NotificationRowMapper implements RowMapper<Notification> {
        private static final DateTimeFormatter DISPLAY_FORMATTER =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        @Override
        public Notification map(ResultSet rs, StatementContext ctx) throws SQLException {
            Notification notification = new Notification();

            notification.setId(rs.getInt("id"));
            notification.setUserId(rs.getInt("user_id"));
            notification.setMessage(rs.getString("message"));
            notification.setType(rs.getString("type"));
            notification.setRead(rs.getBoolean("is_read"));

            LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            notification.setCreatedAt(createdAt);
            notification.setFormattedCreatedAt(createdAt.format(DISPLAY_FORMATTER));

            return notification;
        }
    }

    public void insertForAllUsers(String message, String type) {
        String getUsersSql = "SELECT id FROM users WHERE is_deleted = 0";
        List<Integer> userIds = jdbi.withHandle(handle ->
                handle.createQuery(getUsersSql)
                        .mapTo(Integer.class)
                        .list()
        );

        if (userIds.isEmpty()) return;

        String insertSql = """
        INSERT INTO notifications (user_id, message, type, is_read, created_at)
        VALUES (?, ?, ?, 0, NOW())
        """;

        jdbi.useHandle(handle -> {
            var batch = handle.prepareBatch(insertSql);
            for (int userId : userIds) {
                batch.bind(0, userId)
                        .bind(1, message)
                        .bind(2, type)
                        .add();
            }
            batch.execute();
        });
    }
}