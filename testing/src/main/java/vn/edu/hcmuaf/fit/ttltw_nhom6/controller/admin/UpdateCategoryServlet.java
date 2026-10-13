package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/updateCategory")
public class UpdateCategoryServlet extends HttpServlet {

    private CategoriesDao categoriesDao;
    private Gson gson;

    @Override
    public void init() {
        categoriesDao = new CategoriesDao();

        // Cấu hình Gson để serialize LocalDateTime
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        Map<String, Object> result = new HashMap<>();

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Map<String, Object> data = gson.fromJson(sb.toString(), Map.class);

            int id = ((Double) data.get("id")).intValue();
            String name = (String) data.get("name");
            String description = (String) data.get("description");

            if (id <= 0 || name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Dữ liệu không hợp lệ!");
                out.print(gson.toJson(result));
                return;
            }

            if (categoriesDao.isNameExists(name.trim(), id)) {
                result.put("success", false);
                result.put("message", "Tên thể loại đã tồn tại!");
                out.print(gson.toJson(result));
                return;
            }

            Category category = new Category();
            category.setId(id);
            category.setNameCategories(name.trim());
            category.setDescription(description != null ? description.trim() : "");

            boolean success = categoriesDao.updateCategory(category);

            result.put("success", success);
            result.put("message", success ? "Cập nhật thể loại thành công!" : "Cập nhật thất bại!");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi hệ thống!");
            e.printStackTrace();
        }

        out.print(gson.toJson(result));
        out.flush();
    }
}