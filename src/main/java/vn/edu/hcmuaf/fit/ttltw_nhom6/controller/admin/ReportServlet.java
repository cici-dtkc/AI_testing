package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ReportDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ReportServlet", urlPatterns = {"/admin/ReportManagement", "/admin/report-data"})
public class ReportServlet extends HttpServlet {
    private ReportDAO reportDAO;
    private FlashSaleDAO flashSaleDAO;
    private Gson gson;
    private VoucherDao voucherDao;

    @Override
    public void init() throws ServletException {
        super.init();
        reportDAO = new ReportDAO();
        flashSaleDAO = new FlashSaleDAO();
        voucherDao = new VoucherDao();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/admin/ReportManagement".equals(path)) {
            request.setAttribute("comicSold", reportDAO.comicSold());
            request.getRequestDispatcher("/frontend/admin/report.jsp").forward(request, response);
            return;
        }

        if ("/admin/report-data".equals(path)) {
            handleReportData(request, response);
            return;
        }
    }


    private void handleReportData(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        String filter = request.getParameter("filter");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        LocalDate startDate;
        LocalDate endDate;

        if ("custom".equals(filter) && startDateStr != null && endDateStr != null) {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } else if ("week".equals(filter)) {
            startDate = LocalDate.now().minusDays(6);
            endDate = LocalDate.now();
        } else if ("month".equals(filter)) {
            startDate = LocalDate.now().minusDays(29);
            endDate = LocalDate.now();
        }
        else if ("year".equals(filter)) {
            startDate = LocalDate.now().withDayOfYear(1);
            endDate = LocalDate.now();
        } else {
            startDate = LocalDate.now();
            endDate = LocalDate.now();
        }

        try {
            Map<String, Object> result = new HashMap<>();

            Map<String, Object> kpi = reportDAO.getOverviewStats(startDate, endDate);
            result.put("kpi", kpi);

            List<Map<String, Object>> topProducts = reportDAO.getTopSellingProducts(startDate, endDate, 3);
            result.put("topProducts", topProducts);

            List<Map<String, Object>> dailyData;
            boolean isYearFilter = "year".equals(filter);

            if (isYearFilter) {
                dailyData = reportDAO.getMonthlyRevenue(startDate, endDate);
            } else {
                dailyData = reportDAO.getDailyRevenue(startDate, endDate);
            }


            Map<String, Object> chartData = new HashMap<>();

            List<String> labels = new ArrayList<>();
            List<Double> revenueData = new ArrayList<>();
            List<Integer> ordersData = new ArrayList<>();
            List<Double> avgValueData = new ArrayList<>();

            for (Map<String, Object> day : dailyData) {
                String dateStr;
                if (isYearFilter) {
                    // Hiện "Tháng 1", "Tháng 2"...
                    int month = ((Number) day.get("month")).intValue();
                    dateStr = "Tháng " + month;
                } else {
                    dateStr = day.get("date").toString();
                }
                labels.add(dateStr);

                Double revenue = day.get("revenue") != null ?
                        ((Number) day.get("revenue")).doubleValue() : 0.0;
                Integer orderCount = day.get("order_count") != null ?
                        ((Number) day.get("order_count")).intValue() : 0;

                revenueData.add(revenue);
                ordersData.add(orderCount);
                avgValueData.add(orderCount > 0 ? revenue / orderCount : 0.0);

            }

            Map<String, Object> revenueChart = new HashMap<>();
            revenueChart.put("labels", labels);
            revenueChart.put("data", revenueData);

            Map<String, Object> ordersChart = new HashMap<>();
            ordersChart.put("labels", labels);
            ordersChart.put("data", ordersData);

            Map<String, Object> avgValueChart = new HashMap<>();
            avgValueChart.put("labels", labels);
            avgValueChart.put("data", avgValueData);

            chartData.put("revenue", revenueChart);
            chartData.put("orders", ordersChart);
            chartData.put("avgValue", avgValueChart);

            result.put("chartData", chartData);

            List<Map<String, Object>> flashSaleStats = reportDAO.getFlashSaleTopRevenue(5);
            result.put("flashSales", flashSaleStats);

            result.put("topVouchers", voucherDao.get5TheMostUsedVouchersAsMap(startDate, endDate));
            result.put("allVouchers", voucherDao.getAllVouchersAsMap(startDate, endDate));
            String jsonResponse = gson.toJson(result);

            response.getWriter().write(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("message", e.getMessage());

            response.getWriter().write(gson.toJson(errorResult));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}