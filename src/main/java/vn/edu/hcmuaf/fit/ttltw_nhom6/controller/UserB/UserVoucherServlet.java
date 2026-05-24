package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;

@WebServlet("/user-voucher")
public class UserVoucherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            VoucherDao voucherDao = new VoucherDao();
            int userId = currentUser.getId();

            request.setAttribute("userProductVouchers",
                    voucherDao.getProductVouchersByUser(userId));
            request.setAttribute("userShippingVouchers",
                    voucherDao.getShippingVouchersByUser(userId));

        } catch (Exception e) {
            e.printStackTrace();
        }

        request.getRequestDispatcher("/frontend/nguoiB/userVoucher.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập\"}");
            return;
        }

        try {
            int promoId = Integer.parseInt(request.getParameter("promoId"));
            String code = request.getParameter("code");

            VoucherDao voucherDao = new VoucherDao();
            boolean saved = voucherDao.saveVoucherForUser(currentUser.getId(), promoId, code);

            if (saved) {
                response.getWriter().write("{\"success\":true,\"message\":\"Đã lưu voucher thành công!\"}");
            } else {
                response.getWriter().write("{\"success\":false,\"message\":\"Bạn đã lưu voucher này rồi!\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"Có lỗi xảy ra!\"}");
        }
    }
}