package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.AuthorPublisherService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.gson.LocalDateTimeAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthorPublisherInforServlet", urlPatterns = {"/author-publisher-info"}, loadOnStartup = 1)
public class AuthorPublisherInforServlet extends HttpServlet {
    private AuthorPublisherService service;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        service = new AuthorPublisherService();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                    @Override
                    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                })
                .serializeNulls()
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String type = request.getParameter("type");
        String name = request.getParameter("name");


        if (type == null || name == null || name.trim().isEmpty()) {
            sendError(response, "Thiếu thông tin yêu cầu");
            return;
        }

        try {
            Map<String, Object> result;

            if ("author".equals(type)) {
                result = service.getAuthorInfo(name.trim());
                System.out.println("result 1: "+ result.size());
            } else if ("publisher".equals(type)) {
                result = service.getPublisherInfo(name.trim());
                System.out.println("result 2: "+ result.size());

            } else {
                sendError(response, "Loại thông tin không hợp lệ");
                return;
            }

            String jsonResponse = gson.toJson(result);

            PrintWriter out = response.getWriter();
            out.write(jsonResponse);
            out.flush();


        } catch (Exception e) {
            System.err.println("Servlet error:");
            e.printStackTrace();
            sendError(response, "Lỗi server: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        String json = gson.toJson(error);

        PrintWriter out = response.getWriter();
        out.write(json);
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}