package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ACLChecker;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.RoleHierarchyService;

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
            String statusFilter = request.getParameter("status");
            String expenditure = request.getParameter("expenditure");
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

            if (statusFilter != null && !statusFilter.trim().isEmpty()) {
                if ("locked".equals(statusFilter)) {
                    users = users.stream()
                            .filter(u -> !u.isActive() ||
                                    (u.getLockedUntil() != null && u.getLockedUntil().isAfter(java.time.LocalDateTime.now())))
                            .collect(java.util.stream.Collectors.toList());
                } else if ("active".equals(statusFilter)) {
                    users = users.stream()
                            .filter(u -> u.isActive() &&
                                    (u.getLockedUntil() == null || u.getLockedUntil().isBefore(java.time.LocalDateTime.now())))
                            .collect(java.util.stream.Collectors.toList());
                }
            }


            if(expenditure != null && !expenditure.trim().isEmpty()) {
                System.out.println("hello");
                if("high-low".equals(expenditure)) {
                    users = users.stream().
                            sorted(java.util.Comparator.comparing(User::getTotalSpent).reversed())
                            .collect(java.util.stream.Collectors.toList());
                }else if("low-high".equals(expenditure)){
                    users = users.stream().
                            sorted(java.util.Comparator.comparing(User::getTotalSpent))
                            .collect(java.util.stream.Collectors.toList());
                }
            }

            User currentUser = (User) request.getSession().getAttribute("currentUser");
            Role currentRole = new RoleDAO().getRoleById(currentUser.getRoleId());
            boolean isAdminUser = "admin_user".equalsIgnoreCase(currentRole.getRoleName()) || RoleHierarchyService.isSuperAdmin(currentRole);
            request.setAttribute("canLock", isAdminUser || ACLChecker.hasPermission(currentUser, "LOCK_USER"));
            request.setAttribute("canUnlock", isAdminUser || ACLChecker.hasPermission(currentUser, "UNLOCK_USER"));
            request.setAttribute("canUpgrade", isAdminUser || ACLChecker.hasPermission(currentUser, "UPGRADE_USER"));


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