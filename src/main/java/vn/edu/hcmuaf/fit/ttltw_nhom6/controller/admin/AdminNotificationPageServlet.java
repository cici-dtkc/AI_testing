package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/notifications")
public class AdminNotificationPageServlet extends HttpServlet {
    private NotificationDAO notificationDAO;
    private static final int ADMIN_ID = 1;
    private static final int PAGE_SIZE = 15;

    @Override
    public void init() throws ServletException {
        notificationDAO = NotificationDAO.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filter = request.getParameter("filter");
        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null) page = Integer.parseInt(pageParam);
        } catch (NumberFormatException ignored) {}

        String typeFilter;
        if ("login".equals(filter)) {
            typeFilter = "USER_VIOLATION_LOGIN";
        } else if ("cancel".equals(filter)) {
            typeFilter = "USER_VIOLATION_CANCEL";
        } else {
            typeFilter = "USER_VIOLATION%";
        }
        List<Notification> notifications = notificationDAO.getAdminViolationList(typeFilter, page, PAGE_SIZE);
        long totalCount = notificationDAO.countAdminViolation(typeFilter);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

        request.setAttribute("notifications", notifications);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("currentPage", page);
        request.setAttribute("currentFilter", filter);

        request.getRequestDispatcher("/frontend/admin/notification.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}