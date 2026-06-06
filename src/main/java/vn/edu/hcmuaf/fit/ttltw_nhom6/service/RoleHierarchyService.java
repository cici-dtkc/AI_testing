package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;

public class RoleHierarchyService {

    //kiem tra role A co quyen cao hon role B khong (level cang nho -> quyen cang cao)
    public static boolean isHigherThan(Role roleA, Role roleB) {
        return roleA.getLevel() < roleB.getLevel();
    }

    // chỉ đc gán role cho level lớn hơn mình(level 2 gán role cho level 3)
    public static boolean canAssignRole(Role currentUserRole, Role targetRole) {
        return currentUserRole.getLevel() < targetRole.getLevel();
    }

    // override user có role level cao hơn mình
    public static boolean canOverridePermission(Role currentUserRole, Role targetUserRole) {
        return currentUserRole.getLevel() < targetUserRole.getLevel();
    }

    public static boolean isSuperAdmin(Role role) {
        return role != null && role.getLevel() == 1;
    }

}