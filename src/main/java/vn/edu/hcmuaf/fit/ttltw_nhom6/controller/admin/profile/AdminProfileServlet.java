package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.profile;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.PasswordUtils;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/profile")
public class AdminProfileServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() throws ServletException {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/frontend/admin/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        Gson gson = new Gson();
        if ("changePassword".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("currentUser") == null) {
                response.getWriter().write(gson.toJson(Map.of("success", false, "message", "Phiên đăng nhập hết hạn")));
                return;
            }
            User admin = (User) session.getAttribute("currentUser");
            String oldPassword    = request.getParameter("oldPassword");
            String newPassword    = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            if (isEmpty(oldPassword)|| isEmpty(newPassword) || isEmpty(confirmPassword)){
                response.getWriter().write(gson.toJson(Map.of("success", false, "message", "Vui lòng điền đầy đủ thông tin")));
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false, "message", "Mật khẩu mới và xác nhận không khớp"
                )));
                return;
            }
            if (!PasswordUtils.isValidPasswordFormat(newPassword)) {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false,
                        "message", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
                )));
                return;
            }
            User userFromDb = userDao.findById(admin.getId()).orElse(null);
            if (userFromDb == null) {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false, "message", "Không tìm thấy tài khoản"
                )));
                return;
            }
            if (!PasswordUtils.verifyPassword(oldPassword, userFromDb.getPasswordHash())) {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false, "message", "Mật khẩu hiện tại không đúng"
                )));
                return;
            }
            if (PasswordUtils.verifyPassword(newPassword, userFromDb.getPasswordHash())) {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false, "message", "Mật khẩu mới không được trùng mật khẩu cũ"
                )));
                return;
            }
            String newHash = PasswordUtils.hashPassword(newPassword);
            boolean ok = userDao.updatePassword(admin.getId(), newHash);

            if (ok) {
                admin.setPasswordHash(newHash);
                session.setAttribute("currentUser", admin);
                response.getWriter().write(gson.toJson(Map.of(
                        "success", true, "message", "Đổi mật khẩu thành công!"
                )));
            } else {
                response.getWriter().write(gson.toJson(Map.of(
                        "success", false, "message", "Có lỗi xảy ra, vui lòng thử lại"
                )));
            }

        }
    }

    private boolean isEmpty(String oldPassword) {
        return oldPassword == null || oldPassword.trim().isEmpty();
    }
}
