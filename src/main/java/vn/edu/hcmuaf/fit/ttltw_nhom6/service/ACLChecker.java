package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.util.List;

public class ACLChecker {
    private static final RoleDAO roleDAO = new RoleDAO();

    public static boolean hasPermission(User user, String permissionName) {
        if (user == null) return false;

        if (RoleHierarchyService.isSuperAdmin(
                roleDAO.getRoleById(user.getRoleId()))) {
            return true;
        }

        List<String> revoked = roleDAO.getRevokedOverridesByUserId(user.getId());
        if (revoked.contains(permissionName)) return false;

        List<String> granted = roleDAO.getGrantedOverridesByUserId(user.getId());
        if (granted.contains(permissionName)) return true;

        List<String> rolePermissions = roleDAO.getPermissionsByRoleId(user.getRoleId());
        return rolePermissions.contains(permissionName);
    }
}
