package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Cart;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CartService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.OrderViolationService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.PasswordUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ReCaptchaUtils;
import vn.edu.hcmuaf.fit.ttltw_nhom6.config.SecurityConfig;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private UserDao userDao;
    private CartService cartService;

    @Override
    public void init() {
        userDao = new UserDao(JdbiConnector.get());
        cartService = new CartService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        if (!ReCaptchaUtils.verify(gRecaptchaResponse)) {
            request.setAttribute("error", "Vui lòng xác minh CAPTCHA chính xác!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        String usernameOrEmail = request.getParameter("username");
        String password = request.getParameter("password");
        String reme = request.getParameter("rememberMe");

        if (!PasswordUtils.isValidPasswordFormat(password)) {
            request.setAttribute("error",
                    "Mat khau it nhat 8 ky tu, gom chu hoa, chu thuong, so va ky tu dac biet!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        Optional<User> userOpt = userDao.findByUsernameOrEmail(usernameOrEmail);
        if (!userOpt.isPresent()) {
            request.setAttribute("error", "Hay nhap dung tai khoan va mat khau!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        User user = userOpt.get();
        // Check khóa vĩnh viễn / chưa kích hoạt
        if (!user.isActive()) {
            request.setAttribute("error", "Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }


        // Check khóa tạm
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes();
            if (minutesLeft <= 60) {
                request.setAttribute("error", "Tài khoản bị khóa tạm " + minutesLeft + " phút do đăng nhập sai quá nhiều lần!");
            } else {
                long hoursLeft = minutesLeft / 60;
                request.setAttribute("error", "Tài khoản bị khóa tạm " + hoursLeft + " giờ do hủy đơn hàng quá nhiều lần trong 1 giờ!");
            }
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
            return;
        }

        if (PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            OrderViolationService.getInstance().resetLoginFailureCount(user.getId());

            // BUOC 1: Lấy guest cart từ session CŨ trước khi invalidate
            HttpSession oldSession = request.getSession(false);
            Cart guestCart = null;
            if (oldSession != null) {
                guestCart = (Cart) oldSession.getAttribute("cart");
                oldSession.invalidate();
            }

// BUOC 2: Tạo session mới
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("clientFingerprint", SecurityConfig.generateFingerprint(request));

            //save cookie
            if ("on".equals(reme)) {
                Cookie cookieNameUser = new Cookie("cookieUsername", usernameOrEmail);
                Cookie cookiePassUser = new Cookie("cookiePass", password);
                Cookie cookieReme = new Cookie("cookieReme", reme);
                response.addCookie(cookieNameUser);
                response.addCookie(cookiePassUser);
                response.addCookie(cookieReme);
            }

            if ("admin".equalsIgnoreCase(user.getRole())) {
                newSession.setAttribute("currentUser", user);
                newSession.setAttribute("userId", user.getId());
                newSession.setAttribute("isAdmin", true);
                setNoCacheHeaders(response);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            }

// BUOC 3: Merge guest cart (session) vào user cart (DB)
            Cart userCart = new Cart();
            if (guestCart != null && !guestCart.getItems().isEmpty()) {
                cartService.mergeSessionCartToDb(userCart, user.getId(), guestCart);
            } else {
                cartService.getCart(userCart, user.getId(), null);
            }

            newSession.setAttribute("cart", userCart);
            newSession.setAttribute("currentUser", user);

            setNoCacheHeaders(response);
            String redirectUrl = (String) newSession.getAttribute("redirectAfterLogin");
            if (redirectUrl != null) {
                newSession.removeAttribute("redirectAfterLogin");
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect(request.getContextPath() + "/home");
            }

        } else {
            OrderViolationService.getInstance().incrementLoginFailureCount(usernameOrEmail);
            OrderViolationService.getInstance().checkLoginFailureViolation(usernameOrEmail);

            Optional<User> updatedUserOpt = userDao.findByUsernameOrEmail(usernameOrEmail);
            if (updatedUserOpt.isPresent()) {
                User updatedUser = updatedUserOpt.get();
                if (updatedUser.getLockedUntil() != null && updatedUser.getLockedUntil().isAfter(LocalDateTime.now())) {
                    long minutesLeft = java.time.Duration.between(LocalDateTime.now(), updatedUser.getLockedUntil()).toMinutes();
                    if (minutesLeft <= 60) {
                        request.setAttribute("error", "Tài khoản bị khóa tạm " + minutesLeft + " phút do đăng nhập sai quá nhiều lần!");
                    } else {
                        long hoursLeft = minutesLeft / 60;
                        request.setAttribute("error", "Tài khoản bị khóa tạm " + hoursLeft + " giờ do hủy đơn hàng quá nhiều lần trong 1 giờ!");
                    }
                    request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
                    return;
                }
            }
            request.setAttribute("error", "Hay nhap dung tai khoan va mat khau!");
            request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession().getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        request.getRequestDispatcher("/frontend/public/login.jsp").forward(request, response);
    }

    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}