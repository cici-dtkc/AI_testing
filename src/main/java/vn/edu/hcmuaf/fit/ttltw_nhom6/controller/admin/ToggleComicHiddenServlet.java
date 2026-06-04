package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "ToggleHiddenServlet", urlPatterns = {"/admin/products/toggle-hidden"})
public class ToggleComicHiddenServlet extends HttpServlet {

    private final ComicDAO comicDAO = new ComicDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        Map<String, Object> jsonResponse = new HashMap<>();
        PrintWriter out = response.getWriter();

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!RoleUtil.canManage(currentUser, "manage_product")) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Bạn không có quyền thực hiện thao tác này.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }

        try {
            String idStr = request.getParameter("id");
            String hiddenStr = request.getParameter("hidden");


            if (idStr == null || idStr.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "ID sản phẩm không hợp lệ");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (hiddenStr == null || hiddenStr.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Giá trị hidden không hợp lệ");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            int comicId;
            int hidden;

            try {
                comicId = Integer.parseInt(idStr);
                hidden = Integer.parseInt(hiddenStr);
            } catch (NumberFormatException e) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Dữ liệu không đúng định dạng");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if (hidden != 0 && hidden != 1) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Giá trị hidden phải là 0 hoặc 1");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            boolean success = comicDAO.toggleHidden(comicId, hidden);

            if (success) {
                String statusMessage = (hidden == 1) ? "ẩn" : "hiển thị";

                jsonResponse.put("success", true);
                jsonResponse.put("message", "Đã cập nhật trạng thái sản phẩm thành " + statusMessage);
                jsonResponse.put("comicId", comicId);
                jsonResponse.put("isHidden", hidden);

            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Không thể cập nhật trạng thái sản phẩm");
            }

        } catch (Exception e) {
            e.printStackTrace();

            jsonResponse.put("success", false);
            jsonResponse.put("message", "Lỗi server: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", false);
        jsonResponse.put("message", "Method GET không được hỗ trợ. Vui lòng sử dụng POST");

        PrintWriter out = response.getWriter();
        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}