package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.UserB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.OrderDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ReviewDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.OrderItem;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Review;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.CloudinaryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/submit-review")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
        maxFileSize = 1024 * 1024 * 10,       // 10MB
        maxRequestSize = 1024 * 1024 * 50     // 50MB
)
public class SubmitReviewServlet extends HttpServlet {
    private ReviewDAO reviewDAO;
    private OrderDAO orderDAO;

    @Override
    public void init() throws ServletException {
        reviewDAO = new ReviewDAO();
        orderDAO = new OrderDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập\"}");
            return;
        }

        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int rating = Integer.parseInt(request.getParameter("rating"));
            String comment = request.getParameter("comment");

            // Kiểm tra đã review chưa
            if (reviewDAO.hasUserReviewedOrder(user.getId(), orderId)) {
                response.getWriter().write("{\"success\":false,\"message\":\"Bạn đã đánh giá đơn hàng này rồi\"}");
                return;
            }

            // Lấy tất cả comic trong order
            List<OrderItem> items = orderDAO.getOrderItems(orderId);

            if (items.isEmpty()) {
                response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy sản phẩm trong đơn hàng\"}");
                return;
            }

            // Lấy danh sách ảnh từ request
            List<String> uploadedImageUrls = new ArrayList<>();

            for (Part part : request.getParts()) {
                String partName = part.getName();

                if ("images".equals(partName) && part.getSize() > 0) {
                    try {
                        String imageUrl = CloudinaryService.uploadImage(part, "reviews");
                        if (imageUrl != null) {
                            uploadedImageUrls.add(imageUrl);
                        }
                    } catch (Exception e) {
                    }
                }
            }

            for (OrderItem item : items) {
                Review review = new Review();
                review.setComicId(item.getComicId());
                review.setUserId(user.getId());
                review.setRating(rating);
                review.setComment(comment);
                review.setOrderId(orderId);

                int reviewId = reviewDAO.addReview(review);

                for (String imageUrl : uploadedImageUrls) {
                    boolean added = reviewDAO.addReviewImage(reviewId, imageUrl);
                }
            }
            response.getWriter().write("{\"success\":true,\"message\":\"Đánh giá thành công\"}");

        } catch (NumberFormatException e) {
            response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu không hợp lệ\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"message\":\"Có lỗi xảy ra: " + e.getMessage() + "\"}");
        }
    }
}