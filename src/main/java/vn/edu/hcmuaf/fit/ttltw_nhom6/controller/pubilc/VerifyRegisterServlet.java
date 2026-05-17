package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;

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
        String inputOtp = request.getParameter("otp");
        String sessionOtp = (String) request.getSession().getAttribute("otp_register");
        String email = (String) request.getSession().getAttribute("otpEmail_register");
    if (sessionOtp != null && sessionOtp.equals(inputOtp)){
        userDao.activateUserByEmail(email);
        request.getSession().removeAttribute("otp_register");
        request.getSession().removeAttribute("otpEmail_register");
        request.setAttribute("success","Tài khoản của bạn đã được kích hoạt thành công!");
        request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
    }else {
        request.setAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn!");
        request.getRequestDispatcher("/frontend/public/VerifyRegister.jsp").forward(request, response);
    }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
