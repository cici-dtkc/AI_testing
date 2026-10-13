package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthorPublisherService {
    private ComicDAO comicDAO;

    public AuthorPublisherService() {
        this.comicDAO = new ComicDAO();
    }

    /**
     * Lấy thông tin tác giả và danh sách truyện
     */
    public Map<String, Object> getAuthorInfo(String authorName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy danh sách truyện của tác giả
            List<Comic> comics = comicDAO.getComicsByAuthor(authorName);

            result.put("success", true);
            result.put("authorName", authorName);
            result.put("comics", comics);
            result.put("totalComics", comics.size());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi khi lấy thông tin tác giả");
        }

        return result;
    }

    /**
     * Lấy thông tin nhà xuất bản và danh sách truyện
     */
    public Map<String, Object> getPublisherInfo(String publisherName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy danh sách truyện của nhà xuất bản
            List<Comic> comics = comicDAO.getComicsByPublisher(publisherName);

            result.put("success", true);
            result.put("publisherName", publisherName);
            result.put("comics", comics);
            result.put("totalComics", comics.size());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi khi lấy thông tin nhà xuất bản");
        }

        return result;
    }
}
