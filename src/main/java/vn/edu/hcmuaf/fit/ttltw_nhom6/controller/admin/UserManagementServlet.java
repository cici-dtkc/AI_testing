package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/user-management")
public class UserManagementServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            UserDao userDao = new UserDao(JdbiConnector.get());

            String search = request.getParameter("search");
            String levelFilter = request.getParameter("level");

            List<User> users;

            if (search != null && !search.trim().isEmpty() && levelFilter != null && !levelFilter.trim().isEmpty()) {
                users = userDao.searchAndFilterCustomers(search, levelFilter);
            } else if (search != null && !search.trim().isEmpty()) {
                users = userDao.searchCustomers(search);
            } else if (levelFilter != null && !levelFilter.trim().isEmpty()) {
                users = userDao.filterCustomersByMembershipLevel(levelFilter);
            } else {
                users = userDao.getAllCustomers();
            }

            String statusFilter = request.getParameter("status");

            if (statusFilter != null && !statusFilter.trim().isEmpty()) {
                if ("locked".equals(statusFilter)) {
                    users = users.stream().filter(u -> !u.isActive()).collect(java.util.stream.Collectors.toList());
                } else if ("active".equals(statusFilter)) {
                    users = users.stream().filter(u -> u.isActive()).collect(java.util.stream.Collectors.toList());
                }
            }


            if (users != null && !users.isEmpty()) {
                System.out.println("First user: " + users.get(0));
            }

            request.setAttribute("users", users);
            request.getRequestDispatcher("/frontend/admin/userManagement.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi xử lý dữ liệu");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String action = request.getParameter("action");

        try {
            UserDao userDao = new UserDao(JdbiConnector.get());

            if ("upgrade".equals(action)) {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String newLevel = request.getParameter("newLevel");

                Map<String, Object> eligibility = userDao.checkUpgradeEligibility(userId, newLevel);

                if ((boolean) eligibility.get("eligible")) {
                    boolean success = userDao.upgradeMembershipLevel(userId, newLevel);

                    if (success) {
                        response.getWriter().write(
                                "{\"status\":\"success\",\"message\":\"Nâng cấp thành công lên " + newLevel + "\"}"
                        );
                    } else {
                        response.getWriter().write(
                                "{\"status\":\"error\",\"message\":\"Nâng cấp thất bại\"}"
                        );
                    }
                } else {
                    response.getWriter().write(
                            "{\"status\":\"error\",\"message\":\"" + eligibility.get("message") + "\"}"
                    );
                }

            } else if ("lock".equals(action)) {
                int userId = Integer.parseInt(request.getParameter("userId"));

                boolean success = userDao.lockUserAccount(userId);

                if (success) {
                    response.getWriter().write("{\"status\":\"success\",\"message\":\"Đã khóa tài khoản\"}");
                } else {
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Khóa tài khoản thất bại\"}");
                }

            } else if ("unlock".equals(action)) {
                int userId = Integer.parseInt(request.getParameter("userId"));
                boolean success = userDao.unlockUserAccount(userId);
                if (success) {
                    response.getWriter().write("{\"status\":\"success\",\"message\":\"Đã mở khóa tài khoản\"}");
                } else {
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Mở khóa thất bại\"}");
                }
            } else if ("syncSpent".equals(action)) {
                String syncType = request.getParameter("syncType");

                if ("all".equals(syncType)) {
                    int updated = userDao.syncAllUsersTotalSpent();
                    response.getWriter().write(
                            "{\"status\":\"success\",\"message\":\"Đã cập nhật " + updated + " users\",\"count\":" + updated + "}"
                    );
                } else if ("one".equals(syncType)) {
                    int userId = Integer.parseInt(request.getParameter("userId"));
                    boolean success = userDao.syncTotalSpentFromOrders(userId);

                    if (success) {
                        response.getWriter().write(
                                "{\"status\":\"success\",\"message\":\"Đã cập nhật tổng chi tiêu\"}"
                        );
                    } else {
                        response.getWriter().write(
                                "{\"status\":\"error\",\"message\":\"Không thể cập nhật\"}"
                        );
                    }
                } else {
                    response.getWriter().write(
                            "{\"status\":\"error\",\"message\":\"syncType không hợp lệ\"}"
                    );
                }
            } else if ("check-violation".equals(action)) {
                handleCheckViolation(request, response);
            } else {
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Action không hợp lệ\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Lỗi xử lý: " + e.getMessage() + "\"}");
        }
    }


    private void handleCheckViolation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));

            UserDao userDao = UserDao.getInstance();
            Map<String, Object> violationInfo = userDao.checkUserViolation(userId);

            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(violationInfo));

        } catch (Exception e) {
            response.getWriter().write("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}