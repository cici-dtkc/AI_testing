package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.pubilc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Banner;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ComicService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.FlashSaleService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.RecommendationService;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
@WebServlet({"/", "/home"})
public class HomeServlet extends HttpServlet {
    private ComicDAO comicDAO;
    private FlashSaleDAO flashSaleDAO;
    private FlashSaleComicsDAO flashSaleComicsDAO;
    private FlashSaleService flashSaleService;
    private ComicService comicService;
    private BannerDao bannerDao;
    private WishlistDAO wishlistDAO;
    private RecommendationService recommendationService;

    @Override
    public void init() throws ServletException {
        comicDAO = new ComicDAO();
        flashSaleDAO = new FlashSaleDAO();
        flashSaleComicsDAO = new FlashSaleComicsDAO();
        flashSaleService = new FlashSaleService();
        comicService = new ComicService(JdbiConnector.get());
        bannerDao = new BannerDao(JdbiConnector.get());
        wishlistDAO = new WishlistDAO();
        recommendationService = new RecommendationService(comicDAO, wishlistDAO);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            flashSaleDAO.updateStatuses();

            HttpSession session = request.getSession(false);
            User currentUser = null;
            Integer userId = null;

            if (session != null) {
                currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null) {
                    userId = currentUser.getId();
                    System.out.println("✓ Found user from session");
                    System.out.println("  - User ID: " + userId);
                    System.out.println("  - Username: " + currentUser.getUsername());
                }
            }

            FlashSale activeFlashSale = flashSaleDAO.getActiveFlashSaleEndingSoon();
            if (activeFlashSale != null) {
                request.setAttribute("flashSale", activeFlashSale);
                long endTimeMillis = activeFlashSale.getEndTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                request.setAttribute("flashSaleEndTimeMillis", endTimeMillis);

                // Lấy 4 comics đầu tiên từ Flash Sale
                List<Map<String, Object>> flashSaleComics =
                        flashSaleService.getExclusiveComicsForFlashSale(activeFlashSale.getId());

                // Chỉ lấy 4 sản phẩm đầu cho homepage
                if (flashSaleComics.size() > 4) {
                    flashSaleComics = flashSaleComics.subList(0, 4);
                }
                request.setAttribute("flashSaleComics", flashSaleComics);
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            List<Banner> banners = bannerDao.findActiveBanners(now);
            request.setAttribute("banners", banners);

            List<Comic> recommendedComics;

            if (currentUser != null) {
                int wishlistCount = wishlistDAO.getWishlistCount(currentUser.getId());

                if (wishlistCount > 0) {
                    // User có wishlist -> Gợi ý dựa trên wishlist
                    recommendedComics = comicDAO.getRecommendedComics(currentUser.getId(), 16);
                } else {
                    // User không có wishlist -> Hiển thị phổ biến
                    recommendedComics = comicDAO.getPopularComics(16);
                }
            } else {
                // Guest user -> Hiển thị phổ biến
                recommendedComics = comicDAO.getPopularComics(16);
            }

            request.setAttribute("recommendedComics", recommendedComics);
            request.setAttribute("isLoggedIn", currentUser != null);

            List<Comic> topComics = comicDAO.getTop5BestSellerThisWeek();
            request.setAttribute("topComics", topComics);
            request.getRequestDispatcher("/frontend/public/homePage.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();

            request.setAttribute("errorMessage", "Lỗi khi tải trang chủ: " + e.getMessage());
            request.getRequestDispatcher("/Error/error404.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}