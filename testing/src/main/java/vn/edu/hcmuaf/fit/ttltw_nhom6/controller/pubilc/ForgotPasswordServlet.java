package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "ForgotPasswordServlet", value = "/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // XÓA tất cả attributes liên quan đến OTP
        request.getSession().removeAttribute("otpSent");
        request.getSession().removeAttribute("otpError"); // THÊM DÒNG NÀY
        request.getSession().removeAttribute("otp");
        request.getSession().removeAttribute("otpEmail");

        // Forward đến trang ForgotPass.jsp
        request.getRequestDispatcher("/frontend/public/ForgotPass.jsp").forward(request, response);
    }
}