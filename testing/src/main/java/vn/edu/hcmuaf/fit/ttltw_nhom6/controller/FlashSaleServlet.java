package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.FlashSaleService;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@WebServlet("/flash-sale")
public class FlashSaleServlet extends HttpServlet {

    private final FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
    private final FlashSaleComicsDAO flashSaleComicsDAO = new FlashSaleComicsDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            flashSaleDAO.updateStatuses();
            FlashSaleService flashSaleService = new FlashSaleService();
            FlashSale activeFlashSale = flashSaleDAO.getActiveFlashSaleEndingSoon();

            if (activeFlashSale != null) {
                req.setAttribute("activeFlashSale", activeFlashSale);

                long endTimeMillis = activeFlashSale.getEndTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                req.setAttribute("flashSaleEndTimeMillis", endTimeMillis);

                List<Map<String, Object>> activeComics =
                        flashSaleService.getExclusiveComicsForFlashSale(activeFlashSale.getId());
                req.setAttribute("activeComics", activeComics);

            }

            List<FlashSale> upcomingFlashSales = flashSaleDAO.getUpcomingAndActiveFlashSales();
            req.setAttribute("upcomingFlashSales", upcomingFlashSales);

            req.getRequestDispatcher("/frontend/public/FlashSale.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Lỗi khi tải Flash Sale: " + e.getMessage());
            req.getRequestDispatcher("/Error/error404.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}