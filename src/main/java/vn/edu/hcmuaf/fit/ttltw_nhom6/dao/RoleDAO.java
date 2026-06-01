package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;

import java.util.List;

public class RoleDAO {
    private final Jdbi jdbi;

    public RoleDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public Role getRoleById(int id) {
        String sql = "SELECT * FROM roles WHERE id = :id";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", id)
                        .mapToBean(Role.class)
                        .findOne()
                        .orElse(null)
        );
    }

    public List<Role> getAllRoles() {
        String sql = "SELECT * FROM roles ORDER BY id";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Role.class)
                        .list()
        );
    }

    public boolean addRole(Role role) {
        String sql = """
                INSERT INTO roles(role_name, description)
                VALUES (:role_name, :description)
                """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("role_name", role.getRoleName())
                        .bind("description", role.getDescription())
                        .execute() > 0
        );
    }

    public boolean deleteRole(int id) {
        String sql = "DELETE FROM roles WHERE id = :id";
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("id", id)
                        .execute() > 0
        );
    }

    public boolean updateRole(Role role) {
        String sql = """
                UPDATE roles
                SET role_name = :role_name,
                    description = :description
                WHERE id = :id
                """;
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("id", role.getId())
                        .bind("role_name", role.getRoleName())
                        .bind("description", role.getDescription())
                        .execute() > 0
        );
    }
}
