package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;

import java.time.LocalDateTime;
import java.util.List;

public class WishlistDAO {
    private final Jdbi jdbi;

    public WishlistDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public WishlistDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    // Kiểm tra comic đã có trong wishlist chưa
    public boolean isInWishlist(int userId, int comicId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM wishlist WHERE user_id = :userId AND comic_id = :comicId")
                        .bind("userId", userId)
                        .bind("comicId", comicId)
                        .mapTo(Integer.class)
                        .one() > 0
        );
    }

    // Thêm comic vào wishlist
    public boolean addToWishlist(int userId, int comicId) {
        if (isInWishlist(userId, comicId)) {
            return false; // Đã tồn tại
        }

        return jdbi.withHandle(handle ->
                handle.createUpdate("INSERT INTO wishlist (user_id, comic_id, added_at) VALUES (:userId, :comicId, :addedAt)")
                        .bind("userId", userId)
                        .bind("comicId", comicId)
                        .bind("addedAt", LocalDateTime.now())
                        .execute()
        ) > 0;
    }

    // Xóa comic khỏi wishlist
    public boolean removeFromWishlist(int userId, int comicId) {
        return jdbi.withHandle(handle ->
                handle.createUpdate("DELETE FROM wishlist WHERE user_id = :userId AND comic_id = :comicId")
                        .bind("userId", userId)
                        .bind("comicId", comicId)
                        .execute()
        ) > 0;
    }


    // Lấy số lượng sản phẩm trong wishlist
    public int getWishlistCount(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM wishlist WHERE user_id = :userId")
                        .bind("userId", userId)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    // Lấy danh sách comic đầy đủ trong wishlist (join với bảng comics)
    public List<Comic> getWishlistComics(int userId) {
        return jdbi.withHandle(handle ->
                handle.createQuery(
                                "SELECT c.* FROM comics c " +
                                        "INNER JOIN wishlist w ON c.id = w.comic_id " +
                                        "WHERE w.user_id = :userId AND c.is_deleted = 0 " +
                                        "ORDER BY w.added_at DESC"
                        )
                        .bind("userId", userId)
                        .mapToBean(Comic.class)
                        .list()
        );
    }
}