package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Banner;

import java.sql.Timestamp;
import java.util.List;

public class BannerDao {
    private final Jdbi jdbi;

    public BannerDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<Banner> findActiveBanners(Timestamp now) {
        return jdbi.withHandle(handle ->
                handle.createQuery("""
                SELECT id, image_url AS imageUrl, season,
                       start_date AS startDate, end_date AS endDate,
                       is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
                FROM banners
                WHERE is_active = true
                  AND start_date <= :now
                  AND end_date >= :now
                ORDER BY id
                """)
                        .bind("now", now)
                        .mapToBean(Banner.class)
                        .list()
        );
    }
}