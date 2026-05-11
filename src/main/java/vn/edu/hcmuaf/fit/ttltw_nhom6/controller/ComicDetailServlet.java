package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ReviewDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.WishlistDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Series;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ComicService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.RecommendationService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/comic-detail")
public class ComicDetailServlet extends HttpServlet {
    private ComicService comicService;
    private RecommendationService recommendationService;
    private WishlistDAO wishlistDAO;
    private SeriesDAO seriesDAO;
    private ComicDAO comicDAO;

    @Override
    public void init() throws ServletException {
        comicService = new ComicService();
        recommendationService = new RecommendationService();
        wishlistDAO = new WishlistDAO();
        seriesDAO = new SeriesDAO();
        comicDAO = new ComicDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String idParam = request.getParameter("id");
            if (idParam == null || idParam.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/home");
                return;
            }

            int comicId = Integer.parseInt(idParam);

            Comic comic = comicService.getComicById(comicId);

            if (comic == null) {
                response.sendRedirect(request.getContextPath() + "/home");
                return;
            }
            int totalSell = comicDAO.getTotalSoldByComicId(comic.getId());
            var images = comicService.getComicImages(comicId);
            var relatedComics = comicService.getRelatedComics(comicId);
            var reviews = comicService.getComicReviews(comicId);
            double avgRating = comicService.getAverageRating(comicId);
            int availableComic = comicDAO.available(comicId);

            ReviewDAO reviewDAO = new ReviewDAO();
            Map<Integer, Integer> ratingDistribution = reviewDAO.getRatingDistribution(comicId);
            int totalReviews = reviews.size();

            String seriesName = null;
            if (comic.getSeriesId() != null && comic.getSeriesId() > 0) {
                try {
                    seriesName = comicService.getSeriesName(comic.getSeriesId());
                } catch (Exception e) {

                    try {
                        Optional<Series> seriesOpt = seriesDAO.getSeriesById(comic.getSeriesId());
                        if (seriesOpt.isPresent()) {
                            seriesName = seriesOpt.get().getSeriesName();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (seriesName == null && comic.getSeriesName() != null) {
                    seriesName = comic.getSeriesName();
                }
            }

            User currentUser = (User) request.getSession().getAttribute("currentUser");
            Integer userId = (currentUser != null) ? currentUser.getId() : null;

            List<Comic> suggestedComics = recommendationService.getDetailPageSuggestions(
                    userId,
                    comicId,
                    24
            );

            String suggestionType = recommendationService.getSuggestionType(userId, comicId);

            if (suggestedComics.isEmpty()) {
                suggestedComics = recommendationService.getRecommendations(null, 24);
                suggestionType = "popular";
            }

            request.setAttribute("seriesName", seriesName);
            request.setAttribute("comic", comic);
            request.setAttribute("images", images);
            request.setAttribute("relatedComics", relatedComics);
            request.setAttribute("reviews", reviews);
            request.setAttribute("avgRating", avgRating);
            request.setAttribute("ratingDistribution", ratingDistribution);
            request.setAttribute("totalReviews", totalReviews);
            request.setAttribute("suggestedComics", suggestedComics);
            request.setAttribute("suggestionType", suggestionType);
            request.setAttribute("totalSell", totalSell);
            request.setAttribute("availableComic", availableComic);


            request.getRequestDispatcher("/frontend/public/detail.jsp")
                    .forward(request, response);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}