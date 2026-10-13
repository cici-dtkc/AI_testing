package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/voucher")
public class VoucherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            VoucherDao dao = new VoucherDao();
            List<Voucher> vouchers = dao.getAllActiveVouchers();

            List<Voucher> productVouchers = new ArrayList<>();
            List<Voucher> shippingVouchers = new ArrayList<>();

            for (Voucher v : vouchers) {
                if ("Vận chuyển".equals(v.getDiscountTarget())) {
                    shippingVouchers.add(v);
                } else {
                    productVouchers.add(v);
                }
            }
            request.setAttribute("productVouchers", productVouchers);
            request.setAttribute("shippingVouchers", shippingVouchers);

        } catch (Exception e) {
            e.printStackTrace();
        }
        request.getRequestDispatcher("/frontend/public/getVoucher.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}