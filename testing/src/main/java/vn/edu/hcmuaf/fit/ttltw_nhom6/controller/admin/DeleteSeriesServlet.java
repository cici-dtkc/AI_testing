package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;

import java.io.IOException;

@WebServlet(name = "DeleteSeriesServlet", urlPatterns = {"/admin/delete-series", "/DeleteSeriesServlet"})
public class DeleteSeriesServlet extends HttpServlet {

    private SeriesDAO seriesDAO;

    @Override
    public void init() throws ServletException {
        seriesDAO = new SeriesDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!RoleUtil.canManage(currentUser, "manage_series")) {
            request.getSession().setAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này.");
            response.sendRedirect(request.getContextPath() + "/admin/SeriesManagement");
            return;
        }

        try {
            String seriesIdParam = request.getParameter("id");

            if (seriesIdParam == null || seriesIdParam.trim().isEmpty()) {
                System.err.println("Missing series ID");
                request.getSession().setAttribute("errorMessage", "Thiếu ID series!");
                response.sendRedirect(request.getContextPath() + "/admin/SeriesManagement");
                return;
            }

            int seriesId = Integer.parseInt(seriesIdParam);

            String seriesName = seriesDAO.getSeriesNameById1(seriesId);

            boolean success = seriesDAO.deleteSeries(seriesId);

            if (success) {
                String message = "Xóa series";
                if (seriesName != null) {
                    message += " '" + seriesName + "'";
                }
                message += " thành công!";
                request.getSession().setAttribute("successMessage", message);
            } else {
                request.getSession().setAttribute("errorMessage", "Xóa series thất bại!");
            }

        } catch (NumberFormatException e) {
            request.getSession().setAttribute("errorMessage", "ID series không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("errorMessage", "Có lỗi xảy ra khi xóa series: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/SeriesManagement");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}