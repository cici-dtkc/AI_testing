package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.RoleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Role;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@MultipartConfig
@WebServlet("/admin/create-flashsale")
public class CreateFlashSaleServlet extends HttpServlet {

    private final FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
    private final FlashSaleComicsDAO flashSaleComicsDAO = new FlashSaleComicsDAO();


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JsonObject responseJson = new JsonObject();

        try {
            String name = req.getParameter("flashSaleName");
            String discountPercentStr = req.getParameter("discountPercent");
            String startTimeStr = req.getParameter("startTime");
            String endTimeStr = req.getParameter("endTime");
            String[] comicIdArray = req.getParameterValues("comicIds");

            if (comicIdArray == null) {
                comicIdArray = new String[0];
            }

            if (name == null || name.trim().isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Tên Flash Sale không được để trống");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            if (discountPercentStr == null || discountPercentStr.trim().isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Vui lòng nhập phần trăm giảm giá");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            double discountPercent;
            try {
                discountPercent = Double.parseDouble(discountPercentStr.trim());

                User user = (User) req.getSession().getAttribute("currentUser");
                Role role = new RoleDAO().getRoleById(user.getRoleId());

                if (role.getRoleName().equalsIgnoreCase("admin_promotion") && discountPercent > 50) {
                    responseJson.addProperty("success", false);
                    responseJson.addProperty("message", "admin_promotion chỉ được tạo Flash Sale giảm tối đa 50%!");
                    resp.getWriter().write(responseJson.toString());
                    return;
                }
                if (discountPercent < 1 || discountPercent > 90) {
                    responseJson.addProperty("success", false);
                    responseJson.addProperty("message", "Phần trăm giảm phải từ 1% đến 90%");
                    resp.getWriter().write(responseJson.toString());
                    return;
                }
            } catch (NumberFormatException e) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Phần trăm giảm không hợp lệ");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            if (startTimeStr == null || startTimeStr.isEmpty() ||
                    endTimeStr == null || endTimeStr.isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Vui lòng chọn giờ bắt đầu và kết thúc");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            if (comicIdArray.length == 0) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Vui lòng chọn ít nhất một truyện");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            LocalDateTime now = LocalDateTime.now();

            if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Giờ kết thúc phải sau giờ bắt đầu");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            String status;
            if (now.isBefore(startTime)) {
                status = "scheduled";  // Sắp diễn ra
            } else if (now.isAfter(endTime)) {
                status = "ended";      // Đã kết thúc
            } else {
                status = "active";     // Đang diễn ra
            }

            FlashSale flashSale = new FlashSale();
            flashSale.setName(name.trim());
            flashSale.setDiscountPercent(discountPercent);
            flashSale.setStartTime(startTime);
            flashSale.setEndTime(endTime);
            flashSale.setStatus(status);

            int flashSaleId = flashSaleDAO.insert(flashSale);

            List<Integer> comicIdList = new ArrayList<>();
            for (String idStr : comicIdArray) {
                try {
                    comicIdList.add(Integer.parseInt(idStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }

            flashSaleComicsDAO.insertLinks(flashSaleId, comicIdList);

            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Tạo Flash Sale thành công!");
            responseJson.addProperty("flashSaleId", flashSaleId);

        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Lỗi server: " + e.getMessage());
        }

        resp.getWriter().write(responseJson.toString());
    }
}