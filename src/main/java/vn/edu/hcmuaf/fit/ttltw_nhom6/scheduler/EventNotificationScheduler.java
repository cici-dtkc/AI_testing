package vn.edu.hcmuaf.fit.ttltw_nhom6.scheduler;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.NotificationDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
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
                checkExpiringVouchers();
            } catch (Exception e) {
                System.err.println("[Scheduler] Voucher check failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1440, TimeUnit.MINUTES);
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
