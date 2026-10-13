package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.PasswordUtils;

import java.io.IOException;
@WebServlet(name = "ResetPassServlet", value = "/ResetPassServlet")
public class ResetPassServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");

        String sessionCode = (String) request.getSession().getAttribute("otp");
        String sessionEmail = (String) request.getSession().getAttribute("otpEmail");

        if (sessionCode == null || sessionEmail == null) {
            request.setAttribute("error", "Bạn chưa có mã xác thực!");
            request.getRequestDispatcher("/frontend/public/reset-password.jsp").forward(request, response);
            return;
        }

        // Kiểm tra mật khẩu khớp
        if (!oldPassword.equals(newPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp!");
            request.getRequestDispatcher("/frontend/public/reset-password.jsp").forward(request, response);
            return;
        }

        // Kiểm tra format mật khẩu
        if (!PasswordUtils.isValidPasswordFormat(newPassword)) {
            request.setAttribute("error", "Mật khẩu phải ít nhất 8 ký tự, chữ hoa, thường, số và ký tự đặc biệt!");
            request.getRequestDispatcher("/frontend/public/reset-password.jsp").forward(request, response);
            return;
        }

        // Hash mật khẩu mới
        String hashed = PasswordUtils.hashPassword(newPassword);

        // Update DB
        userDao.updatePassword(sessionEmail, hashed);

        // Xóa OTP khỏi session
        request.getSession().removeAttribute("otp");
        request.getSession().removeAttribute("otpEmail");

        // CHỈ SET success = true KHI ĐÃ UPDATE THÀNH CÔNG
        request.setAttribute("success", true);
        request.getRequestDispatcher("/frontend/public/reset-password.jsp").forward(request, response);
    }

//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//        request.getRequestDispatcher("/frontend/public/Register.jsp").forward(request, response);
//    }

}