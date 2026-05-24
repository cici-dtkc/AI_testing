package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.RateLimiter;

import java.io.IOException;
//qua nhiu request la loi 429
@WebFilter(filterName = "RateLimitFilter", urlPatterns = {"/login", "/RegisterServlet", "/AuthenEmailServlet"})
public class RateLimitFilter implements Filter {
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SIZE_MILLIS = 60000;
    private RateLimiter rateLimiter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        rateLimiter = new RateLimiter(MAX_REQUESTS, WINDOW_SIZE_MILLIS);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String clientIp = getClientIp(httpRequest);
            if (!rateLimiter.tryAcquire(clientIp)) {
                httpResponse.setStatus(429);
                httpResponse.setHeader("Retry-After", "60");
                String errorMsg = "Bạn đã thực hiện quá nhiều yêu cầu. Vui lòng thử lại sau ít phút!";
                httpRequest.setAttribute("error", errorMsg);
                String path = httpRequest.getRequestURI();
                String targetJsp;
                if (path.contains("/login")) {
                    targetJsp = "/frontend/public/login.jsp";
                } else if (path.contains("/RegisterServlet")) {
                    targetJsp = "/frontend/public/Register.jsp";
                } else if (path.contains("/AuthenEmailServlet")) {
                    targetJsp = "/frontend/public/ForgotPass.jsp";
                } else {
                    targetJsp = "/frontend/public/login.jsp";
                }
                httpRequest.getRequestDispatcher(targetJsp).forward(httpRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
    @Override
    public void destroy() {
    }
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
