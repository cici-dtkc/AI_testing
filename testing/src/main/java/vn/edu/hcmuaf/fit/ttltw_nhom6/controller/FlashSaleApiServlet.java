package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.FlashSaleService;

import java.io.IOException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/flash-sale/*")
public class FlashSaleApiServlet extends HttpServlet {

    private final FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
    private final FlashSaleComicsDAO flashSaleComicsDAO = new FlashSaleComicsDAO();
    private final FlashSaleService flashSaleService = new FlashSaleService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            sendErrorResponse(resp, "Invalid request");
            return;
        }

        String[] parts = pathInfo.split("/");

        if (parts.length < 2) {
            sendErrorResponse(resp, "Flash Sale ID is required");
            return;
        }

        try {
            int flashSaleId = Integer.parseInt(parts[1]);

            if (parts.length >= 3 && "comics".equals(parts[2])) {
                getFlashSaleComics(flashSaleId, resp);
            } else {
                getFlashSaleDetail(flashSaleId, resp);
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(resp, "Invalid Flash Sale ID");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(resp, "Server error: " + e.getMessage());
        }
    }


    private void getFlashSaleComics(int flashSaleId, HttpServletResponse resp) throws IOException {
        flashSaleDAO.updateStatuses();

        FlashSale flashSale = flashSaleDAO.getById(flashSaleId);

        if (flashSale == null) {
            sendErrorResponse(resp, "Flash Sale not found");
            return;
        }

        List<Map<String, Object>> comics = flashSaleService.getExclusiveComicsForFlashSale(flashSaleId);

        Map<String, Object> data = new HashMap<>();

        Map<String, Object> flashSaleInfo = new HashMap<>();
        flashSaleInfo.put("id", flashSale.getId());
        flashSaleInfo.put("name", flashSale.getName());
        flashSaleInfo.put("discountPercent", flashSale.getDiscountPercent());
        flashSaleInfo.put("status", flashSale.getStatus());
        flashSaleInfo.put("startTime", flashSale.getStartTime().toString());
        flashSaleInfo.put("endTime", flashSale.getEndTime().toString());

        long startTimeMillis = flashSale.getStartTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        long endTimeMillis = flashSale.getEndTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        flashSaleInfo.put("startTimeMillis", startTimeMillis);
        flashSaleInfo.put("endTimeMillis", endTimeMillis);

        data.put("flashSale", flashSaleInfo);
        data.put("comics", comics);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        resp.getWriter().write(gson.toJson(response));

    }


    private void getFlashSaleDetail(int flashSaleId, HttpServletResponse resp) throws IOException {
        FlashSale flashSale = flashSaleDAO.getById(flashSaleId);

        if (flashSale == null) {
            sendErrorResponse(resp, "Flash Sale not found");
            return;
        }

        Map<String, Object> flashSaleInfo = new HashMap<>();
        flashSaleInfo.put("id", flashSale.getId());
        flashSaleInfo.put("name", flashSale.getName());
        flashSaleInfo.put("discountPercent", flashSale.getDiscountPercent());
        flashSaleInfo.put("status", flashSale.getStatus());
        flashSaleInfo.put("startTime", flashSale.getStartTime().toString());
        flashSaleInfo.put("endTime", flashSale.getEndTime().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", flashSaleInfo);

        resp.getWriter().write(gson.toJson(response));
    }


    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);

        resp.getWriter().write(gson.toJson(response));
    }
}