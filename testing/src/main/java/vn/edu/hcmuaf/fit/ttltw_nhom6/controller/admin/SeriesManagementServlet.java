package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Series;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/SeriesManagement")
public class SeriesManagementServlet extends HttpServlet {

    private SeriesDAO seriesDAO;
    private static final int PAGE_SIZE = 5;

    @Override
    public void init() throws ServletException {
        seriesDAO = new SeriesDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String pageParam = request.getParameter("page");
            int currentPage = 1;
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                try {
                    currentPage = Integer.parseInt(pageParam);
                    if (currentPage < 1) currentPage = 1;
                } catch (NumberFormatException e) {
                    currentPage = 1;
                }
            }

            String keyword = request.getParameter("keyword");
            if (keyword != null) {
                keyword = keyword.trim();
                if (keyword.isEmpty()) {
                    keyword = null;
                }
            }

            String filterParam = request.getParameter("filter");
            if (filterParam == null || filterParam.trim().isEmpty()) {
                filterParam = "all";
            }

            Boolean isHidden = null;

            if ("visible".equals(filterParam)) {
                isHidden = false;  // is_hidden = 0
            } else if ("hidden".equals(filterParam)) {
                isHidden = true;   // is_hidden = 1
            }

            List<Series> seriesList;
            int totalSeries;

            if (keyword != null && !keyword.isEmpty()) {
                seriesList = seriesDAO.searchSeriesByNameAndVisibility(keyword, isHidden);
                totalSeries = seriesList.size();

                int startIndex = (currentPage - 1) * PAGE_SIZE;
                int endIndex = Math.min(startIndex + PAGE_SIZE, totalSeries);

                if (startIndex < totalSeries && startIndex >= 0) {
                    seriesList = new ArrayList<>(seriesList.subList(startIndex, endIndex));
                } else {
                    seriesList = new ArrayList<>();
                }
            } else {
                seriesList = seriesDAO.getSeriesByVisibility(currentPage, PAGE_SIZE, isHidden);
                totalSeries = seriesDAO.countSeriesByVisibility(isHidden);

            }

            int totalPages = (int) Math.ceil((double) totalSeries / PAGE_SIZE);
            if (totalPages == 0) totalPages = 1;

            if (currentPage > totalPages) {
                currentPage = totalPages;
            }


            String successMessage = (String) request.getSession().getAttribute("successMessage");
            String errorMessage = (String) request.getSession().getAttribute("errorMessage");

            if (successMessage != null) {
                request.setAttribute("successMessage", successMessage);
                request.getSession().removeAttribute("successMessage");
            }

            if (errorMessage != null) {
                request.setAttribute("errorMessage", errorMessage);
                request.getSession().removeAttribute("errorMessage");
            }

            request.setAttribute("seriesList", seriesList);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("keyword", keyword);
            request.setAttribute("filter", filterParam);

            request.getRequestDispatcher("/frontend/admin/seriesManagement.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();

            request.setAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            request.setAttribute("seriesList", new ArrayList<Series>());
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 1);
            request.setAttribute("filter", "all");

        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            String action = request.getParameter("action");
            String idParam = request.getParameter("id");


            if (idParam == null || idParam.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"error\": \"Missing ID\"}");
                return;
            }

            if (action == null || action.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"error\": \"Missing action\"}");
                return;
            }

            int seriesId = Integer.parseInt(idParam);

            boolean hidden = "hide".equals(action);
            boolean success = seriesDAO.updateSeriesVisibility(seriesId, hidden);

            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"success\": true}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"success\": false, \"error\": \"Update failed\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"error\": \"Invalid ID format\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }
}

