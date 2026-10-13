package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
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
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/addCategory")
public class AddCategoryServlet extends HttpServlet {
    private CategoriesDao categoriesDao;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        PrintWriter out = resp.getWriter();
        Map<String, Object> response = new HashMap<>();

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> data = gson.fromJson(sb.toString(), Map.class);

            String name = data.get("name");
            String description = data.get("description");

            if (name == null || name.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên thể loại không được để trống!");
                out.print(gson.toJson(response));
                return;
            }

            if (name.trim().length() > 100) {
                response.put("success", false);
                response.put("message", "Tên thể loại không được vượt quá 100 ký tự!");
                out.print(gson.toJson(response));
                return;
            }

            if (categoriesDao.isNameExists(name.trim(), null)) {
                response.put("success", false);
                response.put("message", "Tên thể loại đã tồn tại!");
                out.print(gson.toJson(response));
                return;
            }

            Category category = new Category();
            category.setNameCategories(name.trim());
            category.setDescription(description != null ? description.trim() : "");

            boolean result = categoriesDao.addCategory(category);

            if (result) {
                response.put("success", true);
                response.put("message", "Thêm thể loại thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Có lỗi xảy ra khi thêm thể loại!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
        }

        out.print(gson.toJson(response));
        out.flush();
    }
}