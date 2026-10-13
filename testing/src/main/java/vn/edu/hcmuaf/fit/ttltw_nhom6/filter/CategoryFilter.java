package vn.edu.hcmuaf.fit.ttltw_nhom6.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;

import java.io.IOException;
import java.util.List;

@WebFilter("/*")
public class CategoryFilter implements Filter {
    private CategoriesDao categoriesDao;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        categoriesDao = new CategoriesDao();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (httpRequest.getAttribute("listCategories") == null) {
            List<Category> listCategories = categoriesDao.listCategories();
            httpRequest.setAttribute("listCategories", listCategories);
        }

        chain.doFilter(request, response);
    }
}
