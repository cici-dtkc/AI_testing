package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin.voucher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ACLChecker;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/vouchersManagement")
public class VoucherDisplay extends HttpServlet {
    private VoucherDao voucherDao;

    @Override
    public void init() throws ServletException {
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setPermissionAttributes(request);

        List<Voucher> allVouchers = voucherDao.getAllVouchers();
        request.setAttribute("allVouchers", allVouchers);
        request.getRequestDispatcher("/frontend/admin/promotion.jsp").forward(request, response);
    }

    public static void setPermissionAttributes(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("currentUser");
        request.setAttribute("canAdd",    ACLChecker.hasPermission(user, "ADD_PROMOTION"));
        request.setAttribute("canEdit",   ACLChecker.hasPermission(user, "EDIT_PROMOTION"));
        request.setAttribute("canDelete", ACLChecker.hasPermission(user, "DELETE_PROMOTION"));
    }


}