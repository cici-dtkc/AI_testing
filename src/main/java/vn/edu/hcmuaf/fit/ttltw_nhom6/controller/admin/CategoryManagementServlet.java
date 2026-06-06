package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;

import java.io.IOException;
@WebServlet({"/admin/CategoryManagement", "/admin/categories/list"})
public class CategoryManagementServlet extends HttpServlet {
    private CategoriesDao categoriesDao;

    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equals(requestedWith)
                || (accept != null && accept.contains("application/json"));
        if (isAjax) {
            response.setContentType("application/json; charset=UTF-8");
            try {
                var categories = categoriesDao.getAllCategories();
                StringBuilder json = new StringBuilder("{\"success\":true,\"categories\":[");
                for (int i = 0; i < categories.size(); i++) {
                    var cat = categories.get(i);
                    if (i > 0) json.append(",");
                    json.append("{\"id\":").append(cat.getId())
                            .append(",\"nameCategories\":\"")
                            .append(cat.getNameCategories().replace("\"", "\\\""))
                            .append("\"}");
                }
                json.append("]}");
                response.getWriter().write(json.toString());
            } catch (Exception e) {
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
            return;
        }
        try {
            String successMessage = (String) request.getSession().getAttribute("successMessage");
            String errorMessage = (String) request.getSession().getAttribute("errorMessage");

            if (successMessage != null) {
                request.setAttribute("successMessage", successMessage);
                request.getSession().removeAttribute("successMessage");
            }
            if (errorMessage != null) {
                request.setAttribute("errorMessage", errorMessage);
                request.getSession().removeAttribute("errorMessage");
            }
            request.setAttribute("loadedFromServlet", true);
            request.getRequestDispatcher("/frontend/admin/category.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            request.getRequestDispatcher("/frontend/admin/category.jsp").forward(request, response);
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        String action = request.getParameter("action"); // "show" hoặc "hide
        if (idParam != null && action != null) {
            try {
                int id = Integer.parseInt(idParam);
                int hidden = "hide".equals(action) ? 1 : 0;

                boolean success = categoriesDao.toggleHidden(id, hidden);

                response.setContentType("application/json; charset=UTF-8");
                if (success) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"success\": true}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"success\": false}");
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
                return;
            }
        }
        doGet(request, response);
    }
}