package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ACLChecker;

import java.io.IOException;

@WebFilter({
        "/admin/vouchersManagement",
        "/admin/vouchers",
        "/admin/deleteVoucher",
        "/admin/editVoucher",
        "/admin/loadCategory",
        "/admin/search-voucher",
        "/admin/sortVoucher",
        "/admin/manage-flashsale",
        "/admin/create-flashsale"
})
public class PromotionAccessFilter implements Filter {
    private final RoleDAO roleDAO = new RoleDAO();
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        User user = (User) session.getAttribute("currentUser");
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String method = httpRequest.getMethod();
        String action = httpRequest.getParameter("action");

        Role role = roleDAO.getRoleById(user.getRoleId());
        String roleName = (role != null) ? role.getRoleName() : "";
        boolean isPromotionAdmin = "admin_promotion".equalsIgnoreCase(roleName)
                || "super_admin".equalsIgnoreCase(roleName)
                || "admin".equalsIgnoreCase(roleName);

        if (!isPromotionAdmin) {
            if (!"GET".equalsIgnoreCase(method)) {
                if (!"detail".equals(action)) {
                    sendForbidden(httpResponse, true);
                    return;
                }
            }
            chain.doFilter(request, response);
            return;
        }

        boolean skipAclCheck = "admin_promotion".equalsIgnoreCase(roleName)
                || "super_admin".equalsIgnoreCase(roleName)
                || "admin".equalsIgnoreCase(roleName);

        if (path.contains("flashsale") || path.contains("flashSale")) {
            if (method.equals("GET") && !skipAclCheck && !ACLChecker.hasPermission(user, "VIEW_FLASHSALE")) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (path.contains("create-flashsale") && method.equals("POST") && !skipAclCheck
                    && !ACLChecker.hasPermission(user, "ADD_FLASHSALE")) {
                sendForbidden(httpResponse, true);
                return;
            }
            if ("delete".equals(action)  && !skipAclCheck && !ACLChecker.hasPermission(user, "DELETE_FLASHSALE")) {
                sendForbidden(httpResponse, true);
                return;
            }
            if ("update".equals(action) && !skipAclCheck && !ACLChecker.hasPermission(user, "EDIT_FLASHSALE")) {
                sendForbidden(httpResponse, true);
                return;
            }
        }

        if (path.contains("oucher") || path.contains("ategory")) {
            if (method.equals("GET") && !skipAclCheck && !ACLChecker.hasPermission(user, "VIEW_PROMOTION")) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (path.contains("deleteVoucher") && !skipAclCheck && !ACLChecker.hasPermission(user, "DELETE_PROMOTION")) {
                sendForbidden(httpResponse, true);
                return;
            }
            if (path.contains("editVoucher") && !skipAclCheck && !ACLChecker.hasPermission(user, "EDIT_PROMOTION")) {
                sendForbidden(httpResponse, true);
                return;
            }
            if (path.contains("/admin/vouchers") && method.equals("POST") && !skipAclCheck
                    && !ACLChecker.hasPermission(user, "ADD_PROMOTION")) {
                sendForbidden(httpResponse, true);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void sendForbidden(HttpServletResponse response, boolean isAjax) throws IOException {
        if (isAjax) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền thực hiện thao tác này!\"}");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
