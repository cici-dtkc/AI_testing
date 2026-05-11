package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/search-voucher")
public class searchVoucher extends HttpServlet {
    private VoucherDao voucherDao;

    @Override
    public void init() {
        voucherDao = new VoucherDao();
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("idVoucher");
        List<Voucher> allVouchers= new ArrayList<>();

        allVouchers = voucherDao.searchVoucherById(id);

        request.setAttribute("idVoucher", id);
        request.setAttribute("size_voucher", allVouchers.size());

        request.setAttribute("allVouchers", allVouchers);
        request.getRequestDispatcher("/frontend/admin/promotion.jsp")
                .forward(request, response);
    }

}