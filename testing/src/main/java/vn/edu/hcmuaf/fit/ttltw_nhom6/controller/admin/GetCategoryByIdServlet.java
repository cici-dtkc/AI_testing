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

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/getCategoryById")
public class GetCategoryByIdServlet extends HttpServlet {

    private CategoriesDao categoriesDao;
    private Gson gson;

    @Override
    public void init() {
        categoriesDao = new CategoriesDao();

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        Map<String, Object> result = new HashMap<>();

        try {
            String idParam = req.getParameter("id");

            if (idParam == null || idParam.isEmpty()) {
                result.put("success", false);
                result.put("message", "Thiếu ID!");
                out.print(gson.toJson(result));
                return;
            }

            int id = Integer.parseInt(idParam);
            Category category = categoriesDao.getCategoryById(id);

            if (category != null && !category.isDeleted()) {

                result.put("success", true);
                result.put("category", category);
                result.put("message", "Lấy thông tin thành công!");

                String json = gson.toJson(result);
                System.out.println("JSON response: " + json);
                out.print(json);

            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy thể loại!");
                out.print(gson.toJson(result));
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "ID không hợp lệ!");
            out.print(gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi hệ thống: " + e.getMessage());
            out.print(gson.toJson(result));
        }

        out.flush();
    }
}