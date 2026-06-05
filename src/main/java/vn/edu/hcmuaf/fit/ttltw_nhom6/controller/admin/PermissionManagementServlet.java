package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.RoleHierarchyService;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/permission-management")
public class PermissionManagementServlet extends HttpServlet {
    private UserDao userDao;
    private RoleDAO roleDao;

    @Override
    public void init() throws ServletException {
        try {
            userDao = new UserDao(JdbiConnector.get());
            roleDao = new RoleDAO();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<User> users = userDao.getAllUsers();
            List<Role> roles = roleDao.getAllRoles();

            request.setAttribute("users", users);
            request.setAttribute("roles", roles);

            request.getRequestDispatcher("/frontend/admin/roleManagement.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Lỗi: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userIdStr = request.getParameter("userId");
        String roleIdStr = request.getParameter("roleId");

        if (userIdStr != null && roleIdStr != null) {
            int targetUserId = Integer.parseInt(userIdStr);
            int newRoleId = Integer.parseInt(roleIdStr);

            HttpSession session = request.getSession(false);
            User currentAdmin = (User) session.getAttribute("currentUser");
            Role currentAdminRole = roleDao.getRoleById(currentAdmin.getRoleId());
            Role newRole = roleDao.getRoleById(newRoleId);

            if (!RoleHierarchyService.canAssignRole(currentAdminRole, newRole)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Bạn không thể gán role cao hơn hoặc bằng quyền của mình!\"}");
                return;
            }

            User targetUser = userDao.getUserById(targetUserId);
            Role targetCurrentRole = roleDao.getRoleById(targetUser.getRoleId());
            if (!RoleHierarchyService.canAssignRole(currentAdminRole, targetCurrentRole)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Bạn không thể thay đổi quyền của user có cấp bậc cao hơn hoặc bằng mình!\"}");
                return;
            }
            userDao.updateRole(targetUserId, newRoleId);
        }
        response.sendRedirect(request.getContextPath() + "/admin/permission-management");
    }
}