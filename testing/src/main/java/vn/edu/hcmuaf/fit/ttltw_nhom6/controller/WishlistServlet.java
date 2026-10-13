package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.WishlistDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


@WebServlet("/WishlistServlet")
public class WishlistServlet extends HttpServlet {
    private WishlistDAO wishlistDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        wishlistDAO = new WishlistDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();

        // kiểm tra đăng nhập
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập để có thể thêm sản phẩm vào danh sách yêu thích");
            result.put("inWishlist", false);
            out.print(gson.toJson(result));
            return;
        }
        String comicIdStr = request.getParameter("comic_id");
        if (comicIdStr == null || comicIdStr.isEmpty()) {
            result.put("success", false);
            result.put("message", "Thiếu thông tin sản phẩm");
            out.print(gson.toJson(result));
            return;
        }
        try {
            int comicId = Integer.parseInt(comicIdStr);
            boolean inWishlist = wishlistDAO.isInWishlist(currentUser.getId(), comicId);

            result.put("success", true);
            result.put("inWishlist", inWishlist);
            result.put("count", wishlistDAO.getWishlistCount(currentUser.getId()));

        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "ID sản phẩm không hợp lệ");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi hệ thống: " + e.getMessage());
        }
        out.print(gson.toJson(result));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập để có thể thêm sản phẩm vào danh sách yêu thích");
            out.print(gson.toJson(result));
            return;
        }
        String action = request.getParameter("action");
        String comicIdStr = request.getParameter("comic_id");

        if (action == null || comicIdStr == null) {
            result.put("success", false);
            result.put("message", "Thiếu thông tin");
            out.print(gson.toJson(result));
            return;
        }
        try {
            int comicId = Integer.parseInt(comicIdStr);
            boolean success = false;
            String message = "";
            switch (action) {
                case "add":
                    success = wishlistDAO.addToWishlist(currentUser.getId(), comicId);
                    message = success ? "Đã thêm vào danh sách yêu thích" : "Sản phẩm đã có trong danh sách yêu thích";
                    break;
                case "remove":
                    success = wishlistDAO.removeFromWishlist(currentUser.getId(), comicId);
                    message = success ? "Đã xóa khỏi danh sách yêu thích" : "Không tìm thấy sản phẩm trong danh sách";
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Hành động không hợp lệ");
                    out.print(gson.toJson(result));
                    return;
            }
            result.put("success", success);
            result.put("message", message);
            result.put("count", wishlistDAO.getWishlistCount(currentUser.getId()));
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "ID sản phẩm không hợp lệ");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        }
        out.print(gson.toJson(result));
    }
}
