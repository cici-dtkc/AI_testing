package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/category")
public class CategoriesServlet extends HttpServlet {
    private CategoriesDao categoriesDao;
    private ComicDAO comicDAO;
    private static final int PAGE_SIZE = 12;


    @Override
    public void init() throws ServletException {
        categoriesDao = new CategoriesDao();
        comicDAO = new ComicDAO();

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        Category selectedCategory = categoriesDao.getCategoryById(categoryId);
        if (selectedCategory == null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        int page = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            try {
                page = Integer.parseInt(pageParam);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        String[] priceParams = request.getParameterValues("price");
        String[] authorParams = request.getParameterValues("author");
        String[] publisherParams = request.getParameterValues("publisher");
        String[] yearParams = request.getParameterValues("year");

        List<String> selectedPrices = priceParams != null ? Arrays.asList(priceParams) : List.of();
        List<String> selectedAuthors = authorParams != null ? Arrays.asList(authorParams) : List.of();
        List<String> selectedPublishers = publisherParams != null ? Arrays.asList(publisherParams) : List.of();
        List<String> selectedYears = yearParams != null ? Arrays.asList(yearParams) : List.of();
        boolean hasFilters = !selectedPrices.isEmpty() || !selectedAuthors.isEmpty()
                || !selectedPublishers.isEmpty() || !selectedYears.isEmpty();
        List<Comic> allComics;
        if (hasFilters) {
            allComics = comicDAO.getComicsByCategoryWithFiltersAndFlashSale(
                    categoryId, selectedPrices, selectedAuthors, selectedPublishers, selectedYears);
        } else {
            allComics = comicDAO.getComicsByCategory1WithFlashSale(categoryId);
        }

        int totalComics = allComics.size();
        int totalPages = (int) Math.ceil((double) totalComics / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalComics);
        List<Comic> comicList = (fromIndex < totalComics)
                ? allComics.subList(fromIndex, toIndex)
                : List.of();

        List<String> availableAuthors = comicDAO.getAuthorsByCategory(categoryId);
        List<String> availablePublishers = comicDAO.getPublishersByCategory(categoryId);



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

        request.setAttribute("recommendations",     recommendations);
        request.setAttribute("isPersonalized",      isPersonalized);

        request.setAttribute("selectedCategory", selectedCategory);
        request.setAttribute("comicList", comicList);
        request.setAttribute("availableAuthors", availableAuthors);
        request.setAttribute("availablePublishers", availablePublishers);
        request.setAttribute("selectedPrices", selectedPrices);
        request.setAttribute("selectedAuthors", selectedAuthors);
        request.setAttribute("selectedPublishers", selectedPublishers);
        request.setAttribute("selectedYears", selectedYears);

        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalComics", totalComics);
        request.setAttribute("pageSize", PAGE_SIZE);

        request.getRequestDispatcher("/frontend/public/CatagoryPage.jsp")
                .forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}