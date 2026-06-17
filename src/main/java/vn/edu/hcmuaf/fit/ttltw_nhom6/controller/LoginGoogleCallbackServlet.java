package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Cart;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@WebServlet(urlPatterns = "/login-google-callback", name = "LoginGoogleCallbackServlet")
public class LoginGoogleCallbackServlet extends HttpServlet {

    private static final String CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
    private static final String REDIRECT_URI = "http://comic-store.id.vn/login-google-callback";

    private UserDao userDao;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());

        if (CLIENT_ID == null || CLIENT_SECRET == null) {
            throw new RuntimeException("Missing GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET environment variables");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String error = request.getParameter("error");

        if (error != null) {
            request.setAttribute("error", "Bạn đã từ chối đăng nhập bằng Google.");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        if (code == null || code.trim().isEmpty()) {
            request.setAttribute("error", "Đăng nhập Google thất bại. Vui lòng thử lại.");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        try {
            String accessToken = exchangeCodeForToken(code);

            JSONObject userInfo = getUserInfo(accessToken);

            String email = userInfo.getString("email");
            String name = userInfo.getString("name");
            String picture = userInfo.optString("picture", "");
            String googleId = userInfo.getString("id");


            Optional<User> userOpt = userDao.findByEmail(email);
            User user;

            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = new User();
                user.setEmail(email);
                user.setUsername(name);
                user.setFullName(name);
                user.setRole("USER");
                user.setId(0);
            }

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                Cart oldCart = (Cart) oldSession.getAttribute("cart");
                if (oldCart != null) {
                    System.out.println("Giỏ hàng cũ có: " + oldCart.getItems().size() + " sản phẩm");
                }
                oldSession.invalidate();
            }

            HttpSession newSession = request.getSession(true);

            boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());

            if (isAdmin) {
                newSession.setAttribute("currentUser", user);
                newSession.setAttribute("userId", user.getId());
                newSession.setAttribute("isAdmin", true);
                newSession.setAttribute("loginMethod", "google");

                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setDateHeader("Expires", 0);

                response.sendRedirect(request.getContextPath() + "/frontend/admin/dashboard.jsp");
                return;
            }

            Cart newCart = new Cart();
            System.out.println("Giỏ hàng mới có: " + newCart.getItems().size() + " sản phẩm");

            newSession.setAttribute("cart", newCart);
            newSession.setAttribute("currentUser", user);
            newSession.setAttribute("clearCartLocalStorage", true);
            newSession.setAttribute("loginMethod", "google");

            newSession.setAttribute("userPicture", picture);
            newSession.setAttribute("googleId", googleId);

            newSession.setMaxInactiveInterval(30 * 60); // 30 phút


            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            String redirectUrl = (String) newSession.getAttribute("redirectAfterLogin");

            if (redirectUrl != null) {
                newSession.removeAttribute("redirectAfterLogin");
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect(request.getContextPath() + "/home");
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Đã xảy ra lỗi khi đăng nhập bằng Google. Vui lòng thử lại.");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
        }
    }

    /**
     * Đổi authorization code thành access token
     */
    private String exchangeCodeForToken(String code) throws IOException {
        String params = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        URL tokenUrl = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection conn = (HttpURLConnection) tokenUrl.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

        StringBuilder tokenResponse = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            tokenResponse.append(line);
        }
        br.close();

        JSONObject tokenJson = new JSONObject(tokenResponse.toString());
        return tokenJson.getString("access_token");
    }


    private JSONObject getUserInfo(String accessToken) throws IOException {
        URL userUrl = new URL(
                "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken);

        HttpURLConnection userConn = (HttpURLConnection) userUrl.openConnection();
        userConn.setRequestMethod("GET");

        BufferedReader userReader = new BufferedReader(
                new InputStreamReader(userConn.getInputStream(), StandardCharsets.UTF_8));

        StringBuilder userInfo = new StringBuilder();
        String line;
        while ((line = userReader.readLine()) != null) {
            userInfo.append(line);
        }
        userReader.close();

        return new JSONObject(userInfo.toString());
    }
}
