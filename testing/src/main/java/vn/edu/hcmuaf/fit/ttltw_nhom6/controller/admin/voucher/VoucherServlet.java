package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/admin/vouchers")
public class VoucherServlet extends HttpServlet {

    private VoucherDao voucherDao;

    public void init() throws ServletException {
        voucherDao  = new VoucherDao();

    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String code = request.getParameter("code");
        String target = request.getParameter("discount_target");
        String type = request.getParameter("discount_type");
        BigDecimal value = new BigDecimal(request.getParameter("discount_value"));
        BigDecimal min_order_amount = new BigDecimal(request.getParameter("min_order_amount"));
        int quantity = Integer.parseInt(request.getParameter("quantity"));
        String scope =   request.getParameter("apply_scope");

        boolean is_single_use =request.getParameter("is_single_use") != null;
        LocalDateTime start_date =LocalDateTime.parse(request.getParameter("start_date"));
        LocalDateTime end_date =LocalDateTime.parse(request.getParameter("end_date"));

        int used_count =0;

        String category = request.getParameter("cate");
        if (!scope.equalsIgnoreCase("Tất cả")) {
            scope = category;
        }

        if(voucherDao.isExistCode(code)){
            request.setAttribute("message","Mã này đã tồn tại!");
            List<Voucher> allVouchers = voucherDao.getAllVouchers();
            request.setAttribute("allVouchers", allVouchers);
            request.getRequestDispatcher("/frontend/admin/promotion.jsp").forward(request, response);
           return;
        }

        Voucher voucher = new Voucher(code, type, used_count, value, target,  scope, quantity,
                                        start_date, end_date, min_order_amount,is_single_use);

        if(voucherDao.addVoucher(voucher)){
            NotificationDAO notificationDAO = NotificationDAO.getInstance();

            String startDateFormatted = voucher.getStartDate() != null
                    ? voucher.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "N/A";

            String endDateFormatted = voucher.getEndDate() != null
                    ? voucher.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "N/A";

            boolean isPercent = voucher.getDiscountType() != null
                    && voucher.getDiscountType().equalsIgnoreCase("PERCENT");

            String message = "Mã giảm giá mới: " + voucher.getCode() + "\n"
                    + "Giảm " + voucher.getDiscountValue() + (isPercent ? "%" : "đ") + "\n"
                    + "Hiệu lực: " + startDateFormatted + " — " + endDateFormatted;

            notificationDAO.insertForAllUsers(message, "VOUCHER_NEW");

            request.setAttribute("message","Tạo thành công!");
            request.setAttribute("allVouchers", voucherDao.getAllVouchers());
            VoucherDisplay.setPermissionAttributes(request);
            request.getRequestDispatcher("/frontend/admin/promotion.jsp").forward(request, response);
        }

    }
}