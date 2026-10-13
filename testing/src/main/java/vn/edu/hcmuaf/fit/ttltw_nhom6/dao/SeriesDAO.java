
package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Series;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SeriesDAO extends ADao {
    private final Jdbi jdbi;

    public SeriesDAO() {
        this.jdbi = JdbiConnector.get();
    }

    //    Lay tât cả seri
    public List<Series> getAllSeries() {
        String sql = "SELECT * FROM series WHERE is_deleted = 0 ORDER BY created_at DESC";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Series.class)
                        .list()
        );
    }

    /**
     * Lấy series theo ID
     */

    public Optional<Series> getSeriesById(int id) {
        String sql = "SELECT * FROM series WHERE id = :id AND is_deleted = 0";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", id)
                        .mapToBean(Series.class)
                        .findFirst()
        );
    }

    /**
     * Thêm series mới
     */
    public int addSeries(Series series) {
        String sql = "INSERT INTO series (series_name, description, cover_url, total_volumes, status, created_at, updated_at) " +
                "VALUES (:seriesName, :description, :coverUrl, :totalVolumes, :status, :createdAt, :updatedAt)";

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("seriesName", series.getSeriesName())
                        .bind("description", series.getDescription())
                        .bind("coverUrl", series.getCoverUrl())
                        .bind("totalVolumes", series.getTotalVolumes())
                        .bind("status", series.getStatus())
                        .bind("createdAt", LocalDateTime.now())
                        .bind("updatedAt", LocalDateTime.now())
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Integer.class)
                        .one()
        );
    }

    /**
     * Cập nhật thông tin series
     */
    public boolean updateSeries(Series series) {
        String sql = "UPDATE series SET " +
                "series_name = :seriesName, " +
                "description = :description, " +
                "cover_url = :coverUrl, " +
                "total_volumes = :totalVolumes, " +
                "status = :status, " +
                "updated_at = :updatedAt " +
                "WHERE id = :id AND is_deleted = 0";

        int rows = jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("id", series.getId())
                        .bind("seriesName", series.getSeriesName())
                        .bind("description", series.getDescription())
                        .bind("coverUrl", series.getCoverUrl())
                        .bind("totalVolumes", series.getTotalVolumes())
                        .bind("status", series.getStatus())
                        .bind("updatedAt", LocalDateTime.now())
                        .execute()
        );

        return rows > 0;
    }

    /**
     * Ẩn/Hiện series
     */
    public boolean updateSeriesVisibility(int id, boolean hidden) {
        String sqlSeries = "UPDATE series SET is_hidden = :hidden, updated_at = :updatedAt WHERE id = :id";
        String sqlComics = "UPDATE comics SET is_hidden = :hidden WHERE series_id = :seriesId";

        try {
            int rows = jdbi.withHandle(handle -> {
                // Update series visibility
                int affectedRows = handle.createUpdate(sqlSeries)
                        .bind("id", id)
                        .bind("hidden", hidden)
                        .bind("updatedAt", LocalDateTime.now())
                        .execute();

                // Also update visibility for all comics belonging to this series
                handle.createUpdate(sqlComics)
                        .bind("seriesId", id)
                        .bind("hidden", hidden ? 1 : 0)
                        .execute();

                return affectedRows;
            });
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Xóa mềm series (soft delete)
     */
    public boolean deleteSeries(int id) {

        String sql = "UPDATE series SET is_deleted = 1, deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id";

        try {
            int rows = jdbi.withHandle(handle -> {
                int affectedRows = handle.createUpdate(sql)
                        .bind("id", id)
                        .bind("deletedAt", LocalDateTime.now())
                        .bind("updatedAt", LocalDateTime.now())
                        .execute();

                return affectedRows;
            });

            boolean result = rows > 0;
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getSeriesNameById1(Integer seriesId) {
        if (seriesId == null) {
            return null;
        }
        String sql = "SELECT series_name FROM series WHERE id = :id AND is_deleted = 0";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", seriesId)
                        .mapTo(String.class)
                        .findFirst()
                        .orElse(null)
        );
    }

    /**
     * Kiểm tra tên series đã tồn tại chưa (trừ series đang sửa)
     */
    public boolean isSeriesNameExistsExcludingId(String seriesName, int excludeId) {
        String sql = "SELECT COUNT(*) FROM series WHERE series_name = :name AND id != :id AND is_deleted = 0";

        int count = jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("name", seriesName)
                        .bind("id", excludeId)
                        .mapTo(Integer.class)
                        .one()
        );

        return count > 0;
    }

    /**
     * Lấy series theo trạng thái hiển thị với phân trang
     */
    public List<Series> getSeriesByVisibility(int page, int pageSize, Boolean isHidden) {
        int offset = (page - 1) * pageSize;

        String sql;
        if (isHidden == null) {
            sql = "SELECT * FROM series WHERE is_deleted = 0 " +
                    "ORDER BY series_name ASC LIMIT :limit OFFSET :offset";
        } else {
            sql = "SELECT * FROM series WHERE is_deleted = 0 AND is_hidden = :hidden " +
                    "ORDER BY series_name ASC LIMIT :limit OFFSET :offset";
        }

        List<Series> result = jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql)
                    .bind("limit", pageSize)
                    .bind("offset", offset);

            if (isHidden != null) {
                query.bind("hidden", isHidden ? 1 : 0);
            }

            return query.mapToBean(Series.class).list();
        });
        return result;
    }

    /**
     * Đếm số series theo trạng thái hiển thị
     */

    public int countSeriesByVisibility(Boolean isHidden) {
        String sql;
        if (isHidden == null) {
            sql = "SELECT COUNT(*) FROM series WHERE is_deleted = 0";
        } else {
            sql = "SELECT COUNT(*) FROM series WHERE is_deleted = 0 AND is_hidden = :hidden";
        }

        int count = jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql);

            if (isHidden != null) {
                query.bind("hidden", isHidden ? 1 : 0);
            }

            return query.mapTo(Integer.class).one();
        });

        return count;
    }

    /**
     * Tìm kiếm series theo tên và trạng thái hiển thị
     */

    public List<Series> searchSeriesByNameAndVisibility(String keyword, Boolean isHidden) {
        String sql;
        if (isHidden == null) {
            sql = "SELECT * FROM series WHERE is_deleted = 0 " +
                    "AND series_name LIKE :keyword ORDER BY series_name ASC";
        } else {
            sql = "SELECT * FROM series WHERE is_deleted = 0 " +
                    "AND series_name LIKE :keyword AND is_hidden = :hidden " +
                    "ORDER BY series_name ASC";
        }

        List<Series> result = jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql)
                    .bind("keyword", "%" + keyword + "%");

            if (isHidden != null) {
                query.bind("hidden", isHidden ? 1 : 0);
            }

            return query.mapToBean(Series.class).list();
        });

        return result;
    }
}