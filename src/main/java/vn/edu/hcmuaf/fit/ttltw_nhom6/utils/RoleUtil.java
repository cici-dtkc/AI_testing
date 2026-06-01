package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;

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
}
