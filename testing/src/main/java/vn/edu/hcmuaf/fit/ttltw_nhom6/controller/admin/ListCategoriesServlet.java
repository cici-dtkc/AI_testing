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

@WebServlet("/admin/listCategories")
public class ListCategoriesServlet extends HttpServlet {
    private CategoriesDao categoriesDao;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
        gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        PrintWriter out = resp.getWriter();
        Map<String, Object> response = new HashMap<>();

        try {
            String pageStr = req.getParameter("page");
            String pageSizeStr = req.getParameter("pageSize");
            String keyword = req.getParameter("keyword");

            int page = (pageStr != null) ? Integer.parseInt(pageStr) : 1;
            int pageSize = (pageSizeStr != null) ? Integer.parseInt(pageSizeStr) : 10;

            List<Category> categories;
            int totalRecords;

            if (keyword != null && !keyword.trim().isEmpty()) {
                categories = categoriesDao.searchCategoriesByName(keyword.trim(), page, pageSize);
                totalRecords = categoriesDao.countSearchResults(keyword.trim());
            } else {
                categories = categoriesDao.getCategoriesByPage(page, pageSize);
                totalRecords = categoriesDao.getTotalCategories();
            }

            int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

            response.put("success", true);
            response.put("categories", categories);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            response.put("totalRecords", totalRecords);
            response.put("pageSize", pageSize);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi khi tải dữ liệu: " + e.getMessage());
        }

        out.print(gson.toJson(response));
        out.flush();
    }
}