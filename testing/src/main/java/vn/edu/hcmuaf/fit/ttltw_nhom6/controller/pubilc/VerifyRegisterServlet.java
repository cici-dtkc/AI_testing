package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.OtpToken;

import java.io.IOException;

@WebServlet(name = "VerifyRegisterServlet", value = "/VerifyRegisterServlet")
public class VerifyRegisterServlet extends HttpServlet {
    private UserDao userDao;

    @Override
    public void init() throws ServletException {
        userDao = new UserDao(JdbiConnector.get());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/frontend/public/VerifyRegister.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputOtp = request.getParameter("otp");
        HttpSession session = request.getSession();
        OtpToken otpToken = (OtpToken) session.getAttribute("otpToken_register");
        String email = (String) session.getAttribute("otpEmail_register");

        if (otpToken != null && otpToken.isValid(inputOtp, email)) {
            userDao.activateUserByEmail(email);
            session.removeAttribute("otpToken_register");
            session.removeAttribute("otpEmail_register");
            request.setAttribute("success", "Tài khoản của bạn đã được kích hoạt thành công!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
        } else {
            String errorMsg = "Mã OTP không hợp lệ hoặc đã hết hạn!";
            if (otpToken != null && otpToken.isExpired()) {
                errorMsg = "Mã OTP đã hết hạn!";
            }
            request.setAttribute("error", errorMsg);
            request.getRequestDispatcher("/frontend/public/VerifyRegister.jsp").forward(request, response);
        }
    }
}
