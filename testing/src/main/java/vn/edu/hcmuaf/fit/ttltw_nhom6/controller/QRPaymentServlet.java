package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet("/momo-qr")
public class QRPaymentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("amount",  req.getParameter("amount"));
        req.setAttribute("orderId", req.getParameter("orderId"));
        req.setAttribute("qrUrl",   req.getParameter("qr"));
        req.setAttribute("info",    req.getParameter("info"));
        req.getRequestDispatcher("/frontend/nguoiB/qr-payment.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}