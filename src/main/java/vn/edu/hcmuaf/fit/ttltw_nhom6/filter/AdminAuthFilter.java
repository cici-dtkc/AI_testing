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
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.RoleHierarchyService;
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
        if (session == null || session.getAttribute("currentUser") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("currentUser");
        Role role = roleDAO.getRoleById(user.getRoleId());

        if (role == null || role.getLevel() >= 3) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang admin.");
            return;
        }

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = uri.substring(contextPath.length());
        boolean isSuperAdminOnly = path.startsWith("/admin/permission-management") ||
                path.startsWith("/admin/permission-override") ||
                path.startsWith("/admin/role");
        if (isSuperAdminOnly && !RoleHierarchyService.isSuperAdmin(role)) {
            String requestedWith = httpRequest.getHeader("X-Requested-With");
            String contentType = httpRequest.getHeader("Accept");
            if ("XMLHttpRequest".equals(requestedWith) ||
                    (contentType != null && contentType.contains("application/json"))) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\": \"Chức năng này chỉ dành cho Super Admin.\"}");
            } else {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này chỉ dành cho Super Admin.");
            }
            return;
        }
        String roleName = role.getRoleName();
        boolean isSuperOrAdmin = "super_admin".equalsIgnoreCase(roleName) || "admin".equalsIgnoreCase(roleName);
        boolean hasAccess = false;

        if (isSuperOrAdmin) {
            hasAccess = true;
        } else {
            String method = httpRequest.getMethod();
            if ("GET".equalsIgnoreCase(method)) {
                hasAccess = true;
            } else {
                boolean isProductPath = path.startsWith("/admin/ProductManagement")
                        || path.startsWith("/admin/products/add")
                        || path.startsWith("/admin/products/update")
                        || path.startsWith("/admin/products/delete")
                        || path.startsWith("/admin/add-comic")
                        || path.startsWith("/admin/update-comic")
                        || path.startsWith("/admin/delete-comic")
                        || path.startsWith("/admin/toggle-comic-hidden")
                        || path.startsWith("/admin/list-comics")
                        || path.startsWith("/admin/get-comic-detail")
                        || path.startsWith("/admin/get-comic-images")
                        || path.startsWith("/admin/search-comics");

                boolean isSeriesPath = path.startsWith("/admin/SeriesManagement")
                        || path.startsWith("/admin/series/")
                        || path.startsWith("/admin/add-series")
                        || path.startsWith("/admin/edit-series")
                        || path.startsWith("/admin/delete-series");

                boolean isCategoryPath = path.startsWith("/admin/CategoryManagement")
                        || path.startsWith("/admin/category/")
                        || path.startsWith("/admin/addCategory")
                        || path.startsWith("/admin/updateCategory")
                        || path.startsWith("/admin/deleteCategory")
                        || path.startsWith("/admin/listCategories")
                        || path.startsWith("/admin/getCategoryById")
                        || path.startsWith("/admin/categories/");

                boolean isFlashSalePath = path.startsWith("/admin/manage-flashsale")
                        || path.startsWith("/admin/create-flashsale");

                boolean isInventoryPath = path.startsWith("/admin/inventory");

                if (isProductPath || isCategoryPath) {
                    // admin_product được sửa Sản phẩm và Thể loại
                    hasAccess = "admin_product".equalsIgnoreCase(roleName)
                            || RoleUtil.canManage(user, "manage_product")
                            || RoleUtil.canManage(user, "manage_category");
                } else if (isSeriesPath) {
                    // Chỉ admin_product mới được thêm/sửa/xóa Series
                    hasAccess = "admin_product".equalsIgnoreCase(roleName)
                            || RoleUtil.canManage(user, "manage_series");
                } else if (isFlashSalePath) {
                    String action = httpRequest.getParameter("action");
                    if ("detail".equals(action)) {
                        hasAccess = true;
                    } else {
                        hasAccess = "admin_promotion".equalsIgnoreCase(roleName)
                                || RoleUtil.canManage(user, "manage_flashsale");
                    }
                } else if (isInventoryPath) {
                    hasAccess = RoleUtil.canManage(user, "manage_inventory");
                } else if (path.startsWith("/admin/user-management")
                        || path.startsWith("/admin/permission-management")
                        || path.startsWith("/admin/permission-override")) {
                    hasAccess = "admin_user".equalsIgnoreCase(roleName)
                            || RoleUtil.canManage(user, "manage_user")
                            || RoleUtil.canManage(user, "manage_permissions");
                } else if (path.startsWith("/admin/orders") || path.startsWith("/admin/order-management")) {
                    hasAccess = "admin_order".equalsIgnoreCase(roleName)
                            || RoleUtil.canManage(user, "manage_order");
                } else if (path.startsWith("/admin/vouchersManagement")
                        || path.startsWith("/admin/vouchers")
                        || path.startsWith("/admin/deleteVoucher")
                        || path.startsWith("/admin/editVoucher")
                        || path.startsWith("/admin/search-voucher")
                        || path.startsWith("/admin/loadCategory")
                        || path.startsWith("/admin/sortVoucher")) {
                    hasAccess = "admin_promotion".equalsIgnoreCase(roleName)
                            || RoleUtil.canManage(user, "manage_promotion");
                } else {
                    hasAccess = true;
                }
            }
        }

        boolean isPromotionOnly = path.startsWith("/admin/manage-flashsale") ||
                path.startsWith("/admin/create-flashsale") ||
                path.startsWith("/admin/vouchersManagement") ||
                path.startsWith("/admin/vouchers") ||
                path.startsWith("/admin/deleteVoucher") ||
                path.startsWith("/admin/editVoucher") ||
                path.startsWith("/admin/search-voucher") ||
                path.startsWith("/admin/loadCategory") ||
                path.startsWith("/admin/sortVoucher");

        String currentAction = httpRequest.getParameter("action");
        if (isPromotionOnly && !"detail".equals(currentAction) && !RoleHierarchyService.isSuperAdmin(role)) {
            boolean isAdminRole = "admin".equalsIgnoreCase(roleName);
            if (!isAdminRole) {
                String method = httpRequest.getMethod();
                if (!"GET".equalsIgnoreCase(method)) {
                    boolean hasAnyPromotionPermission = ACLChecker.hasPermission(user, "VIEW_FLASHSALE")
                            || ACLChecker.hasPermission(user, "VIEW_PROMOTION");
                    if (!hasAnyPromotionPermission) {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
            }
        }

        if (!hasAccess) {
            session.setAttribute("errorMessage", "Bạn không có quyền thực hiện hành động này.");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Cho phép truy cập
        chain.doFilter(request, response);
    }
}