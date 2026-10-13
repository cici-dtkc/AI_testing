package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.StatisticsDAO;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/dashboard")
public class DashBoardServlet extends HttpServlet {
    private StatisticsDAO statisticsDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        statisticsDAO = new StatisticsDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("filter".equals(action)) {
            handleFilter(request, response);
        } else if ("chartData".equals(action)) {
            handleChartData(request, response);
        } else {
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            displayDashboard(request, response, "month", monthStart, monthEnd);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void displayDashboard(HttpServletRequest request, HttpServletResponse response,
                                  String period, LocalDate startDate, LocalDate endDate)
            throws ServletException, IOException {

        Map<String, Object> stats = statisticsDAO.getCustomStats(startDate, endDate);

        List<Map<String, Object>> topProducts =
                statisticsDAO.getTopSellingComics(startDate, endDate, 10);

        List<Map<String, Object>> topRatedComics =
                statisticsDAO.getTopRatedComics(10);

        List<Map<String, Object>> chartData = getChartData(period, startDate, endDate);

        request.setAttribute("stats", stats);
        request.setAttribute("topProducts", topProducts);
        request.setAttribute("topRatedComics", topRatedComics);
        request.setAttribute("chartData", gson.toJson(chartData));
        request.setAttribute("chartType", getChartType(period));
        request.setAttribute("currentPeriod", period);

        request.getRequestDispatcher("/frontend/admin/dashboard.jsp")
                .forward(request, response);
    }


    private void handleFilter(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String period = request.getParameter("period");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        LocalDate startDate;
        LocalDate endDate;

        if ("custom".equals(period) && startDateStr != null && endDateStr != null) {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);

            request.setAttribute("startDate", startDateStr);
            request.setAttribute("endDate", endDateStr);
        } else {
            LocalDate today = LocalDate.now();
            switch (period) {
                case "today":
                    startDate = today;
                    endDate = today;
                    break;
                case "week":
                    startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    endDate = startDate.plusDays(6);
                    break;
                case "year":
                    startDate = today.withDayOfYear(1);
                    endDate = today.withDayOfYear(today.lengthOfYear());
                    break;
                default:
                    startDate = today.withDayOfMonth(1);
                    endDate = today.withDayOfMonth(today.lengthOfMonth());
                    break;
            }
        }
        displayDashboard(request, response, period, startDate, endDate);
    }

    private List<Map<String, Object>> getChartData(String period, LocalDate startDate, LocalDate endDate) {
        switch (period) {
            case "today":
                return statisticsDAO.getDailyRevenue(startDate, endDate);
            case "week":
            case "custom":
                return statisticsDAO.getDailyRevenue(startDate, endDate);
            case "month":
                return statisticsDAO.getDailyRevenue(startDate, endDate);
            case "year":
                return statisticsDAO.getRevenueByMonth(startDate.getYear());
            default:
                return statisticsDAO.getRevenueByMonth(Year.now().getValue());
        }
    }

    private String getChartType(String period) {
        switch (period) {
            case "today":
            case "week":
            case "custom":
            case "month":
                return "daily";
            case "year":
                return "monthly";
            default:
                return "monthly";
        }
    }


    private void handleChartData(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String chartType = request.getParameter("type");
        int year = request.getParameter("year") != null ?
                Integer.parseInt(request.getParameter("year")) :
                Year.now().getValue();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("monthly".equals(chartType)) {
            List<Map<String, Object>> data = statisticsDAO.getRevenueByMonth(year);
            response.getWriter().write(gson.toJson(data));
        }
    }
}