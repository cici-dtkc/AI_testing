package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {
    private ComicDAO comicDAO;
    private FlashSaleDAO flashSaleDAO;
    private static final int ITEMS_PER_PAGE = 10;

    @Override
    public void init() {
        comicDAO = new ComicDAO();
        flashSaleDAO = new FlashSaleDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        flashSaleDAO.updateStatuses();

        String keyword = request.getParameter("keyword");
        String searchType = request.getParameter("searchType");

        int currentPage = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageParam);
                if (currentPage < 1) {
                    currentPage = 1;
                }
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            request.setAttribute("comics", new ArrayList<>());
            request.setAttribute("resultCount", 0);
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalComics", 0);
            request.getRequestDispatcher("/frontend/public/searchResult.jsp")
                    .forward(request, response);
            return;
        }

        List<Comic> comics = new ArrayList<>();
        switch (searchType != null ? searchType : "all") {
            case "author":
                List<Comic> authorResult = comicDAO.findByAuthorWithFlashSale(keyword);
                if (authorResult != null) comics.addAll(authorResult);
                break;

            case "publisher":
                List<Comic> publisherResult = comicDAO.findByPublisherWithFlashSale(keyword);
                if (publisherResult != null) comics.addAll(publisherResult);
                break;

            case "category":
                List<Comic> categoryResult = comicDAO.findByCategoryWithFlashSale(keyword);
                if (categoryResult != null) comics.addAll(categoryResult);
                break;

            case "name":
                List<Comic> nameResult = comicDAO.smartSearchWithFlashSale(keyword);
                if (nameResult != null) comics.addAll(nameResult);
                break;

            default:
                List<Comic> allResults = comicDAO.smartSearchAllWithFlashSale(keyword);
                if (allResults != null) comics.addAll(allResults);
                break;
        }

        if (!comics.isEmpty()) {
            Set<Integer> addedIds = new HashSet<>();
            comics.forEach(c -> addedIds.add(c.getId()));

            Comic first = comics.get(0);

            if (first.getSeriesId() != null) {
                List<Comic> sameSeries = comicDAO.findBySeriesIdWithFlashSale(
                        first.getSeriesId(),
                        first.getId()
                );

                if (sameSeries != null) {
                    for (Comic c : sameSeries) {
                        if (addedIds.add(c.getId())) {
                            comics.add(c);
                        }
                    }
                }
            }
        }

        int totalComics = comics.size();
        int totalPages = (int) Math.ceil((double) totalComics / ITEMS_PER_PAGE);

        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }

        List<Comic> comicsForPage;
        if (totalComics > 0) {
            int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalComics);
            comicsForPage = comics.subList(startIndex, endIndex);
        } else {
            comicsForPage = new ArrayList<>();
        }

        request.setAttribute("keyword", keyword);
        request.setAttribute("searchType", searchType != null ? searchType : "all");
        request.setAttribute("comics", comicsForPage);
        request.setAttribute("resultCount", comicsForPage.size());
        request.setAttribute("totalComics", totalComics);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);

        request.getRequestDispatcher("/frontend/public/searchResult.jsp")
                .forward(request, response);
    }
}