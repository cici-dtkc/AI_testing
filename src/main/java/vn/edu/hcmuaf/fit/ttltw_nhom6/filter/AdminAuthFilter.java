package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;

import java.io.IOException;

@WebFilter({"/frontend/admin/*", "/admin/*"})
public class AdminAuthFilter implements Filter {
    private final RoleDAO roleDAO = new RoleDAO();
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        // Kiểm tra đã đăng nhập chưa
        if (session == null || session.getAttribute("currentUser") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        // Kiểm tra có phải admin không
        User user = (User) session.getAttribute("currentUser");
        Role role = roleDAO.getRoleById(user.getRoleId());
//        boolean isAdmin = "comicstore365@gmail.com".equals(user.getEmail()) ||
//                "comicstore365".equals(user.getUsername());

        // Hoặc kiểm tra theo role:
        boolean isAdmin = RoleUtil.hasAccess(role, "super_admin")
                        || RoleUtil.hasAccess(role, "admin_product")
                        || RoleUtil.hasAccess(role, "admin_order")
                        || RoleUtil.hasAccess(role, "admin_user")
                        || RoleUtil.hasAccess(role, "admin_promotion");

        if (!isAdmin) {
            session.setAttribute("errorMessage", "Bạn không có quyền truy cập trang admin.");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
//            httpResponse.sendRedirect(httpRequest.getContextPath() + "/home");
            return;
        }

        // Cho phép truy cập
        chain.doFilter(request, response);
    }
}