package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.PermissonDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserPermissionDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Permission;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.UserPermissonOverride;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/admin/permission-override")
public class UserPermissionOverrideServlet extends HttpServlet {
    private UserPermissionDAO userPermissionDAO;
    private PermissonDAO permissonDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        userPermissionDAO = new UserPermissionDAO();
        permissonDAO = new PermissonDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));

            List<Permission> allPermissions = permissonDAO.getAllPermissions();

            java.util.Set<String> rolePerms = userPermissionDAO.getPermissionsFromRoleOnly(userId);

            List<UserPermissonOverride> overrides = userPermissionDAO.getOverridesByUserId(userId);
            Map<Integer, Boolean> overrideMap = overrides.stream()
                    .collect(Collectors.toMap(
                            UserPermissonOverride::getPermissionId,
                            UserPermissonOverride::isGranted
                    ));

            List<Permission> result = new ArrayList<>();
            for (Permission p : allPermissions) {
                p.setFromRole(rolePerms.contains(p.getPermissionName()));

                if (overrideMap.containsKey(p.getId())) {
                    p.setOverrideStatus(overrideMap.get(p.getId()) ? "GRANTED" : "REVOKED");
                } else {
                    p.setOverrideStatus("NONE");
                }
                result.add(p);
            }

            response.getWriter().write(gson.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write(gson.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int userId = Integer.parseInt(request.getParameter("userId"));

        boolean success = false;

        switch (action) {
            case "grant" -> {
                int permissionId = Integer.parseInt(request.getParameter("permissionId"));
                success = userPermissionDAO.upsertOverride(userId, permissionId, true);
            }
            case "revoke" -> {
                int permissionId = Integer.parseInt(request.getParameter("permissionId"));
                success = userPermissionDAO.upsertOverride(userId, permissionId, false);
            }
            case "reset" -> {
                int permissionId = Integer.parseInt(request.getParameter("permissionId"));
                success = userPermissionDAO.removeOverride(userId, permissionId);
            }
            case "resetAll" -> {
                success = userPermissionDAO.removeAllOverrides(userId);
            }
        }
        response.getWriter().write(gson.toJson(Map.of("success", success)));
    }
}