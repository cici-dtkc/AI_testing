package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.UserPermissonOverride;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPermissionDAO {
    private final Jdbi jdbi;

    public UserPermissionDAO() {
        this.jdbi = JdbiConnector.get();
    }

    // admin gán role cho user
    public boolean assignRoleToUser(int userId, int roleId, String roleName) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("""
                    UPDATE users
                    SET role_id = :roleId, role = :roleName, updated_at = NOW()
                    WHERE id = :userId
                    """)
                        .bind("roleId", roleId)
                        .bind("roleName", roleName)
                        .bind("userId", userId)
                        .execute() > 0
        );
    }

    public List<UserPermissonOverride> getOverridesByUserId(int userId) {
        String sql = """
            SELECT upo.id, upo.user_id, upo.permission_id, 
                   upo.is_granted, upo.created_at, p.permission_name
            FROM user_permission_overrides upo
            JOIN permissions p ON p.id = upo.permission_id
            WHERE upo.user_id = :userId
            """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .map((rs, ctx) -> {
                            UserPermissonOverride o = new UserPermissonOverride();
                            o.setId(rs.getInt("id"));
                            o.setUserId(rs.getInt("user_id"));
                            o.setPermissionId(rs.getInt("permission_id"));
                            o.setGranted(rs.getInt("is_granted") == 1);
                            o.setPermissionName(rs.getString("permission_name"));
                            return o;
                        })
                        .list()
        );
    }

    public boolean upsertOverride(int userId, int permissionId, boolean isGranted) {
        try {
            Integer existingId = jdbi.withHandle(handle ->
                    handle.createQuery("""
                SELECT id FROM user_permission_overrides
                WHERE user_id = :userId AND permission_id = :permissionId
                """)
                            .bind("userId", userId)
                            .bind("permissionId", permissionId)
                            .mapTo(Integer.class)
                            .findOne()
                            .orElse(null)
            );

            if (existingId == null) {
                int rows = jdbi.withHandle(handle ->
                        handle.createUpdate("""
                    INSERT INTO user_permission_overrides (user_id, permission_id, is_granted)
                    VALUES (:userId, :permissionId, :isGranted)
                    """)
                                .bind("userId", userId)
                                .bind("permissionId", permissionId)
                                .bind("isGranted", isGranted)
                                .execute()
                );
                return rows > 0;
            } else {
                int rows = jdbi.withHandle(handle ->
                        handle.createUpdate("""
                    UPDATE user_permission_overrides
                    SET is_granted = :isGranted
                    WHERE user_id = :userId AND permission_id = :permissionId
                    """)
                                .bind("isGranted", isGranted)
                                .bind("userId", userId)
                                .bind("permissionId", permissionId)
                                .execute()
                );
                return rows >= 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // xóa override của user (reset về quyền mặc định)
    public boolean removeOverride(int userId, int permissionId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("""
                    DELETE FROM user_permission_overrides
                    WHERE user_id = :userId AND permission_id = :permissionId
                    """)
                        .bind("userId", userId)
                        .bind("permissionId", permissionId)
                        .execute() > 0
        );
    }

    // xóa toàn bộ override của user (reset toàn bộ về mặc định)
    public boolean removeAllOverrides(int userId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("""
                    DELETE FROM user_permission_overrides
                    WHERE user_id = :userId
                    """)
                        .bind("userId", userId)
                        .execute() > 0
        );
    }

    // kiểm tra quyền thực tế khi user thao tác
    public Set<String> getEffectivePermissions(int userId) {
        String roleSql = """
                SELECT p.permission_name
                FROM permissions p
                JOIN role_permissions rp ON p.id = rp.permission_id
                JOIN users u ON u.role_id = rp.role_id
                WHERE u.id = :userId
                """;
        Set<String> perms = jdbi.withHandle(handle ->
                handle.createQuery(roleSql)
                        .bind("userId", userId)
                        .mapTo(String.class)
                        .collect(Collectors.toSet())
        );

        // Thêm các quyền mặc định dựa trên vai trò để dự phòng
        try {
            String roleName = jdbi.withHandle(handle ->
                    handle.createQuery("SELECT r.role_name FROM roles r JOIN users u ON u.role_id = r.id WHERE u.id = :userId")
                            .bind("userId", userId)
                            .mapTo(String.class)
                            .findOne()
                            .orElse(null)
            );
            if (roleName != null) {
                if ("super_admin".equalsIgnoreCase(roleName)) {
                    perms.add("manage_product");
                    perms.add("manage_series");
                    perms.add("manage_category");
                    perms.add("manage_order");
                    perms.add("manage_inventory");
                    perms.add("manage_user");
                    perms.add("manage_flashsale");
                    perms.add("manage_promotion");
                    perms.add("manage_permissions");
                } else if ("admin_product".equalsIgnoreCase(roleName)) {
                    perms.add("manage_product");
                    perms.add("manage_series");
                    perms.add("manage_category");
                } else if ("admin_order".equalsIgnoreCase(roleName)) {
                    perms.add("manage_order");
                } else if ("admin_user".equalsIgnoreCase(roleName)) {
                    perms.add("manage_user");
                    perms.add("manage_permissions");
                } else if ("admin_promotion".equalsIgnoreCase(roleName)) {
                    perms.add("manage_promotion");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<UserPermissonOverride> overrides = getOverridesByUserId(userId);
        for (UserPermissonOverride o : overrides) {
            String permissionName = o.getPermissionName();
            if (o.isGranted()) {
                perms.add(permissionName);
            } else {
                perms.remove(permissionName);
                // Xóa cả trường hợp khác biệt hoa thường
                perms.remove(permissionName.toLowerCase());
                perms.remove(permissionName.toUpperCase());
                if (permissionName.equalsIgnoreCase("manage_product")) {
                    perms.remove("manage_product");
                    perms.remove("manage_Product");
                }
            }
        }
        return perms;
    }

    // kiểm tra 1 quyền cụ thể
    public boolean hasPermission(int userId, String permissionName) {
        if (permissionName == null) return false;
        Set<String> perms = getEffectivePermissions(userId);
        for (String p : perms) {
            if (permissionName.equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }

    // Lấy quyền thuần từ role
    public Set<String> getPermissionsFromRoleOnly(int userId) {
        String sql = """
            SELECT p.permission_name
            FROM permissions p
            JOIN role_permissions rp ON p.id = rp.permission_id
            JOIN users u ON u.role_id = rp.role_id
            WHERE u.id = :userId
            """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .mapTo(String.class)
                        .collect(Collectors.toSet())
        );
    }

}
