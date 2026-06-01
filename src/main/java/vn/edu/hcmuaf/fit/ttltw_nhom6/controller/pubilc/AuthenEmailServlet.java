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
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ReCaptchaUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.OtpGenerator;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.OtpToken;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "AuthenEmailServlet", value = "/AuthenEmailServlet")
public class AuthenEmailServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        if (!ReCaptchaUtils.verify(gRecaptchaResponse)) {
            request.setAttribute("error", "Vui lòng xác minh CAPTCHA chính xác!");
            request.getRequestDispatcher("/frontend/public/ForgotPass.jsp").forward(request, response);
            return;
        }
            String email = request.getParameter("email");

        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            request.setAttribute("error", "Email không tồn tại trong hệ thống!");
            request.getRequestDispatcher("/frontend/public/ForgotPass.jsp").forward(request, response);
            return;
        }

        OtpToken otpToken = OtpGenerator.generateOtp(email, 5);
        request.getSession().setAttribute("otpToken", otpToken);
        request.getSession().setAttribute("otpEmail", email);
        request.getSession().setAttribute("otpSent", true); 
        EmailUtils.sendEmail(email, "Mã xác thực quên mật khẩu", "Mã OTP của bạn là: " + otpToken.getCode());
        request.getRequestDispatcher("/frontend/public/ForgotPass.jsp").forward(request, response);
    }
}