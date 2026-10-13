package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.gson.GsonConfig;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "UserNotificationServlet", value = "/NotificationServlet/*")
public class UserNotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = NotificationDAO.getInstance();
    private Gson gson;

    @Override
    public void init() throws ServletException {
        gson = GsonConfig.getGson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User user = (User) request.getSession().getAttribute("currentUser");
        if (user == null) {
            response.setStatus(401);
            response.getWriter().write("{\"error\": \"Not authenticated\"}");
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            if ("/count".equals(pathInfo)) {
                handleCount(request, response, user);
            } else if ("/recent".equals(pathInfo)) {
                handleRecent(request, response, user);
            } else if ("/list".equals(pathInfo)) {
                handleList(request, response, user);
            } else {
                response.setStatus(404);
                response.getWriter().write("{\"error\": \"Endpoint not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User user = (User) request.getSession().getAttribute("currentUser");
        if (user == null) {
            response.setStatus(401);
            response.getWriter().write("{\"error\": \"Not authenticated\"}");
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            if ("/mark-read".equals(pathInfo)) {
                handleMarkRead(request, response, user);
            } else if ("/mark-all-read".equals(pathInfo)) {
                handleMarkAllRead(request, response, user);
            } else {
                response.setStatus(404);
                response.getWriter().write("{\"error\": \"Endpoint not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }


    private void handleCount(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        int unreadCount = notificationDAO.countUnread(user.getId());

        JsonObject json = new JsonObject();
        json.addProperty("unread_count", unreadCount);

        response.getWriter().write(gson.toJson(json));
    }

    private void handleRecent(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        String limitParam = request.getParameter("limit");
        int limit = (limitParam != null) ? Integer.parseInt(limitParam) : 10;

        List<Notification> notifications = notificationDAO.getRecent(user.getId(), limit);
        int unreadCount = notificationDAO.countUnread(user.getId());

        for (Notification noti : notifications) {
            if (noti.getCreatedAt() != null) {
                noti.setFormattedCreatedAt(formatRelativeTime(noti.getCreatedAt()));
            }
            noti.setTitle(generateTitle(noti.getType()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notifications);
        result.put("unread_count", unreadCount);

        response.getWriter().write(gson.toJson(result));
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        String pageParam = request.getParameter("page");
        String pageSizeParam = request.getParameter("pageSize");
        String typeFilter = request.getParameter("type");

        int page = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
        int pageSize = (pageSizeParam != null) ? Integer.parseInt(pageSizeParam) : 20;

        if ("ALL".equals(typeFilter) || typeFilter == null) {
            typeFilter = null;
        }

        List<Notification> notifications = notificationDAO.getList(user.getId(), typeFilter, page, pageSize);

        for (Notification noti : notifications) {
            if (noti.getCreatedAt() != null) {
                String formattedDate = formatRelativeTime(noti.getCreatedAt());
                noti.setFormattedCreatedAt(formattedDate);

                Map<String, Object> extraData = new HashMap<>();
                extraData.put("formatted_date", formattedDate);
            }

            String title = generateTitle(noti.getType());
            noti.setTitle(title);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notifications);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", notifications.size());

        String jsonResponse = gson.toJson(result);

        response.getWriter().write(jsonResponse);
    }

    private void handleMarkRead(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.setStatus(400);
            response.getWriter().write("{\"error\": \"Missing id parameter\"}");
            return;
        }

        int notiId = Integer.parseInt(idParam);

        notificationDAO.markAsread(notiId);

        response.getWriter().write("{\"success\": true}");
    }

    private void handleMarkAllRead(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        notificationDAO.markAllAsRead(user.getId());

        response.getWriter().write("{\"success\": true}");
    }

    private String generateTitle(String type) {
        if (type == null) return "Thông báo";

        switch (type) {
            case "ORDER_CONFIRMED":
                return "Đơn hàng đã được xác nhận";
            case "ORDER_SHIPPED":
                return "Đơn hàng đang được giao";
            case "ORDER_DELIVERED":
                return "Đơn hàng đã giao thành công";
            case "ORDER_CANCELLED":
                return "Đơn hàng đã bị hủy";
            case "REFUND_APPROVED":
                return "Yêu cầu hoàn tiền đã được chấp nhận";
            case "REFUND_REJECTED":
                return "Yêu cầu hoàn tiền bị từ chối";
            case "ORDER_UPDATE":
                return "Cập nhật đơn hàng";
            case "PROMOTION":
                return "Khuyến mãi mới";
            case "SYSTEM":
                return "Thông báo hệ thống";
            case "FLASH_SALE_UPCOMING":
                return "Flash Sale sắp diễn ra!";
            case "FLASH_SALE_STARTED":
                return "Flash Sale đã bắt đầu!";
            case "VOUCHER_NEW":
                return "Mã giảm giá mới dành cho bạn";
            case "VOUCHER_EXPIRING":
                return "Mã giảm giá sắp hết hạn";
            default:
                return "Thông báo";
        }
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }

        long createdAtMillis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long diff = System.currentTimeMillis() - createdAtMillis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return createdAt.format(formatter);
        }
    }
}