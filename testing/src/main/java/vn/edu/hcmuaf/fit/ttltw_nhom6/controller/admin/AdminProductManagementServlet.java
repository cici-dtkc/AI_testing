package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ComicService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet({"/admin/products/search"})
public class AdminProductManagementServlet extends HttpServlet {
    private ComicService comicService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        comicService = new ComicService();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        try {
            String keyword = request.getParameter("keyword");
            String pageStr = request.getParameter("page");
            String hiddenFilterStr = request.getParameter("hiddenFilter");

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                try {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }

            Integer hiddenFilter = null;
            if (hiddenFilterStr != null && !hiddenFilterStr.trim().isEmpty()) {
                try {
                    hiddenFilter = Integer.parseInt(hiddenFilterStr);
                } catch (NumberFormatException e) {
                    hiddenFilter = null;
                }
            }

            int limit = 8;

            List<Comic> comics;
            int totalComics;

            if (keyword != null && !keyword.trim().isEmpty()) {
                comics = comicService.searchComicsAdminWithFilter(keyword.trim(), null, null, hiddenFilter, page, limit);
                totalComics = comicService.countComicsAdminWithFilter(keyword.trim(), null, null, hiddenFilter);
            } else {
                comics = comicService.getAllComicsAdminWithFilter(page, limit, hiddenFilter);
                totalComics = comicService.countAllComicsWithFilter(hiddenFilter);
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
                dto.put("isHidden", comic.getIsHidden());
                simplifiedComics.add(dto);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("comics", simplifiedComics);
            result.put("currentPage", page);
            result.put("totalPages", totalPages);
            result.put("totalComics", totalComics);

            String jsonResponse = gson.toJson(result);

            response.getWriter().write(jsonResponse);
            response.getWriter().flush();


        } catch (Exception e) {
            System.err.println("ERROR:");
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            response.getWriter().write(gson.toJson(errorResponse));
            response.getWriter().flush();
        }
    }
}