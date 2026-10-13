package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/products/get")
public class GetComicDetailServlet extends HttpServlet {

    private ComicDAO comicDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        try {
            comicDAO = new ComicDAO();
            gson = new Gson();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            String idParam = request.getParameter("id");

            if (idParam == null || idParam.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Thiếu ID truyện");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            int comicId = Integer.parseInt(idParam);
            Comic comic = comicDAO.getComicById2(comicId);

            if (comic == null) {
                System.out.println("Comic not found: " + comicId);
                result.put("success", false);
                result.put("message", "Không tìm thấy truyện");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Map<String, Object> comicData = new HashMap<>();
                comicData.put("id", comic.getId());
                comicData.put("nameComics", comic.getNameComics());
                comicData.put("description", comic.getDescription());
                comicData.put("price", comic.getPrice());
                comicData.put("stockQuantity", comic.getStockQuantity());
                comicData.put("categoryId", comic.getCategoryId());
                comicData.put("categoryName", comic.getCategoryName());
                comicData.put("seriesId", comic.getSeriesId());
                comicData.put("seriesName", comic.getSeriesName());
                comicData.put("volume", comic.getVolume());
                comicData.put("author", comic.getAuthor());
                comicData.put("publisher", comic.getPublisher());
                comicData.put("thumbnailUrl", comic.getThumbnailUrl());
                comicData.put("status", comic.getStatus());
                comicData.put("createdAt", comic.getCreatedAt() != null ? comic.getCreatedAt().toString() : null);

                result.put("success", true);
                result.put("comic", comicData);

            }

        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "ID không hợp lệ");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi server: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
        response.getWriter().flush();
    }
}