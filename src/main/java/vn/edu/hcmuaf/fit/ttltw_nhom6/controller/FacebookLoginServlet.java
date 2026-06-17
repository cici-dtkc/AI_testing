package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/facebook-login")
public class FacebookLoginServlet extends HttpServlet {

    private static final String APP_ID = "4112997315630876";
    private static final String REDIRECT_URI = "http://comic-store.id.vn/login-facebook-callback";
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String facebookLoginUrl = "https://www.facebook.com/v19.0/dialog/oauth"
                + "?client_id=" + APP_ID
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&scope=public_profile,email"
                + "&response_type=code";

        response.sendRedirect(facebookLoginUrl);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
