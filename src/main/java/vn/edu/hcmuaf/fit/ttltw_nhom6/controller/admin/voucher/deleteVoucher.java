package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;

@WebServlet("/admin/deleteVoucher")
public class deleteVoucher extends HttpServlet {
    VoucherDao voucherDao = new VoucherDao();

    @Override
    public void init() throws ServletException {
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String code = request.getParameter("deleted-code");
        voucherDao.deleteByCode(code);

        request.setAttribute("message", "Đã xóa thành công!");
        request.setAttribute("allVouchers", voucherDao.getAllVouchers());
        request.getRequestDispatcher("/frontend/admin/promotion.jsp").forward(request, response);

    }
}