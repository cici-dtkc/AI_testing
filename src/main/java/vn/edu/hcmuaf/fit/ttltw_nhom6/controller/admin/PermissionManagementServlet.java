package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

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
            int userId = Integer.parseInt(userIdStr);
            int roleId = Integer.parseInt(roleIdStr);
            userDao.updateRole(userId, roleId);
        }
        response.sendRedirect(request.getContextPath() + "/admin/permission-management");
    }
}