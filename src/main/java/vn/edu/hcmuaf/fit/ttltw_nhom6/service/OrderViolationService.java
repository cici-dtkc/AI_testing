package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.OrderDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.OrderHistoryDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.User;

import java.util.Optional;

public class OrderViolationService {
    private static OrderViolationService instance;
    private static final int VIOLATION_THRESHOLD = 5;

    private OrderViolationService() {
    }

    public static OrderViolationService getInstance() {
        if (instance == null) {
            instance = new OrderViolationService();
        }
        return instance;
    }

    /**
     * Kiểm tra user có hủy quá nhiều đơn trong 1 giờ không
     * Nếu có, thông báo cho admin
     */
    public void checkCancelViolation(int userId) {

        int cancelCount = OrderHistoryDAO.countUserCancelledOrdersLastHour(userId);

        if (cancelCount >= VIOLATION_THRESHOLD) {
            User user = UserDao.getInstance().getUserById(userId);
            String username = user != null ? user.getFullName() : "User #" + userId;

            UserDao.getInstance().lockUserTemporary(userId, 24 * 60);

            NotificationService.getInstance().notifyAdminUserViolation(
                    userId,
                    username,
                    "HỦY ĐƠN HÀNG NHIỀU LẦN",
                    String.format("Đã hủy %d đơn hàng trong vòng 1 giờ. Tài khoản tự động khóa 24 giờ.", cancelCount)
            );
        }
    }

    /**
     * Kiểm tra đăng nhập thất bại nhiều lần
     */
    public void checkLoginFailureViolation(String username) {
        Optional<User> userOpt = UserDao.getInstance().findByUsernameOrEmail(username);
        if (!userOpt.isPresent()) return;

        User user = userOpt.get();
        int failCount = user.getFailedLoginAttempts();

        if (failCount >= VIOLATION_THRESHOLD) {
            // Tự động khóa 15 phút
            UserDao.getInstance().lockUserTemporary(user.getId(), 15);
            UserDao.getInstance().resetFailedLoginAttempts(user.getId());

            // thông báo admin
            NotificationService.getInstance().notifyAdminUserViolation(
                    user.getId(),
                    user.getFullName() != null ? user.getFullName() : username,
                    "ĐĂNG NHẬP THẤT BẠI NHIỀU LẦN",
                    "Đã tự động khóa tài khoản 15 phút do đăng nhập thất bại 5 lần liên tiếp."
            );
        }
    }

    /**
     * Reset số lần đăng nhập thất bại về 0 khi đăng nhập thành công
     *
     * @param userId ID của user
     */
    public void resetLoginFailureCount(int userId) {
        UserDao.getInstance().resetFailedLoginAttempts(userId);
    }

    /**
     * Tăng số lần đăng nhập thất bại lên 1
     */
    public void incrementLoginFailureCount(String username) {
        Optional<User> userOpt = UserDao.getInstance().findByUsernameOrEmail(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserDao.getInstance().incrementFailedLoginAttempts(user.getId());
        }
    }
}
