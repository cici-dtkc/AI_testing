package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import java.util.List;
import java.util.Map;

public class FlashSaleComicsDAO extends ADao {

    public void insertLinks(int flashSaleId, List<Integer> comicIds) {
        if (comicIds == null || comicIds.isEmpty()) return;

        String sql = "INSERT IGNORE INTO flashsale_comics (flashsale_id, comic_id) VALUES (?, ?)";

        jdbi.useHandle(handle -> {
            try (var batch = handle.prepareBatch(sql)) {
                for (int comicId : comicIds) {
                    batch.bind(0, flashSaleId)
                            .bind(1, comicId)
                            .add();
                }
                batch.execute();
            }
        });
    }

    public List<Map<String, Object>> getComicsByFlashSaleId(int flashSaleId) {
        String sql = """
        SELECT 
            c.id, 
            c.name_comics AS name,
            c.thumbnail_url AS image_url,
            c.price AS original_price,
            ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_price,
            fs.discount_percent,
            fs.id AS flashsale_id,
            fs.name AS flashsale_name,
            COALESCE(
                (SELECT SUM(oi.quantity) 
                 FROM order_items oi
                 JOIN orders o ON oi.order_id = o.id
                 WHERE oi.comic_id = c.id 
                   AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                0
            ) AS sold_count,
            c.stock_quantity AS stock
        FROM flashsale_comics fsc
        JOIN comics c ON fsc.comic_id = c.id
        JOIN flashsale fs ON fsc.flashsale_id = fs.id
        WHERE fsc.flashsale_id = ?
          AND c.stock_quantity > 0
          AND c.is_deleted = 0
          AND c.is_hidden = 0
        ORDER BY c.name_comics
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, flashSaleId)
                        .mapToMap()
                        .list()
        );
    }
    public void deleteLinksByFlashSaleId(int flashSaleId) {
        String sql = "DELETE FROM flashsale_comics WHERE flashsale_id = ?";

        jdbi.useHandle(handle -> {
            handle.createUpdate(sql)
                    .bind(0, flashSaleId)
                    .execute();
        });
    }


    /**
     * Lấy thông tin Flash Sale của một comic (nếu có)
     * Dùng để áp dụng giá Flash Sale khi thêm vào giỏ hàng
     * trả về Flash Sale có discount cao nhất
     */
    public Map<String, Object> getFlashSaleInfoByComicId(int comicId) {
        String sql = """
        SELECT 
            fs.id AS flashsale_id,
            fs.name AS flashsale_name,
            fs.discount_percent,
            fs.status,
            fs.start_time,
            fs.end_time
        FROM flashsale_comics fsc
        JOIN flashsale fs ON fsc.flashsale_id = fs.id
        WHERE fsc.comic_id = ?
          AND fs.status = 'active'
          AND fs.start_time <= NOW()
          AND fs.end_time >= NOW()
        ORDER BY fs.discount_percent DESC
        LIMIT 1
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, comicId)
                        .mapToMap()
                        .findOne()
                        .orElse(null)
        );
    }

    /**
     * Lấy danh sách comics CHỈ THUỘC VỀ Flash Sale có discount cao nhất
     * Loại trừ những comic đã được hiển thị ở Flash Sale khác có discount cao hơn
     */
    public List<Map<String, Object>> getExclusiveComicsForFlashSale(int flashSaleId) {
        String sql = """
        WITH ComicFlashSaleRanking AS (
            SELECT 
                c.id AS comic_id,
                c.name_comics AS name,
                c.thumbnail_url AS image_url,
                c.price AS original_price,
                c.stock_quantity AS stock,
                fs.id AS flashsale_id,
                fs.name AS flashsale_name,
                fs.discount_percent,
                ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_price,
                COALESCE(
                    (SELECT SUM(oi.quantity) 
                     FROM order_items oi
                     JOIN orders o ON oi.order_id = o.id
                     WHERE oi.comic_id = c.id 
                       AND o.status NOT IN ('cancelled', 'returned')),
                    0
                ) AS sold_count,
                ROW_NUMBER() OVER (
                    PARTITION BY c.id 
                    ORDER BY fs.discount_percent DESC, fs.end_time ASC, fs.id ASC
                ) AS rn
            FROM flashsale_comics fsc
            JOIN comics c ON fsc.comic_id = c.id
            JOIN flashsale fs ON fsc.flashsale_id = fs.id
            WHERE fs.status = 'active'
              AND fs.start_time <= NOW()
              AND fs.end_time >= NOW()
              AND c.stock_quantity > 0
              AND c.is_deleted = 0
              AND c.is_hidden = 0
        )
        SELECT 
            comic_id AS id,
            name,
            image_url,
            original_price,
            flash_price,
            discount_percent,
            flashsale_id,
            flashsale_name,
            sold_count,
            stock
        FROM ComicFlashSaleRanking
        WHERE rn = 1 
          AND flashsale_id = ?
        ORDER BY name
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, flashSaleId)
                        .mapToMap()
                        .list()
        );
    }

    /**
     * Kiểm tra xem một comic có thuộc về Flash Sale nào có discount cao hơn không
     */
    public Integer getBestFlashSaleIdForComic(int comicId) {
        String sql = """
        SELECT fs.id
        FROM flashsale_comics fsc
        JOIN flashsale fs ON fsc.flashsale_id = fs.id
        WHERE fsc.comic_id = ?
          AND fs.status = 'active'
          AND fs.start_time <= NOW()
          AND fs.end_time >= NOW()
        ORDER BY fs.discount_percent DESC, fs.end_time ASC, fs.id ASC
        LIMIT 1
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, comicId)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(null)
        );
    }

    /**
     * Lấy tất cả comics unique (mỗi comic chỉ xuất hiện 1 lần)
     * Thuộc về Flash Sale có discount cao nhất
     */
    public List<Map<String, Object>> getUniqueComicsWithBestDiscount() {
        String sql = """
        WITH ComicBestFlashSale AS (
            SELECT 
                c.id AS comic_id,
                c.name_comics AS name,
                c.thumbnail_url AS image_url,
                c.price AS original_price,
                c.stock_quantity AS stock,
                fs.id AS flashsale_id,
                fs.name AS flashsale_name,
                fs.discount_percent,
                fs.end_time,
                ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_price,
                COALESCE(
                    (SELECT SUM(oi.quantity) 
                     FROM order_items oi
                     JOIN orders o ON oi.order_id = o.id
                     WHERE oi.comic_id = c.id 
                       AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                    0
                ) AS sold_count,
                ROW_NUMBER() OVER (
                    PARTITION BY c.id 
                    ORDER BY fs.discount_percent DESC, fs.end_time ASC, fs.id ASC
                ) AS rn
            FROM flashsale_comics fsc
            JOIN comics c ON fsc.comic_id = c.id
            JOIN flashsale fs ON fsc.flashsale_id = fs.id
            WHERE fs.status = 'active'
              AND fs.start_time <= NOW()
              AND fs.end_time >= NOW()
              AND c.stock_quantity > 0
              AND c.is_deleted = 0
              AND c.is_hidden = 0
        )
        SELECT 
            comic_id AS id,
            name,
            image_url,
            original_price,
            flash_price,
            discount_percent,
            flashsale_id,
            flashsale_name,
            sold_count,
            stock
        FROM ComicBestFlashSale
        WHERE rn = 1
        ORDER BY name
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToMap()
                        .list()
        );
    }

}