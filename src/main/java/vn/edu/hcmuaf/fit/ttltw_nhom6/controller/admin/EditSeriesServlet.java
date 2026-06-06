
package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Series;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CloudinaryService;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "EditSeriesServlet", urlPatterns = {"/EditSeriesServlet", "/admin/edit-series"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
        maxFileSize = 1024 * 1024 * 10,       // 10MB
        maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class EditSeriesServlet extends HttpServlet {

    private SeriesDAO seriesDAO;

    @Override
    public void init() throws ServletException {
        seriesDAO = new SeriesDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/SeriesManagement");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
            String seriesIdParam = request.getParameter("seriesId");

            if (seriesIdParam == null || seriesIdParam.trim().isEmpty()) {
                request.getSession().setAttribute("errorMessage", "Thiếu ID series!");
                response.sendRedirect(request.getContextPath() + "/SeriesManagement");
                return;
            }

            int seriesId = Integer.parseInt(seriesIdParam);

            Optional<Series> optionalSeries = seriesDAO.getSeriesById(seriesId);

            if (!optionalSeries.isPresent()) {
                request.getSession().setAttribute("errorMessage", "Không tìm thấy series!");
                response.sendRedirect(request.getContextPath() + "/SeriesManagement");
                return;
            }

            Series series = optionalSeries.get();

            String seriesName = request.getParameter("seriesName");
            String description = request.getParameter("seriesDescription");
            String status = request.getParameter("seriesStatus");
            String volumesParam = request.getParameter("seriesVolumes");

            if (seriesName == null || seriesName.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên series không được để trống!");
            }

            if (volumesParam == null || volumesParam.trim().isEmpty()) {
                throw new IllegalArgumentException("Số tập không được để trống!");
            }

            int totalVolumes = Integer.parseInt(volumesParam);

            if (totalVolumes <= 0) {
                throw new IllegalArgumentException("Số tập phải lớn hơn 0!");
            }

            if (seriesDAO.isSeriesNameExistsExcludingId(seriesName, seriesId)) {
                request.getSession().setAttribute("errorMessage", "Tên series '" + seriesName + "' đã tồn tại!");
                response.sendRedirect(request.getContextPath() + "/SeriesManagement");
                return;
            }

            series.setSeriesName(seriesName);
            series.setDescription(description);
            series.setStatus(status);
            series.setTotalVolumes(totalVolumes);

            Part coverPart = request.getPart("seriesCover");

            if (coverPart != null && coverPart.getSize() > 0) {
                try {
                    String oldCoverUrl = series.getCoverUrl();
                    if (oldCoverUrl != null && !oldCoverUrl.isEmpty()) {
                        String publicId = CloudinaryService.getPublicIdFromUrl(oldCoverUrl);
                        if (publicId != null) {
                            boolean deleted = CloudinaryService.deleteImage(publicId);
                            if (deleted) {
                                System.out.println("Deleted old cover: " + publicId);
                            } else {
                                System.out.println("Failed to delete old cover: " + publicId);
                            }
                        }
                    }

                    String newCoverUrl = CloudinaryService.uploadImage(coverPart, "comics/series");
                    series.setCoverUrl(newCoverUrl);

                } catch (IOException e) {
                    e.printStackTrace();
                    request.getSession().setAttribute("errorMessage", "Lỗi khi upload ảnh mới: " + e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/SeriesManagement");
                    return;
                }
            }

            boolean success = seriesDAO.updateSeries(series);

            if (success) {
                request.getSession().setAttribute("successMessage", "Cập nhật series thành công!");
            } else {
                request.getSession().setAttribute("errorMessage", "Cập nhật series thất bại!");
            }

        } catch (NumberFormatException e) {
            request.getSession().setAttribute("errorMessage", "Số tập phải là số nguyên hợp lệ!");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/SeriesManagement");
    }
}