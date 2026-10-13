package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@WebServlet("/login-facebook-callback")
public class LoginFacebookCallbackServlet extends HttpServlet {

    private static final String APP_ID     = "4112997315630876";
    private static final String APP_SECRET = "49ed258d8df844f2d60d366d7968e64b";
    private static final String REDIRECT_URI = "http://comic-store.id.vn/login-facebook-callback";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        if (code == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=Bạn đã hủy đăng nhập Facebook");
            return;
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String tokenUrl = "https://graph.facebook.com/v19.0/oauth/access_token"
                    + "?client_id="     + APP_ID
                    + "&redirect_uri="  + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&client_secret=" + APP_SECRET
                    + "&code="          + code;
            HttpGet tokenGet = new HttpGet(tokenUrl);
            CloseableHttpResponse tokenResponse = client.execute(tokenGet);
            String tokenJson = EntityUtils.toString(tokenResponse.getEntity());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> tokenData = mapper.readValue(tokenJson, Map.class);
            String accessToken = (String) tokenData.get("access_token");
            if (accessToken == null) {
                response.sendRedirect(request.getContextPath()
                        + "/login?error=Không thể lấy access token từ Facebook");
                return;
            }
            String userInfoUrl = "https://graph.facebook.com/me"
                    + "?fields=id,name,email"
                    + "&access_token=" + accessToken;
            HttpGet infoGet = new HttpGet(userInfoUrl);
            CloseableHttpResponse infoResponse = client.execute(infoGet);
            String userJson = EntityUtils.toString(infoResponse.getEntity());
            Map<String, Object> userInfo = mapper.readValue(userJson, Map.class);
            String fbId    = (String) userInfo.get("id");
            String fbName  = (String) userInfo.get("name");
            String fbEmail = (String) userInfo.getOrDefault("email", "");
            UserDao userDao = UserDao.getInstance();
            User user = findOrCreateUser(userDao, fbId, fbName, fbEmail);
            if (!user.getIsActive()) {
                response.sendRedirect(request.getContextPath()
                        + "/login?error=Tài khoản của bạn đã bị khóa");
                return;
            }
            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);
            session.setAttribute("userId",      user.getId());
            session.setAttribute("role",        user.getRole());
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/home");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath()
                    + "/login?error=Đăng nhập Facebook thất bại, vui lòng thử lại");
        }
    }
    private User findOrCreateUser(UserDao userDao, String fbId, String fbName, String fbEmail) {
        // Tìm theo email trước
        if (!fbEmail.isEmpty()) {
            Optional<User> existing = userDao.findByEmail(fbEmail);
            if (existing.isPresent()) return existing.get();
        }
        // Tìm theo username fb_<id>
        Optional<User> byUsername = userDao.findByUsername("fb_" + fbId);
        if (byUsername.isPresent()) return byUsername.get();
        // Chưa có thì tạo mới
        User newUser = new User();
        newUser.setUsername("fb_" + fbId);
        newUser.setEmail(fbEmail.isEmpty() ? fbId + "@facebook.com" : fbEmail);
        newUser.setFullName(fbName);
        newUser.setPasswordHash("");
        userDao.insert(newUser);
        return userDao.findByUsername("fb_" + fbId)
                .orElseThrow(() -> new RuntimeException("Lỗi tạo tài khoản Facebook"));
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
