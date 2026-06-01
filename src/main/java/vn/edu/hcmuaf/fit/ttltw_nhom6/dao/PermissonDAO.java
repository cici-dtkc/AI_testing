package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Permission;

import java.util.List;

public class PermissonDAO {
    private final Jdbi jdbi;

    public PermissonDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public List<Permission> getAllPermissions() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM permissions ORDER BY id")
                        .mapToBean(Permission.class)
                        .list()
        );
    }

    public List<Permission> getPermissionsByRoleId(int roleId) {
        String sql = """
                SELECT p.* FROM permissions p
                JOIN role_permissions rp ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("roleId", roleId)
                        .mapToBean(Permission.class)
                        .list()
        );
    }

    public boolean assignPermissionToRole(int roleId, int permissionId) {
        String sql = """
                INSERT IGNORE INTO role_permissions(role_id, permission_id)
                VALUES (:roleId, :permissionId)
                """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("roleId", roleId)
                        .bind("permissionId", permissionId)
                        .execute() > 0
        );
    }

    public boolean removePermissionFromRole(int roleId, int permissionId) {
        String sql = """
                DELETE FROM role_permissions
                WHERE role_id = :roleId AND permission_id = :permissionId
                """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("roleId", roleId)
                        .bind("permissionId", permissionId)
                        .execute() > 0
        );
    }

}
