package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;

import java.io.IOException;

@WebServlet({
        "/admin/mark-notification-read",
        "/admin/mark-all-notifications-read"
})
public class NotificationServlet extends HttpServlet {
    private NotificationDAO notificationDAO;
    private static final int ADMIN_ID = 1;

    @Override
    public void init() throws ServletException {
        notificationDAO = NotificationDAO.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();

        try {
            if (path.endsWith("mark-notification-read")) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    int id = Integer.parseInt(idParam);
                    notificationDAO.markAsread(id);
                    response.getWriter().write("{\"success\": true}");
                } else {
                    response.getWriter().write("{\"success\": false, \"error\": \"Missing id parameter\"}");
                }
            } else if (path.endsWith("mark-all-notifications-read")) {
                notificationDAO.markAllAsRead(ADMIN_ID);
                response.getWriter().write("{\"success\": true}");
            } else {
                response.getWriter().write("{\"success\": false, \"error\": \"Unknown action\"}");
            }
        } catch (NumberFormatException e) {
            response.getWriter().write("{\"success\": false, \"error\": \"Invalid ID format\"}");
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}