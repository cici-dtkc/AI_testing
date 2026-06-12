package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.config.SecurityConfig;

import java.io.IOException;

@WebFilter("/*")
public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "no-referrer-when-downgrade");

        String csp = "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.google.com/recaptcha/ https://www.google.com/ https://www.gstatic.com/; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com/ https://cdnjs.cloudflare.com/; " +
                "font-src 'self' https://fonts.gstatic.com/ https://cdnjs.cloudflare.com/; " +
                "img-src 'self' data: https:; " +
                "frame-src 'self' https://www.google.com/; " +
                "connect-src 'self' https://www.google.com/ https://provinces.open-api.vn/ https://online-gateway.ghn.vn/;";
        httpResponse.setHeader("Content-Security-Policy", csp);

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        boolean isStatic = path.startsWith("/frontend/css/") || path.startsWith("/frontend/js/") || path.startsWith("/img/")
                || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")
                || path.endsWith(".jpeg") || path.endsWith(".gif") || path.endsWith(".svg") || path.endsWith(".ico")
                || path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".otf");

        if (!isStatic) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null && session.getAttribute("currentUser") != null) {
                String sessionFingerprint = (String) session.getAttribute("clientFingerprint");
                String currentFingerprint = SecurityConfig.generateFingerprint(httpRequest);
                
                if (sessionFingerprint != null) {
                    if (!sessionFingerprint.equals(currentFingerprint)) {
                     session.invalidate();
                        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                        return;
                    }
                } else {
                    session.setAttribute("clientFingerprint", currentFingerprint);
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
