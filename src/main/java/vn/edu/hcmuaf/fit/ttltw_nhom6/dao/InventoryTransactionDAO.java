package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import java.util.List;
import java.util.Map;

public class InventoryTransactionDAO {
    private final Jdbi jdbi;

    public InventoryTransactionDAO() {
        this.jdbi = JdbiConnector.get();
    }

    /**
     * Inserts an import record. (Used by AddComicServlet)
     */
    public boolean insertImport(int comic_id, int quantity, String note) {
        String sql = "INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                "VALUES (:comic_id, 'IMPORT', :quantity, :note, NOW());";

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("comic_id", comic_id)
                        .bind("quantity", quantity)
                        .bind("note", note)
                        .execute()) > 0;
    }

    /**
     * Updates the stock quantity of a comic (adds/subtracts quantity).
     */
    public void updateStockQuantity(int comic_id, int quantity) {
        String sql = "UPDATE comics SET stock_quantity = stock_quantity + :quantity WHERE id = :comic_id;";
        jdbi.useHandle(handle ->
                handle.createUpdate(sql)
                        .bind("quantity", quantity)
                        .bind("comic_id", comic_id)
                        .execute());
    }

    /**
     * Inserts an export record.
     */
    public void insertExport(int comic_id, String type, int quantity, String note) {
        String sql = "INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                "VALUES (:comic_id, :type, :quantity, :note, NOW());";
        jdbi.useHandle(handle ->
                handle.createUpdate(sql)
                        .bind("comic_id", comic_id)
                        .bind("type", type)
                        .bind("quantity", quantity)
                        .bind("note", note)
                        .execute());
    }

    /**
     * Updates stock quantity for sales (subtracts quantity).
     */
    public boolean updateStockQuantity(String comic_id, int quantity_sell) {
        try {
            int id = Integer.parseInt(comic_id);
            String sql = "UPDATE comics SET stock_quantity = stock_quantity - :quantity_sell " +
                    "WHERE id = :comic_id AND stock_quantity >= :quantity_sell;";
            return jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("comic_id", id)
                            .bind("quantity_sell", quantity_sell)
                            .execute()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Inserts a damage record.
     */
    public boolean insertDAMAGEItem(String comic_id, int quantity, String note) {
        try {
            int id = Integer.parseInt(comic_id);
            String sql = "INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                    "VALUES (:comic_id, 'DAMAGE', :quantity, :note, NOW());";
            return jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("comic_id", id)
                            .bind("quantity", quantity)
                            .bind("note", note)
                            .execute()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Updates damaged quantity.
     */
    public boolean updateDamagedQuantity(String comic_id, int quantity_damaged) {
        try {
            int id = Integer.parseInt(comic_id);
            String sql = "UPDATE comics SET damaged_quantity = COALESCE(damaged_quantity, 0) + :quantity_damaged " +
                    "WHERE id = :comic_id;";
            return jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("comic_id", id)
                            .bind("quantity_damaged", quantity_damaged)
                            .execute()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==========================================
    // NEW METHODS FOR WAREHOUSE MANAGEMENT
    // ==========================================

    /**
     * Fetches all inventory transactions.
     */
    public List<Map<String, Object>> getTransactionHistory() {
        String sql = "SELECT t.id, t.comic_id, t.type, t.quantity, t.note, t.created_at, " +
                "c.name_comics, c.volume " +
                "FROM inventory_transaction t " +
                "JOIN comics c ON t.comic_id = c.id " +
                "ORDER BY t.created_at DESC;";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql).mapToMap().list()
        );
    }

    /**
     * Fetches low stock comics based on available stock threshold.
     */
    public List<Map<String, Object>> getLowStockComics(int threshold) {
        String sql = "SELECT c.id, c.name_comics, c.stock_quantity, c.volume, c.thumbnail_url, " +
                "COALESCE(c.damaged_quantity, 0) AS damaged_quantity, " +
                "(c.stock_quantity - COALESCE(c.damaged_quantity, 0)) AS available_quantity, " +
                "COALESCE(SUM(oi.quantity), 0) AS total_sold " +
                "FROM comics c " +
                "LEFT JOIN order_items oi ON c.id = oi.comic_id " +
                "LEFT JOIN orders o ON oi.order_id = o.id AND o.status = 'Completed' " +
                "WHERE c.is_deleted = 0 " +
                "GROUP BY c.id, c.name_comics, c.stock_quantity, c.volume, c.thumbnail_url, c.damaged_quantity " +
                "HAVING available_quantity <= :threshold " +
                "ORDER BY available_quantity ASC;";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("threshold", threshold)
                        .mapToMap()
                        .list()
        );
    }

    /**
     * Fetches all active comics with their current stock and damaged details.
     */
    public List<Map<String, Object>> getAllComicsWithStock() {
        String sql = "SELECT id, name_comics, volume, stock_quantity, " +
                "COALESCE(damaged_quantity, 0) AS damaged_quantity, " +
                "(stock_quantity - COALESCE(damaged_quantity, 0)) AS available_quantity " +
                "FROM comics " +
                "WHERE is_deleted = 0 " +
                "ORDER BY name_comics ASC, volume ASC;";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql).mapToMap().list()
        );
    }

    /**
     * Atomic transaction to record manual import and increment stock.
     */
    public boolean recordImport(int comicId, int qty, String note) {
        return jdbi.inTransaction(handle -> {
            handle.createUpdate("INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                            "VALUES (:comicId, 'IMPORT', :qty, :note, NOW());")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .bind("note", note)
                    .execute();

            handle.createUpdate("UPDATE comics SET stock_quantity = stock_quantity + :qty WHERE id = :comicId;")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .execute();
            return true;
        });
    }

    /**
     * Atomic transaction to record manual export and decrement stock.
     */
    public boolean recordExport(int comicId, int qty, String note) {
        return jdbi.inTransaction(handle -> {
            // Check available stock first
            Integer currentStock = handle.createQuery("SELECT stock_quantity FROM comics WHERE id = :comicId")
                    .bind("comicId", comicId)
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(0);

            if (currentStock < qty) {
                return false;
            }

            handle.createUpdate("INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                            "VALUES (:comicId, 'EXPORT', :qty, :note, NOW());")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .bind("note", note)
                    .execute();

            handle.createUpdate("UPDATE comics SET stock_quantity = stock_quantity - :qty WHERE id = :comicId;")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .execute();
            return true;
        });
    }

    /**
     * Atomic transaction to record manual damage and increment damaged count.
     */
    public boolean recordDamage(int comicId, int qty, String note) {
        return jdbi.inTransaction(handle -> {
            // Check available stock first
            Map<String, Object> comicInfo = handle.createQuery(
                            "SELECT stock_quantity, COALESCE(damaged_quantity, 0) AS damaged_quantity " +
                                    "FROM comics WHERE id = :comicId")
                    .bind("comicId", comicId)
                    .mapToMap()
                    .findOne()
                    .orElse(null);

            if (comicInfo == null) {
                return false;
            }

            int stock = ((Number) comicInfo.get("stock_quantity")).intValue();
            int damaged = ((Number) comicInfo.get("damaged_quantity")).intValue();
            int available = stock - damaged;

            if (available < qty) {
                return false;
            }

            handle.createUpdate("INSERT INTO inventory_transaction(comic_id, type, quantity, note, created_at) " +
                            "VALUES (:comicId, 'DAMAGE', :qty, :note, NOW());")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .bind("note", note)
                    .execute();

            handle.createUpdate("UPDATE comics SET damaged_quantity = COALESCE(damaged_quantity, 0) + :qty WHERE id = :comicId;")
                    .bind("comicId", comicId)
                    .bind("qty", qty)
                    .execute();
            return true;
        });
    }
}
