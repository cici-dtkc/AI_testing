package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;

import java.io.IOException;
import java.util.List;

@WebServlet("/header")
public class HeaderServlet extends HttpServlet {
    private CategoriesDao categoriesDao;

    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
             //cái trên là load cái  số thể loại thôi đó nghe
            List<Category> listCategories = categoriesDao.listCategories();
            request.setAttribute("listCategories", listCategories);
            request.getRequestDispatcher("/home").forward(request, response);


    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}