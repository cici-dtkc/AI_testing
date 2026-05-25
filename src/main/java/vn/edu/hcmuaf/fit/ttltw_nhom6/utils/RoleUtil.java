package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

public class RoleUtil {
    public static boolean hasAccess(String userRole, String requiredRole) {
        if ("admin".equalsIgnoreCase(userRole)) {
            return true;
        }
        if ("user".equalsIgnoreCase(userRole)
                && "user".equalsIgnoreCase(requiredRole)) {
            return true;
        }
        return false;
    }
}
