package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.PointTransactionDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.PointTransaction;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.io.IOException;
import java.util.List;


@WebServlet("/pointHistory")


public class PointHistoryServlet extends HttpServlet {
    private UserDao userDao;
    private PointTransactionDAO  pointTransactionDao;

    private JdbiConnector jdbiConnector;
    @Override
    public void init() throws ServletException {
        userDao = new UserDao(JdbiConnector.get());
        pointTransactionDao = new PointTransactionDAO(JdbiConnector.get());
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user =(User) request.getSession().getAttribute("currentUser");
        if (user == null) {
            response.sendRedirect("/frontend/public/login.jsp");
            return;
        }
        System.out.println(user.getId());
        int point = userDao.userPoint(user.getId());
        request.setAttribute("point", point);

        List<PointTransaction> pointHistory  = pointTransactionDao.getTransactionsByUserId(user.getId());
        request.setAttribute("pointHistory", pointHistory);

        request.getRequestDispatcher("/frontend/nguoiB/points.jsp").forward(request, response);

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}