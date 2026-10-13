package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/admin/products/delete")
public class DeleteComicServlet extends HttpServlet {

    private ComicDAO comicsDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        comicsDAO = new ComicDAO();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (!RoleUtil.canManage(currentUser, "manage_product")) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Bạn không có quyền thực hiện thao tác này.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }

        try {
            String comicIdsParam = request.getParameter("comicIds");

            if (comicIdsParam == null || comicIdsParam.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Thiếu thông tin ID truyện");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            String[] comicIds = comicIdsParam.split(",");
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (String idStr : comicIds) {
                try {
                    int comicId = Integer.parseInt(idStr.trim());

                    Comic comic = comicsDAO.getComicById(comicId);
                    if (comic == null) {
                        failCount++;
                        errorMessages.append("Không tìm thấy truyện ID: ").append(comicId).append("; ");
                        continue;
                    }

                    boolean deleted = comicsDAO.softDeleteComicSafely(comicId);

                    if (deleted) {
                        successCount++;
                        logActivity(request, "SOFT_DELETE_COMIC", String.valueOf(comicId), comic.getNameComics());
                    } else {
                        failCount++;
                        errorMessages.append("Không thể xóa truyện ID: ").append(comicId).append("; ");
                    }

                } catch (NumberFormatException e) {
                    failCount++;
                    errorMessages.append("ID không hợp lệ: ").append(idStr).append("; ");
                }
            }

            if (successCount > 0 && failCount == 0) {
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message",
                        "Đã xóa thành công " + successCount + " truyện");
            } else if (successCount > 0 && failCount > 0) {
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message",
                        "Xóa thành công " + successCount + " truyện, thất bại " + failCount + " truyện. " + errorMessages.toString());
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message",
                        "Không thể xóa truyện. " + errorMessages.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Lỗi hệ thống: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }

    private void logActivity(HttpServletRequest request, String action,
                             String comicId, String comicName) {
        try {
            HttpSession session = request.getSession(false);
            String adminUser = "unknown";

            if (session != null) {
                Object username = session.getAttribute("adminUsername");
                if (username != null) {
                    adminUser = username.toString();
                }
            }

        } catch (Exception e) {
            System.err.println("Error logging activity: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", false);
        jsonResponse.addProperty("message", "Method not allowed. Use POST instead.");

        response.getWriter().print(gson.toJson(jsonResponse));
    }
}