package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.PasswordUtils;

import java.io.IOException;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Lấy thông báo từ session
        String successMessage = (String) session.getAttribute("success");
        if (successMessage != null) {
            request.setAttribute("success", successMessage);
            session.removeAttribute("success");
        }

        request.getRequestDispatcher("/frontend/nguoiB/change-password.jsp").forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        // Kiểm tra đăng nhập
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        int userId = currentUser.getId();

        // Lấy dữ liệu từ form
        String currentPassword = request.getParameter("current-password");
        String newPassword = request.getParameter("new-password");
        String confirmPassword = request.getParameter("confirm-password");

        // 1. Kiểm tra các trường bắt buộc
        if (isEmpty(currentPassword) || isEmpty(newPassword) || isEmpty(confirmPassword)) {
            setErrorAndForward(request, response, "Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // 2. Kiểm tra mật khẩu mới có khớp với xác nhận không
        if (!newPassword.equals(confirmPassword)) {
            setErrorAndForward(request, response, "Mật khẩu mới và xác nhận không khớp!");
            return;
        }

        // 3. Kiểm tra format mật khẩu mới
        if (!PasswordUtils.isValidPasswordFormat(newPassword)) {
            setErrorAndForward(request, response,
                    "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return;
        }

        // 4. Lấy thông tin user từ database (đảm bảo thông tin mới nhất)
        User userFromDb = userDao.findById(userId).orElse(null);
        if (userFromDb == null) {
            setErrorAndForward(request, response, "Không tìm thấy tài khoản. Vui lòng đăng nhập lại!");
            return;
        }

        // 5. Kiểm tra mật khẩu hiện tại có đúng không
        if (!PasswordUtils.verifyPassword(currentPassword, userFromDb.getPasswordHash())) {
            setErrorAndForward(request, response, "Mật khẩu hiện tại không đúng!");
            return;
        }

        // 6. Kiểm tra mật khẩu mới KHÔNG được trùng với mật khẩu cũ
        if (PasswordUtils.verifyPassword(newPassword, userFromDb.getPasswordHash())) {
            setErrorAndForward(request, response, "Mật khẩu mới không được trùng với mật khẩu cũ!");
            return;
        }

        // 7. Mã hóa mật khẩu mới
        String newPasswordHash = PasswordUtils.hashPassword(newPassword);

        // 8. Cập nhật vào database
        boolean success = userDao.updatePassword(userId, newPasswordHash);

        if (success) {
            currentUser.setPasswordHash(newPasswordHash);
            session.setAttribute("currentUser", currentUser);

            session.setAttribute("success", "Đổi mật khẩu thành công!");
            response.sendRedirect(request.getContextPath() + "/change-password");
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void setErrorAndForward(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/frontend/nguoiB/change-password.jsp").forward(request, response);
    }
}