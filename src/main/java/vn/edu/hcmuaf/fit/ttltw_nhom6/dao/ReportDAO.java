package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.RevenueRow;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {
    private final Jdbi jdbi;

    public ReportDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public ReportDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }


    public Map<String, Object> getOverviewStats(LocalDate startDate, LocalDate endDate) {
        return jdbi.withHandle(handle -> {
            Map<String, Object> stats = new HashMap<>();


            Double totalRevenue = handle.createQuery(
                            "SELECT COALESCE(SUM(total_amount), 0) FROM orders "
                                    + "WHERE status = 'Completed' AND DATE(order_date) BETWEEN :start AND :end"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .mapTo(Double.class)
                    .one();


            Integer orderCount = handle.createQuery(
                            "SELECT COUNT(*) " +
                                    "FROM orders " +
                                    "WHERE order_date IS NOT NULL " +
                                    "AND DATE(order_date) BETWEEN :start AND :end"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .mapTo(Integer.class)
                    .one();


            Double avgOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0.0;

            Map<String, Object> topProduct = handle.createQuery(
                            "SELECT c.name_comics, SUM(oi.quantity) as total_sold " +
                                    "FROM order_items oi " +
                                    "JOIN comics c ON oi.comic_id = c.id " +
                                    "JOIN orders o ON oi.order_id = o.id " +
                                    "WHERE o.order_date IS NOT NULL " +
                                    "AND DATE(o.order_date) BETWEEN :start AND :end " +
                                    "GROUP BY c.id, c.name_comics " +
                                    "ORDER BY total_sold DESC " +
                                    "LIMIT 1"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .mapToMap()
                    .findFirst()
                    .orElse(null);

            stats.put("revenue", totalRevenue);
            stats.put("totalOrders", orderCount);
            stats.put("avgOrderValue", avgOrderValue);
            stats.put("bestProduct", topProduct != null ? topProduct.get("name_comics") : "Chưa có dữ liệu");

            return stats;
        });
    }


    public List<Map<String, Object>> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        return jdbi.withHandle(handle -> {
            List<Map<String, Object>> result = handle.createQuery(
                            "SELECT DATE(order_date) as date, " +
                                    "COALESCE(SUM(total_amount), 0) as revenue, " +
                                    "COUNT(*) as order_count " +
                                    "FROM orders " +
                                    "WHERE order_date IS NOT NULL " +
                                    "AND DATE(order_date) BETWEEN :start AND :end " +
                                    "GROUP BY DATE(order_date) " +
                                    "ORDER BY date ASC"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .mapToMap()
                    .list();

            return result;
        });
    }

    /**
     * Lấy top sản phẩm bán chạy
     */
    public List<Map<String, Object>> getTopSellingProducts(LocalDate startDate, LocalDate endDate, int limit) {
        return jdbi.withHandle(handle -> {
            List<Map<String, Object>> result = handle.createQuery(
                            "SELECT c.id, c.name_comics, " +
                                    "SUM(oi.quantity) as total_sold, " +
                                    "SUM(oi.quantity * oi.price_at_purchase) as total_revenue " +
                                    "FROM order_items oi " +
                                    "JOIN comics c ON oi.comic_id = c.id " +
                                    "JOIN orders o ON oi.order_id = o.id " +
                                    "WHERE o.order_date IS NOT NULL " +
                                    "AND DATE(o.order_date) BETWEEN :start AND :end " +
                                    "GROUP BY c.id, c.name_comics " +
                                    "ORDER BY total_sold DESC " +
                                    "LIMIT :limit"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .bind("limit", limit)
                    .mapToMap()
                    .list();

            return result;
        });
    }

    public List<Map<String, Object>> getFlashSaleTopRevenue(int limit) {
        String sql = """
        SELECT 
            fs.id,
            fs.name,
            fs.status,
            fs.discount_percent,
            COUNT(DISTINCT o.id)                                    AS total_orders,
            COALESCE(SUM(oi.quantity), 0)                          AS total_items_sold,
            COALESCE(SUM(oi.quantity * oi.price_at_purchase), 0)   AS total_revenue
        FROM FlashSale fs
        LEFT JOIN order_items oi ON oi.flashsale_id = fs.id
        LEFT JOIN orders o ON o.id = oi.order_id
            AND o.status NOT IN ('Cancelled', 'Returned')
        GROUP BY fs.id, fs.name, fs.status, fs.discount_percent
        ORDER BY total_revenue DESC
        LIMIT :limit
    """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("limit", limit)
                        .mapToMap()
                        .list()
        );
    }

    public List<Map<String, Object>> getMonthlyRevenue(LocalDate startDate, LocalDate endDate) {
        return jdbi.withHandle(handle -> {
            List<Map<String, Object>> result = handle.createQuery(
                            "SELECT MONTH(order_date) as month, " +
                                    "COALESCE(SUM(total_amount), 0) as revenue, " +
                                    "COUNT(*) as order_count " +
                                    "FROM orders " +
                                    "WHERE order_date IS NOT NULL " +
                                    "AND DATE(order_date) BETWEEN :start AND :end " +
                                    "GROUP BY MONTH(order_date) " +
                                    "ORDER BY month ASC"
                    )
                    .bind("start", startDate)
                    .bind("end", endDate)
                    .mapToMap()
                    .list();
            return result;
        });
    }

    public List<RevenueRow> getRevenueData(String filter, String startDate, String endDate) {
        LocalDate start;
        LocalDate end = LocalDate.now();

        switch (filter) {
            case "week":
                start = end.minusDays(6);
                break;
            case "month":
                start = end.withDayOfMonth(1);
                break;
            case "year":
                start = end.withDayOfYear(1);
                break;
            case "custom":
                start = LocalDate.parse(startDate);
                end   = LocalDate.parse(endDate);
                break;
            default:
                start = end;
                break;
        }

        final LocalDate finalStart = start;
        final LocalDate finalEnd   = end;

        return jdbi.withHandle(handle -> {
            String groupExpr, labelExpr;
            if ("year".equals(filter)) {
                groupExpr = "MONTH(order_date)";
                labelExpr = "CONCAT('Tháng ', MONTH(order_date))";
            } else if ("month".equals(filter) || "custom".equals(filter)) {
                groupExpr = "DATE(order_date)";
                labelExpr = "DATE_FORMAT(order_date, '%d/%m')";
            } else {
                groupExpr = "DATE(order_date)";
                labelExpr = "DATE_FORMAT(order_date, '%d/%m')";
            }

            String sql =
                    "SELECT " + labelExpr + " AS label, " +
                            "  COALESCE(SUM(total_amount), 0) AS revenue, " +
                            "  COUNT(*) AS total_orders, " +
                            "  COALESCE(SUM(total_amount) / COUNT(*), 0) AS avg_value " +
                            "FROM orders " +
                            "WHERE order_date IS NOT NULL " +
                            "  AND DATE(order_date) BETWEEN :start AND :end " +
                            "GROUP BY " + groupExpr + " " +
                            "ORDER BY " + groupExpr + " ASC";

            return handle.createQuery(sql)
                    .bind("start", finalStart)
                    .bind("end",   finalEnd)
                    .map((rs, ctx) -> {
                        RevenueRow row = new RevenueRow();
                        row.setLabel(rs.getString("label"));
                        row.setRevenue(rs.getDouble("revenue"));
                        row.setTotalOrders(rs.getInt("total_orders"));
                        row.setAvgValue(rs.getDouble("avg_value"));
                        return row;
                    })
                    .list();
        });
    }

    public List<Map<String, Object>> comicSold(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT c.id, c.name_comics, SUM(oi.quantity) AS total_sold, " +
                "SUM(oi.quantity * c.price) AS revenue " +
                "FROM comics c " +
                "JOIN order_items oi ON c.id = oi.comic_id " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.order_date BETWEEN :startDate AND :endDate " +
                "AND o.status IN ('completed','success') " +
                "GROUP BY c.id, c.name_comics " +
                "ORDER BY total_sold DESC";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .mapToMap()
                        .list()
        );
    }

    public List<Map<String, Object>> comicUnsold(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT c.id, c.name_comics, c.stock_quantity AS stock " +
                "FROM comics c " +
                "WHERE c.id NOT IN ( " +
                "    SELECT DISTINCT oi.comic_id FROM order_items oi " +
                "    JOIN orders o ON oi.order_id = o.id " +
                "    WHERE o.order_date BETWEEN :startDate AND :endDate " +
                "    AND o.status IN ('completed','success') " +
                ") " +
                "ORDER BY c.name_comics";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("startDate", startDate)
                        .bind("endDate", endDate)
                        .mapToMap()
                        .list()
        );
    }
}