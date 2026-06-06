package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import vn.edu.hcmuaf.fit.ttltw_nhom6.model.FlashSale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FlashSaleDAO extends ADao {
    public FlashSale getActiveFlashSaleEndingSoon() {
        String sql = """
                    SELECT *
                    FROM FlashSale
                    WHERE status = 'active'
                      AND start_time <= NOW()
                      AND end_time >= NOW()
                      AND is_deleted = 0
                    ORDER BY end_time ASC
                    LIMIT 1
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(FlashSale.class)
                        .findOne()
                        .orElse(null)
        );
    }


    public int insert(FlashSale flashSale) {
        String sql = """
                    INSERT INTO FlashSale (name, discount_percent, start_time, end_time, status)
                    VALUES (:name, :discountPercent, :startTime, :endTime, :status)
                """;

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bindBean(flashSale)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(int.class)
                        .one()
        );
    }

    public List<FlashSale> getAllFlashSales() {

        updateStatuses();

        String sql = """
                    SELECT id, name, discount_percent, start_time, end_time, status, created_at
                    FROM FlashSale
                    WHERE is_deleted = 0
                    ORDER BY created_at ASC
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }

    public boolean deleteById(int id) {
        String sql = "UPDATE FlashSale SET is_deleted = 1 WHERE id = ?";

        return jdbi.withHandle(handle ->
            handle.createUpdate(sql).bind(0, id).execute() > 0
        );
    }

    public FlashSale getById(int id) {

        updateStatuses();

        String sql = "SELECT * FROM FlashSale WHERE id = ?";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, id)
                        .mapToBean(FlashSale.class)
                        .findOne()
                        .orElse(null)
        );
    }

    public boolean updateFlashSale(int id, String name, double discountPercent, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
                    UPDATE FlashSale 
                    SET name = :name,
                        discount_percent = :discountPercent,
                        start_time = :startTime,
                        end_time = :endTime
                    WHERE id = :id
                """;

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("name", name)
                        .bind("discountPercent", discountPercent)
                        .bind("startTime", startTime)
                        .bind("endTime", endTime)
                        .bind("id", id)
                        .execute() > 0
        );
    }

    public void updateStatuses() {
        String sql = """
                    UPDATE FlashSale
                    SET status = CASE
                        WHEN NOW() < start_time THEN 'scheduled'
                        WHEN NOW() >= start_time AND NOW() <= end_time THEN 'active'
                        WHEN NOW() > end_time THEN 'ended'
                    END
                    WHERE status != CASE
                        WHEN NOW() < start_time THEN 'scheduled'
                        WHEN NOW() >= start_time AND NOW() <= end_time THEN 'active'
                        WHEN NOW() > end_time THEN 'ended'
                    END
                """;

        jdbi.useHandle(handle -> handle.createUpdate(sql).execute());
    }

    /**
     * Lấy danh sách Flash Sale đang active và sắp diễn ra
     * Sắp xếp theo thời gian bắt đầu
     */
    public List<FlashSale> getUpcomingAndActiveFlashSales() {

        updateStatuses();

        String sql = """
                    SELECT *
                    FROM FlashSale
                    WHERE status IN ('active', 'scheduled') 
                    AND is_deleted = 0
                    ORDER BY start_time ASC
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }

    /**
     * Lấy Flash Sale theo trạng thái
     */
    public List<FlashSale> getFlashSalesByStatus(String status) {
        updateStatuses();

        String sql = """
        SELECT *
        FROM FlashSale
        WHERE status = :status
        AND is_deleted = 0
        ORDER BY start_time ASC
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("status", status)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }

    public List<Map<String, Object>> getFlashSaleStatistics() {
        updateStatuses();

        String sql = """
        SELECT 
            fs.id,
            fs.name,
            fs.start_time,
            fs.end_time,
            fs.discount_percent,
            fs.status,
            COUNT(DISTINCT o.id)                              AS total_orders,
            COALESCE(SUM(oi.quantity), 0)                    AS total_items_sold,
            COALESCE(SUM(oi.quantity * oi.price_at_purchase), 0) AS total_revenue
        FROM FlashSale fs
        LEFT JOIN order_items oi ON oi.flashsale_id = fs.id
        LEFT JOIN orders o ON o.id = oi.order_id
            AND o.status NOT IN ('Cancelled', 'Returned')
        GROUP BY fs.id, fs.name, fs.start_time, fs.end_time, 
                 fs.discount_percent, fs.status
        ORDER BY fs.start_time DESC
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql).mapToMap().list()
        );
    }

    public List<FlashSale> getNewlyCreatedFlashSales() {
        String sql = """
        SELECT id, name, start_time, end_time, discount_percent,
               status, created_at, is_deleted,
               notified_created, notified_started
        FROM flashsale
        WHERE notified_created = 0
          AND is_deleted = 0
          AND end_time > NOW()
        """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }

    public List<FlashSale> getFlashSalesStartingNow() {
        String sql = """
        SELECT id, name, start_time, end_time, discount_percent,
               status, created_at, is_deleted,
               notified_created, notified_started
        FROM flashsale
        WHERE notified_started = 0
          AND status = 'active'
          AND start_time <= NOW()
          AND end_time > NOW()
          AND is_deleted = 0
        """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }

    public void markNotifiedCreated(int flashSaleId) {
        jdbi.useHandle(handle ->
                handle.createUpdate("UPDATE flashsale SET notified_created = 1 WHERE id = ?")
                        .bind(0, flashSaleId)
                        .execute()
        );
    }

    public void markNotifiedStarted(int flashSaleId) {
        jdbi.useHandle(handle ->
                handle.createUpdate("UPDATE flashsale SET notified_started = 1 WHERE id = ?")
                        .bind(0, flashSaleId)
                        .execute()
        );
    }

    public List<FlashSale> getFlashSalesPaginated(int page, int pageSize) {
        updateStatuses();
        int offset = (page - 1) * pageSize;
        String sql = """
                    SELECT id, name, discount_percent, start_time, end_time, status, created_at
                    FROM FlashSale
                    WHERE is_deleted = 0
                    ORDER BY created_at DESC
                    LIMIT :limit OFFSET :offset
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("limit", pageSize)
                        .bind("offset", offset)
                        .mapToBean(FlashSale.class)
                        .list()
        );
    }
    public int countFlashSales() {
        String sql = "SELECT COUNT(*) FROM FlashSale WHERE is_deleted = 0";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapTo(Integer.class)
                        .one()
        );
    }

}
