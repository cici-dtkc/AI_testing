package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;
@WebFilter(urlPatterns = {"/admin/*", "/frontend/admin/*"})
public class AdminNotificationFilter implements Filter {
    private NotificationDAO notificationDAO;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        notificationDAO = NotificationDAO.getInstance();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);

        // Kiểm tra admin đã login
        if (session != null) {
            User currentUser = (User) session.getAttribute("currentUser");
            Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

            if (currentUser != null && isAdmin != null && isAdmin) {
                try {
                    // Load notifications cho admin
                    List<Notification> notifications = notificationDAO.getRecentForAdminViolationOnly(10);
                    int unreadCount = notificationDAO.countUnreadViolationForAdmin();

                    request.setAttribute("notifications", notifications);
                    request.setAttribute("unreadNotificationCount", unreadCount);

                } catch (Exception e) {
                    e.printStackTrace();

                    // Set mặc định nếu lỗi
                    request.setAttribute("notifications", List.of());
                    request.setAttribute("unreadNotificationCount", 0);
                }
            } else {
                // Không phải admin
                request.setAttribute("notifications", List.of());
                request.setAttribute("unreadNotificationCount", 0);
            }
        } else {
            // Không có session
            request.setAttribute("notifications", List.of());
            request.setAttribute("unreadNotificationCount", 0);
        }

        // Tiếp tục chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
