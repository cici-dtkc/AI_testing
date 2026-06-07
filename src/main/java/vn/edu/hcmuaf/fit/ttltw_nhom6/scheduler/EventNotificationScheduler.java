package vn.edu.hcmuaf.fit.ttltw_nhom6.scheduler;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class EventNotificationScheduler implements ServletContextListener {
    private ScheduledExecutorService scheduler;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        initDbRolesAndPermissions();
        String siteKey = System.getenv("RECAPTCHA_SITE_KEY");
        if (siteKey == null || siteKey.trim().isEmpty()){
            siteKey ="6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
        }
        sce.getServletContext().setAttribute("RECAPTCHA_SITE_KEY", siteKey);
        scheduler = Executors.newScheduledThreadPool(2);


        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkNewlyCreatedFlashSales();
                checkFlashSalesStartingNow();
            } catch (Exception e) {
                System.err.println("[Scheduler] Flash sale check failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkExpiringVouchers("1DAY");
                checkExpiringVouchers("1HOUR");
            } catch (Exception e) {
                System.err.println("[Scheduler] Voucher 1DAY check failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 60, TimeUnit.MINUTES);
    }

    private void initDbRolesAndPermissions() {
        try {
            org.jdbi.v3.core.Jdbi jdbi = vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector.get();
            jdbi.useHandle(handle -> {
                // 1. Chèn role admin nếu chưa tồn tại
                handle.createUpdate("INSERT INTO roles (role_name, description) " +
                        "SELECT 'admin', 'Quản trị viên thường' " +
                        "FROM DUAL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'admin')").execute();
                // 2. Chèn các permissions nếu chưa tồn tại
                String[] permissions = {
                        "manage_product", "Quản lý sản phẩm (Thêm/Sửa/Xóa)",
                        "manage_series", "Quản lý series (Thêm/Sửa/Xóa)",
                        "manage_category", "Quản lý thể loại (Thêm/Sửa/Xóa)",
                        "manage_order", "Quản lý đơn hàng (Sửa/Xóa/Cập nhật)",
                        "manage_inventory", "Quản lý kho (Nhập/Cập nhật)",
                        "manage_user", "Quản lý người dùng (Khóa/Mở khóa)",
                        "manage_flashsale", "Quản lý Flash Sale (Thêm/Sửa/Xóa)",
                        "manage_promotion", "Quản lý khuyến mãi/Voucher (Thêm/Sửa/Xóa)",
                        "manage_permissions", "Quản lý phân quyền (Gán/Override)"
                };
                for (int i = 0; i < permissions.length; i += 2) {
                    String pName = permissions[i];
                    String pDesc = permissions[i+1];
                    handle.createUpdate("INSERT INTO permissions (permission_name, description) " +
                                    "SELECT :pName, :pDesc " +
                                    "FROM DUAL " +
                                    "WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_name = :pName)")
                            .bind("pName", pName)
                            .bind("pDesc", pDesc)
                            .execute();
                }
            });
            System.out.println("[Database Init] Roles and permissions initialized successfully.");
        } catch (Exception e) {
            System.err.println("[Database Init] Failed to initialize roles/permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkExpiringVouchers(String phase) {
        VoucherDao voucherDao = new VoucherDao();
        NotificationDAO notificationDAO = NotificationDAO.getInstance();

        List<Voucher> vouchers = "1DAY".equals(phase)
                ? voucherDao.getVouchersExpiringTomorrow()
                : voucherDao.getVouchersExpiringInOneHour();

        if (vouchers.isEmpty()) return;

        List<Integer> userIds = notificationDAO.getAllActiveUserIds();

        for (Voucher v : vouchers) {
            String endDateFormatted = v.getEndDate() != null
                    ? v.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "N/A";

            String timeLabel = "1DAY".equals(phase) ? "còn 1 ngày" : "còn 1 tiếng";
            String message = "Mã giảm giá \"" + v.getCode() + "\" sắp hết hạn (" + timeLabel + ")!\n"
                    + "HSD: " + endDateFormatted + "\n"
                    + "[" + phase + "]";

            for (int userId : userIds) {
                if (!notificationDAO.existsVoucherExpiringNotif(userId, v.getCode(), phase)) {
                    vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification noti =
                            new vn.edu.hcmuaf.fit.ttltw_nhom6.model.Notification();
                    noti.setUserId(userId);
                    noti.setMessage(message);
                    noti.setType("VOUCHER_EXPIRING");
                    notificationDAO.insert(noti);
                }
            }
        }
    }

    private void checkNewlyCreatedFlashSales() {
        FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
        NotificationDAO notificationDAO = NotificationDAO.getInstance();

        List<FlashSale> newFlashSales = flashSaleDAO.getNewlyCreatedFlashSales();

        for (FlashSale fs : newFlashSales) {
            try {
                String startTime = fs.getStartTime() != null
                        ? fs.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "N/A";
                int discount = fs.getDiscountPercent() != null
                        ? fs.getDiscountPercent().intValue() : 0;

                String message = "Flash Sale \"" + fs.getName() + "\" sắp diễn ra!\n"
                        + "Bắt đầu lúc: " + startTime + "\n"
                        + "Giảm đến " + discount + "% — Đừng bỏ lỡ!";

                notificationDAO.insertForAllUsers(message, "FLASH_SALE_UPCOMING");
                flashSaleDAO.markNotifiedCreated(fs.getId());
            } catch (Exception e) {
                System.err.println("[Scheduler] Failed to notify flash sale ID: " + fs.getId());
                e.printStackTrace();
            }
        }
    }

    private void checkFlashSalesStartingNow() {
        FlashSaleDAO flashSaleDAO = new FlashSaleDAO();
        NotificationDAO notificationDAO = NotificationDAO.getInstance();

        List<FlashSale> startingNow = flashSaleDAO.getFlashSalesStartingNow();

        for (FlashSale fs : startingNow) {
            String message = "Flash Sale \"" + fs.getName() + "\" đã bắt đầu!\n"
                    + "Giảm đến " + fs.getDiscountPercent().intValue() + "%"
                    + " — Kết thúc lúc: " + fs.getEndTimeFormatted() + "\n"
                    + "Mua ngay kẻo hết!";

            notificationDAO.insertForAllUsers(message, "FLASH_SALE_STARTED");
            flashSaleDAO.markNotifiedStarted(fs.getId());
        }
    }

    private void checkExpiringVouchers() {
        VoucherDao voucherDao = new VoucherDao();
        NotificationDAO notificationDAO = NotificationDAO.getInstance();

        List<Voucher> expiringVouchers = voucherDao.getVouchersExpiringTomorrow();

        for (Voucher v : expiringVouchers) {
            String endDateFormatted = v.getEndDate() != null
                    ? v.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "N/A";

            String message = "Mã giảm giá \"" + v.getCode() + "\" sắp hết hạn!\n"
                    + "HSD: " + endDateFormatted;

            notificationDAO.insertForAllUsers(message, "VOUCHER_EXPIRING");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) scheduler.shutdown();
    }
}
