package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@WebServlet("/admin/editVoucher")
public class editVoucher extends HttpServlet {
    private VoucherDao voucherDao;
    @Override
    public void init() throws ServletException {
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String code = request.getParameter("code");
        BigDecimal minOrder = new BigDecimal(request.getParameter("minOrder"));
        int quantity = Integer.parseInt(request.getParameter("quantity"));

        LocalDateTime startDate =LocalDateTime.parse(request.getParameter("startDate"));
        LocalDateTime endDate =LocalDateTime.parse(request.getParameter("endDate"));

        boolean isSingleUse = request.getParameter("is_single_use") != null;

        boolean isUpdate = voucherDao.updateVoucher(code, minOrder, quantity,startDate, endDate, isSingleUse);
        request.setAttribute("allVouchers", voucherDao.getAllVouchers());
        if(isUpdate){
            request.setAttribute("message", "Đã chỉnh sửa thành công");
        }else{
            request.setAttribute("message", "Đã chỉnh sửa thất bại");
        }
        request.getRequestDispatcher("/frontend/admin/promotion.jsp")
                .forward(request, response);
    }

}