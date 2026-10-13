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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/products/list")
public class ListComicsServlet extends HttpServlet {

    private ComicDAO comicDAO;
    private Gson gson;


    @Override
    public void init() throws ServletException {
        try {
            comicDAO = new ComicDAO();
            gson = new Gson();

        } catch (Exception e) {
            e.printStackTrace();
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
            int page = 1;
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.isEmpty()) {
                page = Integer.parseInt(pageParam);
                if (page < 1) page = 1;
            }

            int limit = 8;
            String limitParam = request.getParameter("limit");
            if (limitParam != null && !limitParam.isEmpty()) {
                limit = Integer.parseInt(limitParam);
                if (limit < 1) limit = 8;
                if (limit > 1000) limit = 1000;
            }

            Integer hiddenFilter = null;
            String hiddenParam = request.getParameter("hiddenFilter");
            if (hiddenParam != null && !hiddenParam.isEmpty()) {
                try {
                    int hiddenValue = Integer.parseInt(hiddenParam);
                    if (hiddenValue == 0 || hiddenValue == 1) {
                        hiddenFilter = hiddenValue;
                    }
                } catch (NumberFormatException e) {
                }
            }


            List<Comic> comics;
            int totalComics;

            if (hiddenFilter != null) {
                comics = comicDAO.getAllComicsAdminWithFilter(page, limit, hiddenFilter);
                totalComics = comicDAO.countAllComicsWithFilter(hiddenFilter);
            } else {
                comics = comicDAO.getAllComicsAdmin(page, limit);
                totalComics = comicDAO.countAllComics();
            }

            int totalPages = (int) Math.ceil((double) totalComics / limit);


            List<Map<String, Object>> simplifiedComics = new ArrayList<>();
            for (Comic comic : comics) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", comic.getId());
                dto.put("nameComics", comic.getNameComics());
                dto.put("seriesName", comic.getSeriesName());
                dto.put("categoryName", comic.getCategoryName());
                dto.put("author", comic.getAuthor());
                dto.put("price", comic.getPrice());
                dto.put("stockQuantity", comic.getStockQuantity());
                dto.put("volume", comic.getVolume());
                dto.put("isHidden", comic.getIsHidden());
                simplifiedComics.add(dto);
            }

            result.put("success", true);
            result.put("comics", simplifiedComics);
            result.put("currentPage", page);
            result.put("totalPages", totalPages);
            result.put("totalComics", totalComics);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
        response.getWriter().flush();
    }
}