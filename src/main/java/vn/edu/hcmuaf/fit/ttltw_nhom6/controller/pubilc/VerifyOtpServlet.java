package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.OtpToken;

import java.io.IOException;

@WebServlet(name = "VerifyOtpServlet", value = "/VerifyOtpServlet")
public class VerifyOtpServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        OtpToken otpToken = (OtpToken) request.getSession().getAttribute("otpToken");
        String email = (String) request.getSession().getAttribute("otpEmail");

        if (otpToken != null && otpToken.isValid(code, email)) {
            // OTP đúng -> lưu otp vào session để ResetPassServlet làm bằng chứng xác thực thành công
            request.getSession().setAttribute("otp", otpToken.getCode());
            request.getSession().removeAttribute("otpSent");
            request.getSession().removeAttribute("otpError");
            // Xóa otpToken để tránh sử dụng lại mã
            request.getSession().removeAttribute("otpToken");

            // Chuyển sang trang tạo mật khẩu
            response.sendRedirect(request.getContextPath() + "/frontend/public/reset-password.jsp");
        } else {
            // OTP sai hoặc hết hạn -> giữ otpSent và báo lỗi phù hợp
            String errorMsg = "Mã xác thực không đúng!";
            if (otpToken != null && otpToken.isExpired()) {
                errorMsg = "Mã xác thực đã hết hạn!";
            }
            request.getSession().setAttribute("otpSent", true);
            request.getSession().setAttribute("otpError", errorMsg);

            // dùng forward để giữ popup hiển thị
            request.getRequestDispatcher("/frontend/public/ForgotPass.jsp").forward(request, response);
        }
    }
}