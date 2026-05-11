package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/sortVoucher")
public class SortVoucherServlet extends HttpServlet {

    private VoucherDao voucherDao;

    @Override
    public void init() {
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

       String status = request.getParameter("statusFilter");
       System.out.println(status);
        List<Voucher> allVouchers = switch (status) {
            case "out" -> voucherDao.getOutVouchers();
            case "running" -> voucherDao.getRunningVouchers();
            case "expired" -> voucherDao.getExpredVouchers();
            case null, default -> voucherDao.getAllVouchers();
        };


        request.setAttribute("allVouchers", allVouchers);
        request.getRequestDispatcher("/frontend/admin/promotion.jsp")
                .forward(request, response);
    }
}
