// src/main/java/vn/edu/hcmuaf/fit/ttltw_nhom6/service/ComicService.java
package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ReviewDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.ComicImage;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Review;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.cache.CacheService;

import java.util.List;

public class ComicService {

    private final Jdbi jdbi;
    private final ComicDAO comicDAO;
    private SeriesDAO seriesDAO = new SeriesDAO();
    private CategoriesDao categoriesDao = new CategoriesDao();
    private ReviewDAO reviewDAO;

    public ComicService() {
        this.jdbi = JdbiConnector.get();
        this.comicDAO = new ComicDAO();
        reviewDAO = new ReviewDAO();
    }

    public ComicService(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.comicDAO = new ComicDAO();
    }

    public ComicService(Jdbi jdbi, ComicDAO comicDAO) {
        this.jdbi = jdbi;
        this.comicDAO = comicDAO;
    }


    /**
     * Lấy danh sách ảnh của truyện
     */
    public List<ComicImage> getComicImages(int comicId) {
        return comicDAO.getComicImages(comicId);
    }

    /**
     * Lấy danh sách truyện tương tự
     */
    public List<Comic> getRelatedComics(int comicId) {
        return comicDAO.getRelatedComics(comicId);
    }

    /**
     * Tính điểm đánh giá trung bình
     */
    public double getAverageRating(int comicId) {
        return comicDAO.getAverageRating(comicId);
    }


    public Comic getComicById(int comicId) {
        // 1. check cache
        if (CacheService.comicCache.containsKey(comicId)) {
            return CacheService.comicCache.get(comicId);
        }

        // 2. load DB
        Comic comic = comicDAO.getComicById(comicId);

        // 3. lưu cache
        if (comic != null) {
            CacheService.comicCache.put(comicId, comic);
        }

        return comic;
    }

    public String getSeriesName(Integer seriesId) {
        if (seriesId == null) return null;
        return seriesDAO.getSeriesNameById1(seriesId);
    }

    /**
     * Lấy danh sách review của comic (BAO GỒM ẢNH)
     */
    public List<Review> getComicReviews(int comicId) {

        List<Review> reviews = reviewDAO.getReviewsByComicId(comicId);

        for (Review review : reviews) {
            System.out.println("   Review #" + review.getId() + " has " +
                    review.getImages().size() + " images");
        }

        return reviews;
    }

    /**
     * Tìm kiếm với filter ẩn/hiện
     */
    public List<Comic> searchComicsAdminWithFilter(String keyword, String author,
                                                   Integer categoryId, Integer hiddenFilter,
                                                   int page, int limit) {
        return comicDAO.searchComicsAdminWithFilter(keyword, author, categoryId, hiddenFilter, page, limit);
    }

    /**
     * Đếm với filter ẩn/hiện
     */
    public int countComicsAdminWithFilter(String keyword, String author,
                                          Integer categoryId, Integer hiddenFilter) {
        return comicDAO.countComicsAdminWithFilter(keyword, author, categoryId, hiddenFilter);
    }

    /**
     * Lấy tất cả với filter ẩn/hiện
     */
    public List<Comic> getAllComicsAdminWithFilter(int page, int limit, Integer hiddenFilter) {
        return comicDAO.getAllComicsAdminWithFilter(page, limit, hiddenFilter);
    }

    /**
     * Đếm tất cả với filter ẩn/hiện
     */
    public int countAllComicsWithFilter(Integer hiddenFilter) {
        return comicDAO.countAllComicsWithFilter(hiddenFilter);
    }
}