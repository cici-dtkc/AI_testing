package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/deleteCategory")
public class DeleteCategoryServlet extends HttpServlet {
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
            Map<String, Object> data = gson.fromJson(sb.toString(), Map.class);

            Double idDouble = (Double) data.get("id");
            int id = idDouble != null ? idDouble.intValue() : 0;

            if (id <= 0) {
                response.put("success", false);
                response.put("message", "ID không hợp lệ!");
                out.print(gson.toJson(response));
                return;
            }

            boolean result = categoriesDao.deleteCategory(id);

            if (result) {
                response.put("success", true);
                response.put("message", "Xóa thể loại thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy thể loại hoặc có lỗi xảy ra!");
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