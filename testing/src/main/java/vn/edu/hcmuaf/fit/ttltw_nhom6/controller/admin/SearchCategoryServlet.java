package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.LocalDateTimeAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SearchCategoryServlet", urlPatterns = {"/admin/categories/search"})
public class SearchCategoryServlet extends HttpServlet {

    private CategoriesDao categoriesDao;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        this.categoriesDao = new CategoriesDao();
        gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDateTime.class,
                        new LocalDateTimeAdapter()
                )
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
            String pageParam = request.getParameter("page");

            int page = 1;
            int pageSize = 10;

            if (pageParam != null && !pageParam.trim().isEmpty()) {
                try {
                    page = Integer.parseInt(pageParam);
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }

            Map<String, Object> responseData = new HashMap<>();

            if (keyword == null || keyword.trim().isEmpty()) {

                List<Category> categories = categoriesDao.getCategoriesByPage(page, pageSize);
                int totalCategories = categoriesDao.getTotalCategories();
                int totalPages = (int) Math.ceil((double) totalCategories / pageSize);

                responseData.put("success", true);
                responseData.put("categories", categories);
                responseData.put("currentPage", page);
                responseData.put("totalPages", totalPages);
                responseData.put("totalCategories", totalCategories);
                responseData.put("pageSize", pageSize);
                responseData.put("message", "Loaded " + categories.size() + " categories");

            } else {
                keyword = keyword.trim();

                List<Category> categories = categoriesDao.searchCategoriesByName(keyword, page, pageSize);
                int totalResults = categoriesDao.countSearchResults(keyword);
                int totalPages = (int) Math.ceil((double) totalResults / pageSize);

                responseData.put("success", true);
                responseData.put("categories", categories);
                responseData.put("currentPage", page);
                responseData.put("totalPages", totalPages);
                responseData.put("totalCategories", totalResults);
                responseData.put("pageSize", pageSize);
                responseData.put("keyword", keyword);

                if (categories.isEmpty()) {
                    responseData.put("message", "Không tìm thấy thể loại nào với từ khóa: " + keyword);
                } else {
                    responseData.put("message", "Tìm thấy " + totalResults + " kết quả");
                }
            }

            PrintWriter out = response.getWriter();
            out.print(gson.toJson(responseData));
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi server: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            PrintWriter out = response.getWriter();
            out.print(gson.toJson(errorResponse));
            out.flush();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}