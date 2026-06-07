package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserPermissionDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

public class RoleUtil {
    public static boolean hasAccess(Role role, String requiredRole) {
        if (role == null) {
            return false;
        }
        if ("super_admin".equalsIgnoreCase(role.getRoleName())) {
            return true;
        }
        return role.getRoleName().equalsIgnoreCase(requiredRole);
    }

    public static boolean canManage(User user, String permissionName) {
        if (user == null) {
            return false;
        }
        try {
            RoleDAO roleDAO = new RoleDAO();
            Role role = roleDAO.getRoleById(user.getRoleId());
            if (role != null && "super_admin".equalsIgnoreCase(role.getRoleName())) {
                return true;
            }
            UserPermissionDAO userPermissionDAO = new UserPermissionDAO();
            return userPermissionDAO.hasPermission(user.getId(), permissionName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
