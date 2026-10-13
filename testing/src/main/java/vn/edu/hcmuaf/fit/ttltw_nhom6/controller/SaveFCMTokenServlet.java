package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "SaveFCMTokenServlet", value = "/SaveFCMTokenServlet")
public class SaveFCMTokenServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            User user = (User) request.getSession().getAttribute("currentUser");
            if (user == null) {
                response.setStatus(401);
                response.getWriter().write("{\"success\": false, \"error\": \"Not authenticated\"}");
                return;
            }

            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String token = json.get("token").getAsString();

            if (token == null || token.isEmpty()) {
                response.setStatus(400);
                response.getWriter().write("{\"success\": false, \"error\": \"Token is required\"}");
                return;
            }

            NotificationDAO.getInstance().saveFCMToken(user.getId(), token);

            response.getWriter().write("{\"success\": true, \"message\": \"FCM token saved\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }

    }
}
