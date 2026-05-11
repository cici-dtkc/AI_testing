package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.InventoryTransactionDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.ComicImage;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CloudinaryService;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/products/add")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
        maxFileSize = 1024 * 1024 * 10,       // 10MB
        maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class AddComicServlet extends HttpServlet {

    private ComicDAO comicDAO;
    private CategoriesDao categoriesDao;
    private SeriesDAO seriesDAO;
    private Gson gson;
    private InventoryTransactionDAO  inventoryTransactionDAO;
    @Override
    public void init() throws ServletException {
        comicDAO = new ComicDAO();
        categoriesDao = new CategoriesDao();
        seriesDAO = new SeriesDAO();
        inventoryTransactionDAO = new InventoryTransactionDAO();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        Map<String, Object> result = new HashMap<>();

        try {
            Comic comic = extractComicFromRequest(request);
            String authorName = request.getParameter("author");
            String publisherName = request.getParameter("publisher");

            List<String> errors = validateComic(comic);
            if (!errors.isEmpty()) {
                result.put("success", false);
                result.put("message", "Dữ liệu không hợp lệ");
                result.put("errors", errors);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            if (comicDAO.isComicNameExist(comic.getNameComics(), comic.getSeriesId(), comic.getVolume())) {
                result.put("success", false);
                result.put("message", "Truyện này đã tồn tại");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Part coverPart = request.getPart("coverImage");
            if (coverPart != null && coverPart.getSize() > 0) {
                try {
                    String coverUrl = CloudinaryService.uploadImage(coverPart, "comics/covers");
                    if (coverUrl != null) {
                        comic.setThumbnailUrl(coverUrl);
                    }
                } catch (IOException e) {
                    result.put("success", false);
                    result.put("message", "Lỗi upload ảnh bìa: " + e.getMessage());
                    response.getWriter().write(gson.toJson(result));
                    return;
                }
            }

            int authorId = -1;
            int publisherId = -1;

            if (authorName != null && !authorName.trim().isEmpty()) {
                authorId = comicDAO.findOrCreateAuthor(authorName.trim());
            }

            if (publisherName != null && !publisherName.trim().isEmpty()) {
                publisherId = comicDAO.findOrCreatePublisher(publisherName.trim());
            }


            comic.setAuthor(authorName != null ? authorName.trim() : null);
            comic.setPublisher(publisherName != null ? publisherName.trim() : null);

            int comicId = comicDAO.insertComic(comic);
            if (comicId <= 0) {
                result.put("success", false);
                result.put("message", "Không thể thêm truyện");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            inventoryTransactionDAO.insertImport(comicId, comic.getStockQuantity(),"");

            if (authorId > 0) {
                comicDAO.linkComicAuthor(comicId, authorId);
            }

            if (publisherId > 0) {
                comicDAO.linkComicPublisher(comicId, publisherId);
            }

            List<ComicImage> detailImages = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                Part imagePart = request.getPart("detailImage" + i);
                if (imagePart != null && imagePart.getSize() > 0) {
                    try {
                        String imageUrl = CloudinaryService.uploadImage(imagePart, "comics/details");
                        if (imageUrl != null) {
                            ComicImage img = new ComicImage();
                            img.setComicId(comicId);
                            img.setImageUrl(imageUrl);
                            img.setImageType("detail");
                            img.setSortOrder(i);
                            detailImages.add(img);

                        }
                    } catch (IOException e) {
                    }
                }
            }

            if (!detailImages.isEmpty()) {
                comicDAO.insertComicImages(detailImages);
            }

            Map<String, Object> comicData = new HashMap<>();
            comicData.put("id", comicId);
            comicData.put("nameComics", comic.getNameComics());
            comicData.put("author", comic.getAuthor());
            comicData.put("publisher", comic.getPublisher());
            comicData.put("price", comic.getPrice());
            comicData.put("stockQuantity", comic.getStockQuantity());
            comicData.put("thumbnailUrl", comic.getThumbnailUrl());

            String categoryName = "Chưa phân loại";
            if (comic.getCategoryId() != null) {
                try {
                    List<Category> categories = categoriesDao.getAllCategories();
                    for (Category cat : categories) {
                        if (cat.getId() == comic.getCategoryId()) {
                            categoryName = cat.getNameCategories();
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not load category name: " + e.getMessage());
                }
            }
            comicData.put("categoryName", categoryName);

            String seriesName = "-";
            if (comic.getSeriesId() != null) {
                try {
                    String name = seriesDAO.getSeriesNameById1(comic.getSeriesId());
                    if (name != null && !name.isEmpty()) {
                        seriesName = name;
                    }
                } catch (Exception e) {
                    System.err.println("Could not load series name: " + e.getMessage());
                }
            }
            comicData.put("seriesName", seriesName);

            result.put("success", true);
            result.put("message", "Thêm truyện thành công");
            result.put("comicId", comicId);
            result.put("comic", comicData);


        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi: " + e.getMessage());
        }
        response.getWriter().write(gson.toJson(result));
    }

    private Comic extractComicFromRequest(HttpServletRequest request) {
        Comic comic = new Comic();

        comic.setNameComics(request.getParameter("nameComics"));
        comic.setDescription(request.getParameter("description"));

        try {
            String priceStr = request.getParameter("price");
            if (priceStr != null) {
                priceStr = priceStr.replace(",", "");
                comic.setPrice(Double.parseDouble(priceStr));
            }
        } catch (NumberFormatException e) {
            comic.setPrice(0);
        }

        try {
            comic.setStockQuantity(Integer.parseInt(request.getParameter("stockQuantity")));
        } catch (NumberFormatException e) {
            comic.setStockQuantity(0);
        }

        String categoryIdStr = request.getParameter("categoryId");
        if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
            comic.setCategoryId(Integer.parseInt(categoryIdStr));
        }

        String seriesIdStr = request.getParameter("seriesId");
        if (seriesIdStr != null && !seriesIdStr.isEmpty()) {
            comic.setSeriesId(Integer.parseInt(seriesIdStr));
        }

        String volumeStr = request.getParameter("volume");
        if (volumeStr != null && !volumeStr.isEmpty()) {
            comic.setVolume(Integer.parseInt(volumeStr));
        }

        comic.setStatus("available");

        return comic;
    }

    private List<String> validateComic(Comic comic) {
        List<String> errors = new ArrayList<>();

        if (comic.getNameComics() == null || comic.getNameComics().trim().isEmpty()) {
            errors.add("Tên truyện không được để trống");
        }

        if (comic.getPrice() <= 0) {
            errors.add("Giá phải lớn hơn 0");
        }

        if (comic.getStockQuantity() < 0) {
            errors.add("Số lượng không được âm");
        }

        if (comic.getCategoryId() == null) {
            errors.add("Vui lòng chọn thể loại");
        }

        return errors;
    }
}