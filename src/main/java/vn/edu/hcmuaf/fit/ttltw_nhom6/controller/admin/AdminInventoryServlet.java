package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.InventoryTransactionDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RoleUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/inventory")
public class AdminInventoryServlet extends HttpServlet {
    private InventoryTransactionDAO inventoryDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        inventoryDAO = new InventoryTransactionDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
boolean canEditInventory = RoleUtil.canManage(currentUser,"manage_inventory");
        request.setAttribute("canEditInventory", canEditInventory);
        try {
            int threshold = 10;
            String thresholdStr = request.getParameter("threshold");
            if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
                try {
                    threshold = Integer.parseInt(thresholdStr);
                } catch (NumberFormatException ignored) {}
            }

            List<Map<String, Object>> lowStockComics = inventoryDAO.getLowStockComics(threshold);
            List<Map<String, Object>> allComics = inventoryDAO.getAllComicsWithStock();
            List<Map<String, Object>> transactionHistory = inventoryDAO.getTransactionHistory();

            request.setAttribute("lowStockComics", lowStockComics);
            request.setAttribute("allComics", allComics);
            request.setAttribute("transactionHistory", transactionHistory);
            request.setAttribute("threshold", threshold);

            request.getRequestDispatcher("/frontend/admin/inventory.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Đã xảy ra lỗi khi tải dữ liệu kho: " + e.getMessage());
            request.getRequestDispatcher("/frontend/admin/inventory.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        Map<String, Object> jsonResponse = new HashMap<>();
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Phiên làm việc hết hạn!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }
        if (!RoleUtil.canManage(currentUser, "manage_inventory")) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Bạn không có quyền thực hiện thao tác này!");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }
        if (request.getSession().getAttribute("currentUser") == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Phiên làm việc hết hạn hoặc bạn không có quyền!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try {
            String action = request.getParameter("action");
            String comicIdStr = request.getParameter("comicId");
            String quantityStr = request.getParameter("quantity");
            String note = request.getParameter("note");

            if (action == null || comicIdStr == null || quantityStr == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Thiếu tham số bắt buộc!");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            int comicId = Integer.parseInt(comicIdStr);
            int quantity = Integer.parseInt(quantityStr);

            if (quantity <= 0) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Số lượng phải lớn hơn 0!");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            if (note == null) {
                note = "";
            } else {
                note = note.trim();
            }

            boolean result = false;
            String operationName = "";

            if ("import".equalsIgnoreCase(action)) {
                operationName = "Nhập kho";
                if (note.isEmpty()) {
                    note = "Nhập hàng thủ công";
                }
                result = inventoryDAO.recordImport(comicId, quantity, note);
            } else if ("export".equalsIgnoreCase(action)) {
                operationName = "Xuất kho";
                if (note.isEmpty()) {
                    note = "Xuất hàng thủ công";
                }
                result = inventoryDAO.recordExport(comicId, quantity, note);
            } else if ("damage".equalsIgnoreCase(action)) {
                operationName = "Báo hỏng";
                if (note.isEmpty()) {
                    note = "Báo hỏng sản phẩm";
                }
                result = inventoryDAO.recordDamage(comicId, quantity, note);
            }

            if (result) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", operationName + " thành công!");
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Không thể thực hiện " + operationName + ". Vui lòng kiểm tra tồn kho khả dụng!");
            }

        } catch (NumberFormatException e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Mã truyện hoặc số lượng không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Lỗi máy chủ: " + e.getMessage());
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }
}
