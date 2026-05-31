package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.LoginSessionDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.LoginSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;

@WebServlet("/status-login")
public class StatusLogin extends HttpServlet {
    LoginSessionDao  loginSessionDao;
    User user;

    @Override
    public void init() throws ServletException {
      loginSessionDao = new LoginSessionDao();
        user = new User();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
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

        request.getRequestDispatcher("/frontend/nguoiB/status-login.jsp").forward(request, response);

    }
}