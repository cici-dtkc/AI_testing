package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.io.IOException;
import java.util.*;

@WebServlet("/userCategory")
public class UserCategoryServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;

    private CategoriesDao categoriesDao;
    private ComicDAO comicDAO;

    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
        comicDAO = new ComicDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        int categoryId;
        try {
            categoryId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }


        int currentPage = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageParam);
                if (currentPage < 1) currentPage = 1;
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }

        Category selectedCategory = categoriesDao.getCategoryById(categoryId);
        if (selectedCategory == null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        String[] priceArr     = request.getParameterValues("price");
        String[] authorArr    = request.getParameterValues("author");
        String[] publisherArr = request.getParameterValues("publisher");
        String[] yearArr      = request.getParameterValues("year");

        List<String> selectedPrices     = priceArr     != null ? Arrays.asList(priceArr)     : new ArrayList<>();
        List<String> selectedAuthors    = authorArr    != null ? Arrays.asList(authorArr)    : new ArrayList<>();
        List<String> selectedPublishers = publisherArr != null ? Arrays.asList(publisherArr) : new ArrayList<>();
        List<String> selectedYears      = yearArr      != null ? Arrays.asList(yearArr)      : new ArrayList<>();


        List<String> availableAuthors    = comicDAO.getAuthorsByCategory(categoryId);
        List<String> availablePublishers = comicDAO.getPublishersByCategory(categoryId);


        List<Comic> allComics = comicDAO.getComicsByCategoryWithFiltersAndFlashSale(
                categoryId,
                selectedPrices.isEmpty()     ? null : selectedPrices,
                selectedAuthors.isEmpty()    ? null : selectedAuthors,
                selectedPublishers.isEmpty() ? null : selectedPublishers,
                selectedYears.isEmpty()      ? null : selectedYears
        );

        int totalComics = allComics.size();
        int totalPages  = Math.max(1, (int) Math.ceil((double) totalComics / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, totalComics);
        List<Comic> comicList = fromIndex < totalComics
                ? new ArrayList<>(allComics.subList(fromIndex, toIndex))
                : new ArrayList<>();

        HttpSession session = request.getSession(false);
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        List<Comic> recommendedComics;
        boolean isPersonalized = false;

        if (userId != null) {
            recommendedComics = comicDAO.getRecommendedComicsWithFlashSale(userId, 12);
            isPersonalized    = !recommendedComics.isEmpty();
            if (recommendedComics.isEmpty()) {
                recommendedComics = comicDAO.getPopularComicsWithFlashSale(12);
            }
        } else {
            recommendedComics = comicDAO.getPopularComicsWithFlashSale(12);
        }

        Map<String, List<Comic>> recommendations = new LinkedHashMap<>();
        if (!recommendedComics.isEmpty()) {
            recommendations.put(
                    isPersonalized ? "Dành riêng cho bạn" : "Có thể bạn thích",
                    recommendedComics
            );
        }

        request.setAttribute("selectedCategory",   selectedCategory);
        request.setAttribute("comicList",           comicList);
        request.setAttribute("totalComics",         totalComics);
        request.setAttribute("currentPage",         currentPage);
        request.setAttribute("totalPages",          totalPages);
        request.setAttribute("pageSize",            PAGE_SIZE);
        request.setAttribute("selectedPrices",      selectedPrices);
        request.setAttribute("selectedAuthors",     selectedAuthors);
        request.setAttribute("selectedPublishers",  selectedPublishers);
        request.setAttribute("selectedYears",       selectedYears);
        request.setAttribute("availableAuthors",    availableAuthors);
        request.setAttribute("availablePublishers", availablePublishers);
        request.setAttribute("recommendations",     recommendations);
        request.setAttribute("isPersonalized",      isPersonalized);

        request.getRequestDispatcher("/frontend/public/CatagoryPage.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}