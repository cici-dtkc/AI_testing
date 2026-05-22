package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.EmailUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.PasswordUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ValidationUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ReCaptchaUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "RegisterServlet", value = "/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.getRequestDispatcher("/frontend/public/Register.jsp").forward(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        if (!ReCaptchaUtils.verify(gRecaptchaResponse)) {
            forwardWithError(request, response, "Vui lòng xác minh CAPTCHA chính xác!");
            return;
        }

        String username        = request.getParameter("username");
        String email           = request.getParameter("email");
        String phone           = request.getParameter("phone");
        String password        = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        if (!ValidationUtils.isValidUsername(username)) {
            forwardWithError(request, response,
                    "Tên đăng nhập chỉ gồm chữ, số, dấu gạch dưới (3–30 ký tự)!");
            return;
        }
        if (ValidationUtils.isBlank(email)) {
            forwardWithError(request, response, "Vui lòng nhập email!");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            forwardWithError(request, response, "Email không đúng định dạng!");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            forwardWithError(request, response,
                    "Mật khẩu phải ít nhất 8 ký tự, gồm chữ hoa, chữ thường và ký tự đặc biệt!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            forwardWithError(request, response, "Mật khẩu xác nhận không khớp!");
            return;
        }
        Optional<User> existingUserByUsername = userDao.findByUsername(username);
        if (existingUserByUsername.isPresent() && existingUserByUsername.get().isActive()) {
            forwardWithError(request, response, "Tên đăng nhập đã tồn tại!");
            return;
        }
        Optional<User> existingUserByEmail = userDao.findByEmail(email);
        if (existingUserByEmail.isPresent() && existingUserByEmail.get().isActive()) {
            forwardWithError(request, response, "Email đã được sử dụng!");
            return;
        }
        userDao.deleteInactiveUser(username, email);

//        if (!ValidationUtils.isBlank(email) && userDao.findByEmail(email).isPresent()) {
//            forwardWithError(request, response, "Email đã được sử dụng!");
//            return;
//        }
        String passwordHash = PasswordUtils.hashPassword(password);
        User user = new User();
        user.setUsername(username);
        user.setEmail(ValidationUtils.isBlank(email) ? null : email);
        user.setPhone(null);
        user.setPasswordHash(passwordHash);
        user.setFullName(username);
        userDao.insertInactive(user);

        String code = String.valueOf((int)(Math.random()*900000)+100000);
        request.getSession().setAttribute("otp_register",code);
        request.getSession().setAttribute("otpEmail_register", email);
        EmailUtils.sendEmail(email, "Mã xác thực đăng ký tài khoản", "Mã OTP kích hoạt tài khoản của bạn là: " + code);
        request.getRequestDispatcher("/frontend/public/VerifyRegister.jsp").forward(request, response);    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/frontend/public/Register.jsp").forward(request, response);
    }
}