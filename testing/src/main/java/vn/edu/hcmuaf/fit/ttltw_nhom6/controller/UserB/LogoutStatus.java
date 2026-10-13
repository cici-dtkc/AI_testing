package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.LoginSessionDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.LoginSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;

@WebServlet("/logout-status")
public class LogoutStatus extends HttpServlet {
    private LoginSessionDao loginSessionDao;
    private User user;

    @Override
    public void init() throws ServletException {
        loginSessionDao = new LoginSessionDao();
        user = new User();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();

        String ipAddress = request.getParameter("ipAddress");
        int user_id =Integer.parseInt(request.getParameter("user_id"));
        loginSessionDao.logoutStatusSession(ipAddress);

        User currentUser = null;
        int userId = 0;

        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                userId = currentUser.getId();
                List<LoginSession> lstStatusLogin = loginSessionDao.displayAllStatusLogin(userId);
                request.setAttribute("loginSessions", lstStatusLogin);
            }
        }
        if (user_id == currentUser.getId()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/frontend/nguoiB/status-login.jsp").forward(request, response);

    }
}