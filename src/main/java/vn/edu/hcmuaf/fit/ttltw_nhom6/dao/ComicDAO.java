package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.ComicImage;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.TextUtils;


import java.time.LocalDateTime;
import java.util.*;

public class ComicDAO extends ADao {
    public final Jdbi jdbi;

    public ComicDAO() {
        this.jdbi = JdbiConnector.get();
    }

    public ComicDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<Comic> searchCandidate(String keyword) {
        String normalized = TextUtils.normalize(keyword.toLowerCase());
        String number = keyword.replaceAll("\\D+", "");

        String sql = """
                SELECT DISTINCT c.*
                """ + buildFlashSaleColumns() + """
                FROM Comics c
                """ + buildFlashSaleJoin() + """
                    WHERE c.is_deleted = 0 AND c.is_hidden = 0
                      AND c.status = 'available'
                    ORDER BY c.name_comics
                    LIMIT 500
                """;

        List<Comic> allComics = new ArrayList<>(
                jdbi.withHandle(h ->
                        h.createQuery(sql)
                                .mapToBean(Comic.class)
                                .list()
                )
        );

        String[] words = normalized.split("\\s+");
        List<String> meaningfulWords = new ArrayList<>();
        for (String word : words) {
            if (!word.matches("tap|vol|volume|\\d+")) {
                meaningfulWords.add(word);
            }
        }

        int volumeNum = number.isEmpty() ? -1 : Integer.parseInt(number);

        List<Comic> result = new ArrayList<>();

        for (Comic c : allComics) {
            String name = TextUtils.normalize(c.getNameComics().toLowerCase());

            int matchCount = 0;
            for (String w : meaningfulWords) {
                if (name.contains(w)) {
                    matchCount++;
                }
            }

            boolean nameMatch;
            if (meaningfulWords.isEmpty()) {
                nameMatch = true;
            } else if (meaningfulWords.size() == 1) {
                nameMatch = matchCount >= 1;
            } else {
                nameMatch = ((double) matchCount / meaningfulWords.size()) >= 0.5;
            }

            boolean volumeMatch = c.getVolume() != null && c.getVolume() == volumeNum;

            boolean ok;
            if (volumeNum != -1 && !meaningfulWords.isEmpty()) {
                ok = nameMatch && volumeMatch;
            } else if (volumeNum != -1) {
                ok = volumeMatch;
            } else {
                ok = nameMatch;
            }

            if (ok) {
                result.add(c);
            }

            if (result.size() >= 100) break;
        }

        return result;
    }

    public List<Comic> smartSearch(String keyword) {

        List<Comic> candidates = new ArrayList<>(searchCandidate(keyword));

        String normKey = TextUtils.normalize(keyword.toLowerCase());
        String[] words = normKey.split("\\s+");
        String number = keyword.replaceAll("\\D+", "");

        candidates.sort((a, b) -> {
            int scoreA = score(a, words, number, normKey);
            int scoreB = score(b, words, number, normKey);
            return Integer.compare(scoreB, scoreA);
        });

        return candidates;
    }

    private int score(Comic c, String[] words, String number, String fullKeyword) {
        String name = TextUtils.normalize(c.getNameComics().toLowerCase());
        int score = 0;

        if (name.equals(fullKeyword)) {
            score += 1000;
        }

        if (name.contains(fullKeyword)) {
            score += 500;
        }

        int matchCount = 0;
        for (String word : words) {
            if (name.contains(word)) {
                matchCount++;
                score += 50;
            }
        }

        if (matchCount == words.length) {
            score += 200;
        }

        if (!number.isEmpty()) {
            try {
                int targetNum = Integer.parseInt(number);

                if (c.getVolume() != null && c.getVolume() == targetNum) {
                    score += 400;
                }

                String numPattern = ".*(tap|volume|vol)\\s*0*" + number + "([^0-9]|$).*";
                if (name.matches(numPattern)) {
                    score += 300;
                }

            } catch (NumberFormatException e) {
            }
        }

        score -= name.length() / 10;
        return score;
    }


    public List<Comic> findByAuthor(String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = TextUtils.normalize(authorName.toLowerCase());

        String sql = """
                        SELECT DISTINCT c.*, a.name as authorName
                FROM Comics c
                INNER JOIN Comic_Authors ca ON c.id = ca.comic_id
                INNER JOIN Authors a ON ca.author_id = a.id
                WHERE a.is_deleted = 0
                AND c.is_deleted = 0
                AND c.status = 'available' AND c.is_hidden = 0
                """;

        List<Comic> allComics = jdbi.withHandle(h ->
                h.createQuery(sql)
                        .mapToBean(Comic.class)
                        .list()
        );

        List<Comic> result = new ArrayList<>();
        Set<Integer> addedIds = new HashSet<>();

        for (Comic c : allComics) {
            if (c.getAuthorName() != null && !addedIds.contains(c.getId())) {
                String authName = TextUtils.normalize(c.getAuthorName().toLowerCase());
                if (authName.contains(normalized)) {
                    result.add(c);
                    addedIds.add(c.getId());
                }
            }
        }

        return result;
    }


    public List<Comic> findByCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = TextUtils.normalize(categoryName.toLowerCase());

        String sql = """
                        SELECT DISTINCT c.*, cat.name_categories as categoryName
                FROM Comics c
                JOIN Categories cat ON c.category_id = cat.id
                WHERE cat.is_deleted = 0
                AND c.is_deleted = 0
                AND c.status = 'available' AND c.is_hidden = 0
                """;

        List<Comic> allComics = jdbi.withHandle(h ->
                h.createQuery(sql)
                        .mapToBean(Comic.class)
                        .list()
        );

        List<Comic> result = new ArrayList<>();
        for (Comic c : allComics) {
            if (c.getCategoryName() != null) {
                String catName = TextUtils.normalize(c.getCategoryName().toLowerCase());
                if (catName.contains(normalized)) {
                    result.add(c);
                }
            }
        }

        return result;
    }

    public List<Comic> findByPublisher(String publisherName) {
        if (publisherName == null || publisherName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = TextUtils.normalize(publisherName.toLowerCase());

        String sql = """
                        SELECT DISTINCT c.*, p.name as publisherName
                FROM Comics c
                INNER JOIN Comic_Publishers cp ON c.id = cp.comic_id
                INNER JOIN Publishers p ON cp.publisher_id = p.id
                WHERE p.is_deleted = 0
                AND c.is_deleted = 0
                AND c.status = 'available' AND c.is_hidden = 0
                """;

        List<Comic> allComics = jdbi.withHandle(h ->
                h.createQuery(sql)
                        .mapToBean(Comic.class)
                        .list()
        );

        List<Comic> result = new ArrayList<>();
        Set<Integer> addedIds = new HashSet<>();

        for (Comic c : allComics) {
            if (c.getPublisherName() != null && !addedIds.contains(c.getId())) {
                String pubName = TextUtils.normalize(c.getPublisherName().toLowerCase());
                if (pubName.contains(normalized)) {
                    result.add(c);
                    addedIds.add(c.getId());
                }
            }
        }

        return result;

    }

    public List<Comic> findBySeriesId(int seriesId, int excludeId) {
        String sql = """
                SELECT c.*
                """ + buildFlashSaleColumns() + """
                FROM Comics c
                """ + buildFlashSaleJoin() + """
                    WHERE c.series_id = :sid
                      AND c.id != :id
                      AND c.is_deleted = 0
                      AND c.status = 'available' AND c.is_hidden = 0
                    ORDER BY 
                        CASE WHEN c.volume IS NOT NULL THEN c.volume ELSE 999999 END,
                        c.name_comics
                """;

        return new ArrayList<>(
                jdbi.withHandle(h ->
                        h.createQuery(sql)
                                .bind("sid", seriesId)
                                .bind("id", excludeId)
                                .mapToBean(Comic.class)
                                .list()
                )
        );
    }


    public List<Comic> getTop5BestSellerThisWeek() {
        String sql = """
                SELECT c.*,
                       SUM(oi.quantity) AS totalSold
                """ + buildFlashSaleColumns() + """
                FROM Order_Items oi
                JOIN Orders o ON oi.order_id = o.id
                JOIN Comics c ON oi.comic_id = c.id
                """ + buildFlashSaleJoin() + """
                    WHERE c.is_deleted = 0
                      AND c.status = 'available' AND c.is_hidden = 0
                      AND YEARWEEK(o.order_date, 1) = YEARWEEK(CURDATE(), 1)
                    GROUP BY c.id
                    ORDER BY totalSold DESC
                    LIMIT 5
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    public List<Comic> getSuggestedComics(Integer userId) {
        List<Comic> suggested = new ArrayList<>();

        if (userId != null) {
            String sqlNextVolume = """
                    SELECT c.*
                    """ + buildFlashSaleColumns() + """
                    FROM comics c
                    """ + buildFlashSaleJoin() + """
                        WHERE c.series_id IN (
                            SELECT DISTINCT c2.series_id
                            FROM comics c2
                            JOIN wishlist w ON c2.id = w.comic_id
                            WHERE w.user_id = :userId
                              AND c2.is_deleted = 0
                        )
                        AND c.volume = (
                            SELECT MAX(c3.volume) + 1
                            FROM comics c3
                            JOIN wishlist w2 ON c3.id = w2.comic_id
                            WHERE c3.series_id = c.series_id
                              AND w2.user_id = :userId
                              AND c3.is_deleted = 0
                        )
                        AND c.is_deleted = 0 AND c.is_hidden = 0
                        LIMIT 8
                    """;

            List<Comic> nextVolumes = jdbi.withHandle(handle ->
                    handle.createQuery(sqlNextVolume)
                            .bind("userId", userId)
                            .mapToBean(Comic.class)
                            .list()
            );

            suggested.addAll(nextVolumes);

            if (suggested.size() < 12) {
                int need = 12 - suggested.size();

                String sqlSameCategory = """
                        SELECT c.*
                        """ + buildFlashSaleColumns() + """
                        FROM comics c
                        """ + buildFlashSaleJoin() + """
                            WHERE c.category_id IN (
                                SELECT DISTINCT c2.category_id
                                FROM comics c2
                                JOIN wishlist w ON c2.id = w.comic_id
                                WHERE w.user_id = :userId
                                  AND c2.is_deleted = 0
                            )
                            AND c.id NOT IN (
                                SELECT comic_id 
                                FROM wishlist 
                                WHERE user_id = :userId
                            )
                            AND c.is_deleted = 0
                            ORDER BY RAND()
                            LIMIT :limit
                        """;

                List<Comic> sameCategory = jdbi.withHandle(handle ->
                        handle.createQuery(sqlSameCategory)
                                .bind("userId", userId)
                                .bind("limit", need)
                                .mapToBean(Comic.class)
                                .list()
                );

                suggested.addAll(sameCategory);
            }
        }

        if (suggested.isEmpty()) {
            String sqlLatest = """
                    SELECT c.*
                    """ + buildFlashSaleColumns() + """
                    FROM comics c
                    """ + buildFlashSaleJoin() + """
                        WHERE c.is_deleted = 0
                        ORDER BY c.created_at DESC
                        LIMIT 12
                    """;

            suggested = jdbi.withHandle(handle ->
                    handle.createQuery(sqlLatest)
                            .mapToBean(Comic.class)
                            .list()
            );
        }

        return suggested;
    }


    public Comic getComicById(int id) {
        String sql = """
                SELECT c.*
                """ + buildFlashSaleColumns() + """
                FROM comics c
                """ + buildFlashSaleJoin() + """
                    WHERE c.id = :id AND c.is_deleted = 0 AND c.is_hidden = 0
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", id)
                        .mapToBean(Comic.class)
                        .findOne()
                        .orElse(null)
        );
    }


    public Comic getComicById2(int comicId) {
        String sql = """
                SELECT c.*, 
                       cat.name_categories AS categoryName, 
                       s.series_name AS seriesName
                """ + buildFlashSaleColumns() + """
                FROM comics c 
                LEFT JOIN categories cat ON c.category_id = cat.id 
                LEFT JOIN series s ON c.series_id = s.id 
                """ + buildFlashSaleJoin() + """
                    WHERE c.id = ? AND c.is_deleted = 0 
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, comicId)
                        .mapToBean(Comic.class)
                        .findFirst()
                        .orElse(null)
        );
    }

    public Comic getComicById3(int comicId) {
        String sql = """
                SELECT c.*, 
                       cat.name_categories AS categoryName, 
                       s.series_name AS seriesName
                """ + buildFlashSaleColumns() + """
                FROM comics c 
                LEFT JOIN categories cat ON c.category_id = cat.id 
                LEFT JOIN series s ON c.series_id = s.id 
                """ + buildFlashSaleJoin() + """
                    WHERE c.id = ? AND c.is_deleted = 0 AND c.is_hidden = 0
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, comicId)
                        .mapToBean(Comic.class)
                        .findFirst()
                        .orElse(null)
        );
    }


    public List<ComicImage> getComicImages(int comicId) {
        String sql = """
                SELECT * FROM comic_images
                WHERE comic_id = :comicId 
                ORDER BY sort_order ASC
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("comicId", comicId)
                        .mapToBean(ComicImage.class)
                        .list()
        );
    }


    public List<Comic> getRelatedComics(int comicId) {
        final int LIMIT = 6;

        Comic current = jdbi.withHandle(h ->
                h.createQuery("""
                                    SELECT id, series_id, volume, category_id
                                    FROM Comics
                                    WHERE id = :comicId and is_hidden = 0
                                """)
                        .bind("comicId", comicId)
                        .mapToBean(Comic.class)
                        .one()
        );

        List<Comic> result = new ArrayList<>();

        if (current.getSeriesId() != null && current.getVolume() != null) {
            result.addAll(jdbi.withHandle(h ->
                    h.createQuery("""
                                    SELECT c.*
                                    """ + buildFlashSaleColumns() + """
                                    FROM Comics c
                                    """ + buildFlashSaleJoin() + """
                                        WHERE c.series_id = :seriesId
                                          AND c.volume > :volume
                                          AND c.id != :comicId
                                          AND c.is_deleted = 0
                                          AND c.status = 'available' and c.is_hidden = 0
                                        ORDER BY c.volume ASC
                                        LIMIT 9
                                    """)
                            .bind("seriesId", current.getSeriesId())
                            .bind("volume", current.getVolume())
                            .bind("comicId", comicId)
                            .mapToBean(Comic.class)
                            .list()
            ));
        }

        List<Integer> excludedIds = new ArrayList<>();
        excludedIds.add(comicId);
        result.forEach(c -> excludedIds.add(c.getId()));

        if (result.size() < LIMIT) {
            int need = LIMIT - result.size();

            result.addAll(jdbi.withHandle(h ->
                    h.createQuery("""
                                    SELECT c.*
                                    """ + buildFlashSaleColumns() + """
                                    FROM Comics c
                                    """ + buildFlashSaleJoin() + """
                                        WHERE c.category_id = :categoryId
                                          AND c.id NOT IN (<excludedIds>)
                                          AND c.is_deleted = 0
                                          AND c.status = 'available' and c.is_hidden = 0
                                        ORDER BY c.created_at DESC
                                        LIMIT :limit
                                    """)
                            .bind("categoryId", current.getCategoryId())
                            .bind("limit", need)
                            .bindList("excludedIds", excludedIds)
                            .mapToBean(Comic.class)
                            .list()
            ));
        }

        List<Integer> finalExcludedIds = new ArrayList<>();
        finalExcludedIds.add(comicId);
        result.forEach(c -> finalExcludedIds.add(c.getId()));

        if (result.size() < LIMIT) {
            int need = LIMIT - result.size();

            result.addAll(jdbi.withHandle(h ->
                    h.createQuery("""
                                    SELECT c.*
                                    """ + buildFlashSaleColumns() + """
                                    FROM Comics c
                                    """ + buildFlashSaleJoin() + """
                                        WHERE c.id NOT IN (<excludedIds>)
                                          AND c.is_deleted = 0
                                          AND c.status = 'available'
                                        ORDER BY RAND()
                                        LIMIT :limit
                                    """)
                            .bind("limit", need)
                            .bindList("excludedIds", finalExcludedIds)
                            .mapToBean(Comic.class)
                            .list()
            ));
        }

        return result.size() > LIMIT ? result.subList(0, LIMIT) : result;
    }


    public double getAverageRating(int comicId) {
        String sql = """
                SELECT COALESCE(AVG(rating), 0) as avg_rating 
                FROM Reviews 
                WHERE comic_id = :comicId
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("comicId", comicId)
                        .mapTo(Double.class)
                        .findOne()
                        .orElse(0.0)
        );
    }


    public List<Comic> getRecommendedComics(int userId, int limit) {
        String sql = """
                    WITH wishlist_series AS (
                        SELECT DISTINCT 
                            c.series_id,
                            MAX(c.volume) as max_volume
                        FROM Wishlist w
                        JOIN Comics c ON w.comic_id = c.id
                        WHERE w.user_id = :userId 
                        AND c.is_deleted = 0
                        AND c.series_id IS NOT NULL
                        GROUP BY c.series_id
                    ),
                    wishlist_categories AS (
                        SELECT DISTINCT c.category_id
                        FROM Wishlist w
                        JOIN Comics c ON w.comic_id = c.id
                        WHERE w.user_id = :userId 
                        AND c.is_deleted = 0
                        AND c.category_id IS NOT NULL
                    ),
                    next_volumes AS (
                        SELECT 
                            c.*,
                            COALESCE(SUM(oi.quantity), 0) as totalSold,
                            1 as priority,
                            flash_sale.flash_sale_id,
                            flash_sale.flash_sale_name,
                            flash_sale.flash_sale_discount,
                            CASE WHEN flash_sale.flash_sale_id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                            CASE 
                                WHEN flash_sale.flash_sale_id IS NOT NULL 
                                THEN ROUND(c.price * (1 - flash_sale.flash_sale_discount / 100), 0)
                                ELSE NULL
                            END AS flash_sale_price
                        FROM Comics c
                        INNER JOIN wishlist_series ws ON c.series_id = ws.series_id
                        LEFT JOIN Order_Items oi ON c.id = oi.comic_id
                        LEFT JOIN Orders o ON oi.order_id = o.id 
                            AND o.status NOT IN ('cancelled', 'returned')
                            AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                        LEFT JOIN (
                            SELECT 
                                fsc.comic_id,
                                fs.id AS flash_sale_id,
                                fs.name AS flash_sale_name,
                                fs.discount_percent AS flash_sale_discount,
                                ROW_NUMBER() OVER (
                                    PARTITION BY fsc.comic_id 
                                    ORDER BY fs.discount_percent DESC, fs.end_time ASC
                                ) AS rn
                            FROM flashsale_comics fsc
                            JOIN flashsale fs ON fsc.flashsale_id = fs.id
                            WHERE fs.status = 'active'
                              AND NOW() BETWEEN fs.start_time AND fs.end_time
                        ) AS flash_sale ON c.id = flash_sale.comic_id AND flash_sale.rn = 1
                        WHERE c.volume = ws.max_volume + 1
                        AND c.is_deleted = 0
                        AND c.stock_quantity > 0
                        AND NOT EXISTS (
                            SELECT 1 FROM Wishlist w2 
                            WHERE w2.user_id = :userId AND w2.comic_id = c.id
                        )
                        GROUP BY c.id
                    ),
                    same_category AS (
                        SELECT 
                            c.*,
                            COALESCE(SUM(oi.quantity), 0) as totalSold,
                            2 as priority,
                            flash_sale.flash_sale_id,
                            flash_sale.flash_sale_name,
                            flash_sale.flash_sale_discount,
                            CASE WHEN flash_sale.flash_sale_id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                            CASE 
                                WHEN flash_sale.flash_sale_id IS NOT NULL 
                                THEN ROUND(c.price * (1 - flash_sale.flash_sale_discount / 100), 0)
                                ELSE NULL
                            END AS flash_sale_price
                        FROM Comics c
                        INNER JOIN wishlist_categories wc ON c.category_id = wc.category_id
                        LEFT JOIN Order_Items oi ON c.id = oi.comic_id
                        LEFT JOIN Orders o ON oi.order_id = o.id 
                            AND o.status NOT IN ('cancelled', 'returned')
                            AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                        LEFT JOIN (
                            SELECT 
                                fsc.comic_id,
                                fs.id AS flash_sale_id,
                                fs.name AS flash_sale_name,
                                fs.discount_percent AS flash_sale_discount,
                                ROW_NUMBER() OVER (
                                    PARTITION BY fsc.comic_id 
                                    ORDER BY fs.discount_percent DESC, fs.end_time ASC
                                ) AS rn
                            FROM flashsale_comics fsc
                            JOIN flashsale fs ON fsc.flashsale_id = fs.id
                            WHERE fs.status = 'active'
                              AND NOW() BETWEEN fs.start_time AND fs.end_time
                        ) AS flash_sale ON c.id = flash_sale.comic_id AND flash_sale.rn = 1
                        WHERE c.is_deleted = 0
                        AND c.stock_quantity > 0
                        AND NOT EXISTS (
                            SELECT 1 FROM Wishlist w2 
                            WHERE w2.user_id = :userId AND w2.comic_id = c.id
                        )
                        AND c.id NOT IN (SELECT id FROM next_volumes)
                        GROUP BY c.id
                        ORDER BY RAND()
                        LIMIT :limit
                    )
                    SELECT * FROM (
                        SELECT * FROM next_volumes
                        UNION ALL
                        SELECT * FROM same_category
                    ) as final_result
                    ORDER BY priority, RAND()
                    LIMIT :limit
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .bind("limit", limit)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    public List<Comic> getPopularComics(int limit) {
        String sql = """
                SELECT 
                    c.*,
                    COALESCE(SUM(oi.quantity), 0) as totalSold
                """ + buildFlashSaleColumns() + """
                FROM Comics c
                LEFT JOIN Order_Items oi ON c.id = oi.comic_id
                LEFT JOIN Orders o ON oi.order_id = o.id 
                    AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                    AND o.status NOT IN ('cancelled', 'returned')
                """ + buildFlashSaleJoin() + """
                    WHERE c.is_deleted = 0
                    AND c.stock_quantity > 0
                    GROUP BY c.id
                    ORDER BY totalSold DESC, c.created_at DESC
                    LIMIT :limit
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("limit", limit)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * debug
     */
    public void debugWishlist(int userId) {
        String sql = """
                SELECT 
                    c.id,
                    c.name_comics,
                    c.series_id,
                    c.volume,
                    c.category_id,
                    cat.name_categories as category_name
                FROM Wishlist w
                JOIN Comics c ON w.comic_id = c.id
                LEFT JOIN Categories cat ON c.category_id = cat.id
                WHERE w.user_id = :userId
                ORDER BY w.added_at DESC
                """;

        jdbi.withHandle(handle -> {
            handle.createQuery(sql)
                    .bind("userId", userId)
                    .mapToMap()
                    .forEach(row -> {
                        System.out.println("Comic: " + row.get("name_comics"));
                        System.out.println("  - Series ID: " + row.get("series_id"));
                        System.out.println("  - Volume: " + row.get("volume"));
                        System.out.println("  - Category: " + row.get("category_name") + " (ID: " + row.get("category_id") + ")");
                        System.out.println("---");
                    });
            return null;
        });
    }


    public List<Comic> searchComicsAdmin(String keyword, String author, Integer categoryId,
                                         int page, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String searchTerm = keyword.trim().toLowerCase();

        StringBuilder sql = new StringBuilder("""
                    SELECT distinct c.*, s.series_name
                    , cat.name_categories as categoryName
                    FROM Comics c
                    LEFT JOIN Series s ON c.series_id = s.id
                    LEFT JOIN Categories cat ON c.category_id = cat.id
                    WHERE c.is_deleted = 0
                    AND (
                         LOWER(c.name_comics) LIKE :keyword
                         OR LOWER(c.author) LIKE :keyword
                         OR LOWER(cat.name_categories) LIKE :keyword
                         OR LOWER(s.series_name) LIKE :keyword
                """);

        String numberOnly = keyword.replaceAll("\\D+", "");
        if (!numberOnly.isEmpty()) {
            sql.append(" OR c.volume = :volume");
        }

        sql.append(")");

        if (author != null && !author.isEmpty()) {
            sql.append(" AND LOWER(c.author) LIKE :author");
        }

        if (categoryId != null) {
            sql.append(" AND c.category_id = :categoryId");
        }

        sql.append(" ORDER BY c.created_at DESC");
        sql.append(" LIMIT :limit OFFSET :offset");

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("keyword", "%" + searchTerm + "%")
                    .bind("limit", limit)
                    .bind("offset", (page - 1) * limit);

            if (!numberOnly.isEmpty()) {
                try {
                    int volumeNumber = Integer.parseInt(numberOnly);
                    query.bind("volume", volumeNumber);
                } catch (NumberFormatException e) {
                }
            }

            if (author != null && !author.isEmpty()) {
                query.bind("author", "%" + author.toLowerCase() + "%");
            }

            if (categoryId != null) {
                query.bind("categoryId", categoryId);
            }

            return query.mapToBean(Comic.class).list();
        });
    }

    public int countComicsAdmin(String keyword, String author, Integer categoryId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0;
        }

        String searchTerm = keyword.trim().toLowerCase();

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT c.id)
                FROM Comics c
                LEFT JOIN Series s ON c.series_id = s.id
                LEFT JOIN Categories cat ON c.category_id = cat.id
                WHERE c.is_deleted = 0
                AND (
                    LOWER(c.name_comics) LIKE :keyword
                    OR LOWER(c.author) LIKE :keyword
                    OR LOWER(cat.name_categories) LIKE :keyword
                    OR LOWER(s.series_name) LIKE :keyword
                """);

        String numberOnly = keyword.replaceAll("\\D+", "");
        if (!numberOnly.isEmpty()) {
            sql.append(" OR c.volume = :volume");
        }

        sql.append(")");

        if (author != null && !author.isEmpty()) {
            sql.append(" AND LOWER(c.author) LIKE :author");
        }

        if (categoryId != null) {
            sql.append(" AND c.category_id = :categoryId");
        }

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("keyword", "%" + searchTerm + "%");

            if (!numberOnly.isEmpty()) {
                try {
                    int volumeNumber = Integer.parseInt(numberOnly);
                    query.bind("volume", volumeNumber);
                } catch (NumberFormatException e) {
                }
            }

            if (author != null && !author.isEmpty()) {
                query.bind("author", "%" + author.toLowerCase() + "%");
            }

            if (categoryId != null) {
                query.bind("categoryId", categoryId);
            }

            return query.mapTo(Integer.class).one();
        });
    }

    public List<Comic> getAllComicsAdmin(int page, int limit) {
        int offset = (page - 1) * limit;

        String sql = """
                SELECT c.*, 
                       cat.name_categories AS categoryName, 
                       s.series_name AS seriesName
                """ + buildFlashSaleColumns() + """
                FROM comics c 
                LEFT JOIN categories cat ON c.category_id = cat.id 
                LEFT JOIN series s ON c.series_id = s.id 
                """ + buildFlashSaleJoin() + """
                    WHERE c.is_deleted = 0 
                    ORDER BY c.created_at DESC 
                    LIMIT ? OFFSET ?
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, limit)
                        .bind(1, offset)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    public int countAllComics() {
        String sql = "SELECT COUNT(*) FROM Comics WHERE is_deleted = 0";
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public int insertComic(Comic comic) {
        String sql = """
                INSERT INTO Comics (name_comics, author, publisher, description, price,stock_quantity, status, thumbnail_url, category_id,volume,series_id ,is_deleted, deleted_at,created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            return jdbi.withHandle(handle -> {
                return handle.createUpdate(sql)
                        .bind(0, comic.getNameComics())
                        .bind(1, comic.getAuthor())
                        .bind(2, comic.getPublisher())
                        .bind(3, comic.getDescription())
                        .bind(4, comic.getPrice())
                        .bind(5, comic.getStockQuantity())
                        .bind(6, comic.getStatus() != null ? comic.getStatus() : "available")
                        .bind(7, comic.getThumbnailUrl())
                        .bind(8, comic.getCategoryId())
                        .bind(9, comic.getVolume())
                        .bind(10, comic.getSeriesId())
                        .bind(11, false) // is_deleted = false (truyện mới không bị xóa)
                        .bind(12, (LocalDateTime) null) // deleted_at = NULL (chưa bị xóa)
                        .bind(13, LocalDateTime.now())
                        .bind(14, LocalDateTime.now())
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Integer.class)
                        .findFirst()
                        .orElse(-1);
            });
        } catch (UnableToExecuteStatementException e) {
            e.printStackTrace();
            return -1;
        }
    }


    public boolean insertComicImages(List<ComicImage> images) {
        if (images == null || images.isEmpty()) {
            return true;
        }
        try {
            jdbi.useHandle(handle -> {
                String sql = """
                        INSERT INTO comic_images (comic_id,image_url,image_type,sort_order,created_at )
                        values (?,?,?,?,?)
                        """;
                var batch = handle.prepareBatch(sql);
                for (ComicImage img : images) {
                    batch.bind(0, img.getComicId())
                            .bind(1, img.getImageUrl())
                            .bind(2, img.getImageType())
                            .bind(3, img.getSortOrder())
                            .bind(4, LocalDateTime.now())
                            .add();
                }
                batch.execute();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public boolean isComicNameExist(String name, Integer seriesId, Integer volume) {
        String sql = """
                SELECT COUNT(*) FROM Comics
                WHERE name_comics = ?
                AND (series_id = ? OR (series_id IS NULL AND ? IS NULL))
                AND (volume = ? OR (volume IS NULL AND ? IS NULL))
                AND is_deleted = false
                """;
        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, name)
                        .bind(1, seriesId)
                        .bind(2, seriesId)
                        .bind(3, volume)
                        .bind(4, volume)
                        .mapTo(Integer.class)
                        .findFirst()
                        .orElse(0) > 0
        );
    }


    public int findOrCreateAuthor(String authorName) {
        String findSql = "SELECT id FROM authors WHERE LOWER(name) = LOWER(:name) AND is_deleted = 0 LIMIT 1";
        Optional<Integer> existingId = jdbi.withHandle(handle ->
                handle.createQuery(findSql)
                        .bind("name", authorName.trim())
                        .mapTo(Integer.class)
                        .findFirst()
        );

        if (existingId.isPresent()) {
            return existingId.get();
        }

        String insertSql = "INSERT INTO authors (name, is_deleted) VALUES (:name, :isDeleted)";
        return jdbi.withHandle(handle ->
                handle.createUpdate(insertSql)
                        .bind("name", authorName.trim())
                        .bind("isDeleted", false)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Integer.class)
                        .findFirst()
                        .orElse(-1)
        );
    }


    public int findOrCreatePublisher(String publisherName) {
        // Tìm publisher hiện có
        String findSql = "SELECT id FROM publishers WHERE LOWER(name) = LOWER(:name) AND is_deleted = 0 LIMIT 1";
        Optional<Integer> existingId = jdbi.withHandle(handle ->
                handle.createQuery(findSql)
                        .bind("name", publisherName.trim())
                        .mapTo(Integer.class)
                        .findFirst()
        );

        if (existingId.isPresent()) {
            return existingId.get();
        }

        // Tạo mới publisher
        String insertSql = "INSERT INTO publishers (name, is_deleted) VALUES (:name, :isDeleted)";
        return jdbi.withHandle(handle ->
                handle.createUpdate(insertSql)
                        .bind("name", publisherName.trim())
                        .bind("isDeleted", false)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Integer.class)
                        .findFirst()
                        .orElse(-1)
        );
    }

    //    Lien ket comic va au thor
    public boolean linkComicAuthor(int comicId, int authorId) {
        String sql = "INSERT INTO comic_authors (comic_id, author_id) VALUES (:comicId, :authorId)";

        try {
            return jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("comicId", comicId)
                            .bind("authorId", authorId)
                            .execute() > 0
            );
        } catch (Exception e) {
            return false;
        }
    }

    //lien ket comic voi publisher
    public boolean linkComicPublisher(int comicId, int publisherId) {
        String sql = "INSERT INTO comic_publishers (comic_id,publisher_id) values (:comicId, :publisherId)";
        try {
            return jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("comicId", comicId)
                            .bind("publisherId", publisherId)
                            .execute() > 0
            );
        } catch (Exception e) {
            return false;
        }
    }


    public boolean updateComic(Comic comic) {
        String sql = "UPDATE comics SET " +
                "name_comics = ?, " +
                "description = ?, " +
                "price = ?, " +
                "stock_quantity = ?, " +
                "category_id = ?, " +
                "series_id = ?, " +
                "volume = ?, " +
                "thumbnail_url = ?, " +
                "status = ?, " +
                "updated_at = NOW() " +
                "WHERE id = ?";

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind(0, comic.getNameComics())
                        .bind(1, comic.getDescription())
                        .bind(2, comic.getPrice())
                        .bind(3, comic.getStockQuantity())
                        .bind(4, comic.getCategoryId())
                        .bind(5, comic.getSeriesId())
                        .bind(6, comic.getVolume())
                        .bind(7, comic.getThumbnailUrl())
                        .bind(8, comic.getStatus())
                        .bind(9, comic.getId())
                        .execute()
        ) > 0;
    }


    public void updateComicAuthor(int comicId, int authorId) {
        String deleteSql = "DELETE FROM comic_authors WHERE comic_id = ?";
        String insertSql = "INSERT INTO comic_authors (comic_id, author_id) VALUES (?, ?)";

        jdbi.useHandle(handle -> {
            handle.createUpdate(deleteSql).bind(0, comicId).execute();
            handle.createUpdate(insertSql).bind(0, comicId).bind(1, authorId).execute();
        });
    }

    public void updateComicPublisher(int comicId, int publisherId) {
        String deleteSql = "DELETE FROM comic_publishers WHERE comic_id = ?";
        String insertSql = "INSERT INTO comic_publishers (comic_id, publisher_id) VALUES (?, ?)";

        jdbi.useHandle(handle -> {
            handle.createUpdate(deleteSql).bind(0, comicId).execute();
            handle.createUpdate(insertSql).bind(0, comicId).bind(1, publisherId).execute();
        });
    }


    public void deleteComicImages(int comicId) {
        String sql = "DELETE FROM comic_images WHERE comic_id = ?";

        jdbi.useHandle(handle ->
                handle.createUpdate(sql).bind(0, comicId).execute()
        );
    }


    public boolean softDeleteComic(int comicId) {
        String sql = "UPDATE comics SET " +
                "is_deleted = 1, " +
                "deleted_at = NOW() " +
                "WHERE id = ?";

        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind(0, comicId)
                        .execute()
        ) > 0;
    }


    public int getStockQuantity(int comicId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT stock_quantity FROM comics WHERE id = ?")
                        .bind(0, comicId)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(0)
        );
    }


    public boolean hasEnoughStock(int comicId, int requestedQuantity) {
        int stock = getStockQuantity(comicId);
        return stock >= requestedQuantity;
    }


    public boolean isComicReferenced(int comicId) {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT 1 FROM order_items WHERE comic_id = :comicId
                    UNION ALL
                    SELECT 1 FROM wishlist WHERE comic_id = :comicId
                ) AS refs
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("comicId", comicId)
                        .mapTo(Integer.class)
                        .one() > 0
        );
    }


    public boolean softDeleteComicSafely(int comicId) {
        // Kiểm tra ràng buộc trước
        if (isComicReferenced(comicId)) {
            System.out.println("⚠️ Comic ID " + comicId + " is referenced, cannot delete");
            return false;
        }

        return softDeleteComic(comicId);
    }

    public boolean toggleHidden(int id, int hidden) {
        String sql = """
                UPDATE comics set is_hidden = :hidden where id = :id and is_deleted = 0
                """;
        int row = jdbi.withHandle(handle ->
                handle.createUpdate(sql).bind("id", id).bind("hidden", hidden).execute());
        return row > 0;
    }


    public List<Comic> searchComicsAdminWithFilter(String keyword, String author,
                                                   Integer categoryId, Integer hiddenFilter,
                                                   int page, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String searchTerm = keyword.trim().toLowerCase();

        StringBuilder sql = new StringBuilder("""
                    SELECT distinct c.*, s.series_name
                    , cat.name_categories as categoryName
                    FROM Comics c
                    LEFT JOIN Series s ON c.series_id = s.id
                    LEFT JOIN Categories cat ON c.category_id = cat.id
                    WHERE c.is_deleted = 0
                """);

        // Thêm điều kiện lọc ẩn/hiện
        if (hiddenFilter != null) {
            if (hiddenFilter == 0) {
                sql.append(" AND c.is_hidden = 0 ");
            } else if (hiddenFilter == 1) {
                sql.append(" AND c.is_hidden = 1 ");
            }
        }

        sql.append("""
                    AND (
                         LOWER(c.name_comics) LIKE :keyword
                         OR LOWER(c.author) LIKE :keyword
                         OR LOWER(cat.name_categories) LIKE :keyword
                         OR LOWER(s.series_name) LIKE :keyword
                """);

        String numberOnly = keyword.replaceAll("\\D+", "");
        if (!numberOnly.isEmpty()) {
            sql.append(" OR c.volume = :volume");
        }

        sql.append(")");

        if (author != null && !author.isEmpty()) {
            sql.append(" AND LOWER(c.author) LIKE :author");
        }

        if (categoryId != null) {
            sql.append(" AND c.category_id = :categoryId");
        }

        sql.append(" ORDER BY c.created_at DESC");
        sql.append(" LIMIT :limit OFFSET :offset");

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("keyword", "%" + searchTerm + "%")
                    .bind("limit", limit)
                    .bind("offset", (page - 1) * limit);

            // Bind volume nếu có số
            if (!numberOnly.isEmpty()) {
                try {
                    int volumeNumber = Integer.parseInt(numberOnly);
                    query.bind("volume", volumeNumber);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            if (author != null && !author.isEmpty()) {
                query.bind("author", "%" + author.toLowerCase() + "%");
            }

            if (categoryId != null) {
                query.bind("categoryId", categoryId);
            }

            return query.mapToBean(Comic.class).list();
        });
    }

    /**
     * Đếm số truyện với bộ lọc ẩn/hiện
     */
    public int countComicsAdminWithFilter(String keyword, String author,
                                          Integer categoryId, Integer hiddenFilter) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0;
        }

        String searchTerm = keyword.trim().toLowerCase();

        StringBuilder sql = new StringBuilder("""
                    SELECT COUNT(DISTINCT c.id)
                    FROM Comics c
                    LEFT JOIN Series s ON c.series_id = s.id
                    LEFT JOIN Categories cat ON c.category_id = cat.id
                    WHERE c.is_deleted = 0
                """);

        // Thêm điều kiện lọc ẩn/hiện
        if (hiddenFilter != null) {
            if (hiddenFilter == 0) {
                sql.append(" AND c.is_hidden = 0 ");
            } else if (hiddenFilter == 1) {
                sql.append(" AND c.is_hidden = 1 ");
            }
        }

        sql.append("""
                AND (
                    LOWER(c.name_comics) LIKE :keyword
                    OR LOWER(c.author) LIKE :keyword
                    OR LOWER(cat.name_categories) LIKE :keyword
                    OR LOWER(s.series_name) LIKE :keyword
                """);

        String numberOnly = keyword.replaceAll("\\D+", "");
        if (!numberOnly.isEmpty()) {
            sql.append(" OR c.volume = :volume");
        }

        sql.append(")");

        if (author != null && !author.isEmpty()) {
            sql.append(" AND LOWER(c.author) LIKE :author");
        }

        if (categoryId != null) {
            sql.append(" AND c.category_id = :categoryId");
        }

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("keyword", "%" + searchTerm + "%");

            if (!numberOnly.isEmpty()) {
                try {
                    int volumeNumber = Integer.parseInt(numberOnly);
                    query.bind("volume", volumeNumber);
                } catch (NumberFormatException e) {
                }
            }

            if (author != null && !author.isEmpty()) {
                query.bind("author", "%" + author.toLowerCase() + "%");
            }

            if (categoryId != null) {
                query.bind("categoryId", categoryId);
            }

            return query.mapTo(Integer.class).one();
        });
    }

    /**
     * Lấy danh sách truyện với bộ lọc ẩn/hiện
     */
    public List<Comic> getAllComicsAdminWithFilter(int page, int limit, Integer hiddenFilter) {
        int offset = (page - 1) * limit;

        StringBuilder sql = new StringBuilder(
                "SELECT c.*, " +
                        "cat.name_categories AS categoryName, " +
                        "s.series_name AS seriesName " +
                        "FROM comics c " +
                        "LEFT JOIN categories cat ON c.category_id = cat.id " +
                        "LEFT JOIN series s ON c.series_id = s.id " +
                        "WHERE c.is_deleted = 0 "
        );

        // Thêm điều kiện lọc
        if (hiddenFilter != null) {
            if (hiddenFilter == 0) {
                sql.append("AND c.is_hidden = 0 ");
            } else if (hiddenFilter == 1) {
                sql.append("AND c.is_hidden = 1 ");
            }
        }

        sql.append("ORDER BY c.created_at DESC LIMIT ? OFFSET ?");

        return jdbi.withHandle(handle ->
                handle.createQuery(sql.toString())
                        .bind(0, limit)
                        .bind(1, offset)
                        .mapToBean(Comic.class)
                        .list()
        );
    }

    /**
     * Đếm tổng số truyện với bộ lọc ẩn/hiện
     */
    public int countAllComicsWithFilter(Integer hiddenFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM Comics WHERE is_deleted = 0"
        );

        if (hiddenFilter != null) {
            if (hiddenFilter == 0) {
                sql.append(" AND is_hidden = 0");
            } else if (hiddenFilter == 1) {
                sql.append(" AND is_hidden = 1");
            }
        }

        return jdbi.withHandle(handle ->
                handle.createQuery(sql.toString())
                        .mapTo(Integer.class)
                        .one()
        );
    }



    /**
     * Lấy danh sách tác giả trong một category
     */

    public List<String> getAuthorsByCategory(int categoryId) {
        String sql = """
                    SELECT DISTINCT a.name
                    FROM authors a
                    INNER JOIN comic_authors ca ON a.id = ca.author_id
                    INNER JOIN comics c ON ca.comic_id = c.id
                    WHERE c.category_id = :categoryId
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                      AND a.is_deleted = 0
                    ORDER BY a.name
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("categoryId", categoryId)
                        .mapTo(String.class)
                        .list()
        );
    }

    /**
     * Lấy danh sách nhà xuất bản trong một category
     */
    public List<String> getPublishersByCategory(int categoryId) {
        String sql = """
                    SELECT DISTINCT p.name
                    FROM publishers p
                    INNER JOIN comic_publishers cp ON p.id = cp.publisher_id
                    INNER JOIN comics c ON cp.comic_id = c.id
                    WHERE c.category_id = :categoryId
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                      AND p.is_deleted = 0
                    ORDER BY p.name
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("categoryId", categoryId)
                        .mapTo(String.class)
                        .list()
        );
    }


    public Map<Integer, Integer> getTotalSoldBySeriesId(int seriesId) {
        return JdbiConnector.get().withHandle(handle ->
                handle.createQuery("""
                                    SELECT c.series_id AS seriesId,
                                           COALESCE(SUM(oi.quantity), 0) AS totalSold
                                    FROM order_items oi
                                    JOIN comics c ON oi.comic_id = c.id
                                    JOIN orders o ON oi.order_id = o.id
                                    WHERE o.status = 'COMPLETED'
                                      AND c.series_id = :seriesId
                                    GROUP BY c.series_id
                                """)
                        .bind("seriesId", seriesId)
                        .map((rs, ctx) -> Map.entry(
                                rs.getInt("seriesId"),
                                rs.getInt("totalSold")
                        ))
                        .list()
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                )
                        )
        );
    }

    /**
     * Lấy danh sách truyện theo tên tác giả
     */
    public List<Comic> getComicsByAuthor(String authorName) {
        String sql = """
                SELECT c.*,
                       s.series_name AS seriesName
                """ + buildFlashSaleColumns() + """
                FROM comics c
                LEFT JOIN series s ON c.series_id = s.id
                """ + buildFlashSaleJoin() + """
                    WHERE c.id IN (
                        SELECT DISTINCT ca.comic_id
                        FROM comic_authors ca
                        JOIN authors a ON ca.author_id = a.id
                        WHERE a.name LIKE :authorName
                          AND a.is_deleted = 0
                    )
                    AND c.is_deleted = 0
                    AND (c.is_hidden IS NULL OR c.is_hidden = 0)
                    ORDER BY c.created_at DESC
                    LIMIT 50
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("authorName", "%" + authorName.trim() + "%")
                            .mapToBean(Comic.class)
                            .list()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách truyện theo tên nhà xuất bản
     */
    public List<Comic> getComicsByPublisher(String publisherName) {
        String sql = """
                SELECT DISTINCT c.*,
                       s.series_name AS seriesName
                """ + buildFlashSaleColumns() + """
                FROM comics c
                LEFT JOIN series s ON c.series_id = s.id
                """ + buildFlashSaleJoin() + """
                    WHERE c.id IN (
                         SELECT DISTINCT cp.comic_id
                         FROM comic_publishers cp
                         JOIN publishers p ON cp.publisher_id = p.id
                         WHERE p.name LIKE :publisherName
                           AND p.is_deleted = 0
                     )
                    AND c.is_deleted = 0
                    AND (c.is_hidden IS NULL OR c.is_hidden = 0)
                    ORDER BY c.created_at DESC
                    LIMIT 50
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("publisherName", "%" + publisherName.trim() + "%")
                            .mapToBean(Comic.class)
                            .list()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }



    public List<Comic> getPopularComicsWithFlashSale(int limit) {
        String sql = """
                    SELECT 
                        c.*,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND c.stock_quantity > 0
                
                    ORDER BY totalSold DESC
                    LIMIT ?
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, limit)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * JOIN với Flash Sale active
     * Chỉ lấy Flash Sale đang active (trong khung giờ start_time -> end_time)
     */
    private String buildFlashSaleJoin() {
        return """
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id AS flash_sale_id,
                            fs.name AS flash_sale_name,
                            fs.discount_percent AS flash_sale_discount,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND NOW() BETWEEN fs.start_time AND fs.end_time
                    ) AS flash_sale ON c.id = flash_sale.comic_id AND flash_sale.rn = 1
                """;
    }

    /**
     * SELECT columns cho Flash Sale
     * Map các column này vào các field trong Comic model
     */
    private String buildFlashSaleColumns() {
        return """
                    ,
                    flash_sale.flash_sale_id,
                    flash_sale.flash_sale_name,
                    flash_sale.flash_sale_discount,
                    CASE WHEN flash_sale.flash_sale_id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                    CASE 
                        WHEN flash_sale.flash_sale_id IS NOT NULL 
                        THEN ROUND(c.price * (1 - flash_sale.flash_sale_discount / 100), 0)
                        ELSE NULL
                    END AS flash_sale_price
                """;
    }


    public List<Comic> getRecommendedComicsWithFlashSale(int userId, int limit) {
        String sql = """
                WITH wishlist_series AS (
                    SELECT DISTINCT 
                        c.series_id,
                        MAX(c.volume) as max_volume
                    FROM Wishlist w
                    JOIN Comics c ON w.comic_id = c.id
                    WHERE w.user_id = :userId 
                    AND c.is_deleted = 0
                    AND c.series_id IS NOT NULL
                    GROUP BY c.series_id
                ),
                wishlist_categories AS (
                    SELECT DISTINCT c.category_id
                    FROM Wishlist w
                    JOIN Comics c ON w.comic_id = c.id
                    WHERE w.user_id = :userId 
                    AND c.is_deleted = 0
                    AND c.category_id IS NOT NULL
                ),
                next_volumes AS (
                    SELECT 
                        c.*,
                        COALESCE(SUM(oi.quantity), 0) as totalSold,
                        1 as priority
                """ + buildFlashSaleColumns() + """
                FROM Comics c
                INNER JOIN wishlist_series ws ON c.series_id = ws.series_id
                LEFT JOIN Order_Items oi ON c.id = oi.comic_id
                LEFT JOIN Orders o ON oi.order_id = o.id 
                    AND o.status NOT IN ('cancelled', 'returned')
                    AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                """ + buildFlashSaleJoin() + """
                    WHERE c.volume = ws.max_volume + 1
                    AND c.is_deleted = 0
                    AND c.is_hidden = 0
                    AND c.stock_quantity > 0
                    AND NOT EXISTS (
                        SELECT 1 FROM Wishlist w2 
                        WHERE w2.user_id = :userId AND w2.comic_id = c.id
                    )
                    GROUP BY c.id
                ),
                same_category AS (
                    SELECT 
                        c.*,
                        COALESCE(SUM(oi.quantity), 0) as totalSold,
                        2 as priority
                """ + buildFlashSaleColumns() + """
                FROM Comics c
                INNER JOIN wishlist_categories wc ON c.category_id = wc.category_id
                LEFT JOIN Order_Items oi ON c.id = oi.comic_id
                LEFT JOIN Orders o ON oi.order_id = o.id 
                    AND o.status NOT IN ('cancelled', 'returned')
                    AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                """ + buildFlashSaleJoin() + """
                        WHERE c.is_deleted = 0
                        AND c.is_hidden = 0
                        AND c.stock_quantity > 0
                        AND NOT EXISTS (
                            SELECT 1 FROM Wishlist w2 
                            WHERE w2.user_id = :userId AND w2.comic_id = c.id
                        )
                        AND c.id NOT IN (SELECT id FROM next_volumes)
                        GROUP BY c.id
                        ORDER BY RAND()
                        LIMIT :limit
                    )
                    SELECT * FROM (
                        SELECT * FROM next_volumes
                        UNION ALL
                        SELECT * FROM same_category
                    ) as final_result
                    ORDER BY priority, RAND()
                    LIMIT :limit
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("userId", userId)
                        .bind("limit", limit)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * Lấy comics theo category với Flash Sale
     */
    public List<Comic> getComicsByCategoryWithFlashSale(int categoryId, int excludeId, int limit) {
        String sql = """
                    SELECT 
                        c.*,
                        cat.name_categories,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    LEFT JOIN categories cat ON c.category_id = cat.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.category_id = ?
                      AND c.id != ?
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                      AND c.stock_quantity > 0
                
                    ORDER BY totalSold DESC, c.created_at DESC
                    LIMIT ?
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, categoryId)
                        .bind(1, excludeId)
                        .bind(2, limit)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    public List<Comic> smartSearchAllWithFlashSale(String keyword) {
        String sql = """
                    SELECT 
                        c.*,
                        cat.name_categories,
                        s.series_name,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE 
                            WHEN fs.id IS NOT NULL THEN 1 
                            ELSE 0 
                        END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    LEFT JOIN categories cat ON c.category_id = cat.id
                    LEFT JOIN series s ON c.series_id = s.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND (
                          c.name_comics LIKE ? 
                          OR c.author LIKE ? 
                          OR c.publisher LIKE ?
                          OR cat.name_categories LIKE ?
                      )
                
                    ORDER BY c.name_comics
                """;

        String searchPattern = "%" + keyword + "%";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, searchPattern)
                        .bind(1, searchPattern)
                        .bind(2, searchPattern)
                        .bind(3, searchPattern)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * Tìm theo tên với Flash Sale
     */
    public List<Comic> smartSearchWithFlashSale(String keyword) {
        String sql = """
                    SELECT 
                        c.*,
                
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND c.name_comics LIKE ?
                
                    ORDER BY c.name_comics
                """;

        String searchPattern = "%" + keyword + "%";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, searchPattern)
                        .mapToBean(Comic.class)
                        .list()
        );
    }

    /**
     * Tìm theo tác giả với Flash Sale
     */
    public List<Comic> findByAuthorWithFlashSale(String author) {
        String sql = """
                    SELECT 
                        c.*,
                
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND c.author LIKE ?
                
                    ORDER BY c.name_comics
                """;

        String searchPattern = "%" + author + "%";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, searchPattern)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * Tìm theo nhà xuất bản với Flash Sale
     */
    public List<Comic> findByPublisherWithFlashSale(String publisher) {
        String sql = """
                    SELECT 
                        c.*,
                
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND c.publisher LIKE ?
                
                    ORDER BY c.name_comics
                """;

        String searchPattern = "%" + publisher + "%";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, searchPattern)
                        .mapToBean(Comic.class)
                        .list()
        );
    }

    /**
     * Tìm theo category với Flash Sale
     */
    public List<Comic> findByCategoryWithFlashSale(String categoryName) {
        String sql = """
                    SELECT 
                        c.*,
                        cat.name_categories,
                
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    JOIN categories cat ON c.category_id = cat.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND cat.name_categories LIKE ?
                
                    ORDER BY c.name_comics
                """;

        String searchPattern = "%" + categoryName + "%";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, searchPattern)
                        .mapToBean(Comic.class)
                        .list()
        );
    }

    /**
     * Tìm theo series với Flash Sale
     */
    public List<Comic> findBySeriesIdWithFlashSale(int seriesId, int excludeId) {
        String sql = """
                    SELECT 
                        c.*,
                
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.series_id = ?
                      AND c.id != ?
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                
                    ORDER BY c.volume
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, seriesId)
                        .bind(1, excludeId)
                        .mapToBean(Comic.class)
                        .list()
        );
    }

    /**
     * Lấy comics theo category với Flash Sale
     */
    public List<Comic> getComicsByCategory1WithFlashSale(int categoryId) {
        String sql = """
                    SELECT 
                        c.*,
                        cat.name_categories,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    LEFT JOIN categories cat ON c.category_id = cat.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.category_id = ?
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                
                    ORDER BY c.name_comics
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, categoryId)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    /**
     * Lấy comics theo category với filters và Flash Sale
     */
    public List<Comic> getComicsByCategoryWithFiltersAndFlashSale(
            int categoryId,
            List<String> priceRanges,
            List<String> authors,
            List<String> publishers,
            List<String> years) {

        StringBuilder sql = new StringBuilder("""
                    SELECT 
                        c.*,
                        cat.name_categories,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    LEFT JOIN categories cat ON c.category_id = cat.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.category_id = :categoryId
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                """);

        List<String> conditions = new ArrayList<>();

        // Price filters
        if (priceRanges != null && !priceRanges.isEmpty()) {
            List<String> priceConditions = new ArrayList<>();
            for (String range : priceRanges) {
                switch (range) {
                    case "0-15000":
                        priceConditions.add("c.price BETWEEN 0 AND 15000");
                        break;
                    case "15000-30000":
                        priceConditions.add("c.price BETWEEN 15000 AND 30000");
                        break;
                    case "30000-50000":
                        priceConditions.add("c.price BETWEEN 30000 AND 50000");
                        break;
                    case "50000-70000":
                        priceConditions.add("c.price BETWEEN 50000 AND 70000");
                        break;
                    case "70000-100000":
                        priceConditions.add("c.price BETWEEN 70000 AND 100000");
                        break;
                    case "100000+":
                        priceConditions.add("c.price > 100000");
                        break;
                }
            }
            if (!priceConditions.isEmpty()) {
                conditions.add("(" + String.join(" OR ", priceConditions) + ")");
            }
        }

        // Author filters
        if (authors != null && !authors.isEmpty()) {
            List<String> authorConditions = new ArrayList<>();
            for (int i = 0; i < authors.size(); i++) {
                authorConditions.add("c.author = :author" + i);
            }
            conditions.add("(" + String.join(" OR ", authorConditions) + ")");
        }

        // Publisher filters
        if (publishers != null && !publishers.isEmpty()) {
            List<String> publisherConditions = new ArrayList<>();
            for (int i = 0; i < publishers.size(); i++) {
                publisherConditions.add("c.publisher = :publisher" + i);
            }
            conditions.add("(" + String.join(" OR ", publisherConditions) + ")");
        }

        // Year filters
        if (years != null && !years.isEmpty()) {
            List<String> yearConditions = new ArrayList<>();
            for (String year : years) {
                if ("recent".equals(year)) {
                    yearConditions.add("YEAR(c.created_at) = YEAR(CURDATE())");
                } else if ("before2020".equals(year)) {
                    yearConditions.add("YEAR(c.created_at) < 2020");
                } else {
                    yearConditions.add("YEAR(c.created_at) = " + year);
                }
            }
            if (!yearConditions.isEmpty()) {
                conditions.add("(" + String.join(" OR ", yearConditions) + ")");
            }
        }

        if (!conditions.isEmpty()) {
            sql.append(" AND ").append(String.join(" AND ", conditions));
        }

        sql.append(" ORDER BY c.name_comics");

        return jdbi.withHandle(handle -> {
            var query = handle.createQuery(sql.toString())
                    .bind("categoryId", categoryId);

            if (authors != null) {
                for (int i = 0; i < authors.size(); i++) {
                    query.bind("author" + i, authors.get(i));
                }
            }

            if (publishers != null) {
                for (int i = 0; i < publishers.size(); i++) {
                    query.bind("publisher" + i, publishers.get(i));
                }
            }

            return query.mapToBean(Comic.class).list();
        });
    }

    /**
     * Lấy tập tiếp theo với Flash Sale
     */
    public Comic getNextVolumeWithFlashSale(int seriesId, int currentVolume) {
        String sql = """
                    SELECT 
                        c.*,
                        s.series_name,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale
                
                    FROM comics c
                    LEFT JOIN series s ON c.series_id = s.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.series_id = ?
                      AND c.volume > ?
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                      AND c.stock_quantity > 0
                
                    ORDER BY c.volume ASC
                    LIMIT 1
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, seriesId)
                        .bind(1, currentVolume)
                        .mapToBean(Comic.class)
                        .findOne()
                        .orElse(null)
        );
    }
    /**
     * Lấy comics theo series với thông tin Flash Sale
     */
    /**
     * Lấy comics theo series ID với Flash Sale
     */
    public List<Comic> getComicsBySeriesIdWithFlashSale(int seriesId) {
        String sql = """
                    SELECT 
                        c.*,
                        s.series_name,
                
                        -- Flash Sale Info
                        fs.id AS flash_sale_id,
                        fs.name AS flash_sale_name,
                        fs.discount_percent AS flash_sale_discount,
                        ROUND(c.price * (1 - fs.discount_percent / 100), 0) AS flash_sale_price,
                
                        CASE WHEN fs.id IS NOT NULL THEN 1 ELSE 0 END AS has_flash_sale,
                
                        COALESCE(
                            (SELECT SUM(oi.quantity) 
                             FROM order_items oi
                             JOIN orders o ON oi.order_id = o.id
                             WHERE oi.comic_id = c.id 
                               AND o.status IN ('pending', 'confirmed', 'shipping', 'completed')),
                            0
                        ) AS totalSold
                
                    FROM comics c
                    LEFT JOIN series s ON c.series_id = s.id
                
                    LEFT JOIN (
                        SELECT 
                            fsc.comic_id,
                            fs.id,
                            fs.name,
                            fs.discount_percent,
                            ROW_NUMBER() OVER (
                                PARTITION BY fsc.comic_id 
                                ORDER BY fs.discount_percent DESC, fs.end_time ASC
                            ) AS rn
                        FROM flashsale_comics fsc
                        JOIN flashsale fs ON fsc.flashsale_id = fs.id
                        WHERE fs.status = 'active'
                          AND fs.start_time <= NOW()
                          AND fs.end_time >= NOW()
                    ) AS fs ON c.id = fs.comic_id AND fs.rn = 1
                
                    WHERE c.series_id = ?
                      AND c.is_deleted = 0
                      AND c.is_hidden = 0
                
                    ORDER BY c.volume ASC
                """;

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind(0, seriesId)
                        .mapToBean(Comic.class)
                        .list()
        );
    }


    public int getTotalSoldByComicId(int comicId) {
        String sql = """
                    SELECT COALESCE(SUM(oi.quantity), 0)
                    FROM Order_Items oi
                    JOIN Orders o ON oi.order_id = o.id
                    WHERE oi.comic_id = :comicId
                      AND o.status = 'Completed'
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("comicId", comicId)
                            .mapTo(Integer.class)
                            .one()
            );
        } catch (UnableToExecuteStatementException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Đếm số lượng truyện trong một thể loại (chỉ truyện available, không bị xóa, không bị ẩn)
     */
    public int countComicsByCategory(int categoryId) {
        String sql = """
                SELECT COUNT(*) 
                FROM comics 
                WHERE category_id = :categoryId 
                  AND is_deleted = 0 
                  AND is_hidden = 0 
                  AND status = 'available'
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("categoryId", categoryId)
                            .mapTo(Integer.class)
                            .findOne()
                            .orElse(0)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    public List<Comic> getComicsByCategoryPaginated(int categoryId, int page, int pageSize) {
        String sql = """
                SELECT 
                    c.id,
                    c.title,
                    c.description,
                    c.author,
                    c.thumbnail_url as thumbnailUrl,
                    c.category_id as categoryId,
                    c.status,
                    c.view_count as viewCount,
                    c.is_deleted as isDeleted,
                    c.is_hidden as isHidden,
                    c.created_at as createdAt,
                    c.updated_at as updatedAt,
                    cat.name_categories as categoryName
                FROM comics c
                LEFT JOIN categories cat ON c.category_id = cat.id
                WHERE c.category_id = :categoryId 
                  AND c.is_deleted = 0 
                  AND c.is_hidden = 0 
                  AND c.status = 'available'
                ORDER BY c.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        int offset = (page - 1) * pageSize;

        try {

            List<Comic> result = jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("categoryId", categoryId)
                            .bind("limit", pageSize)
                            .bind("offset", offset)
                            .mapToBean(Comic.class)
                            .list()
            );

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public int available(int comicId) {
        String sql = "SELECT (stock_quantity - damaged_quantity) FROM comics WHERE id = :id";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .bind("id", comicId)
                        .mapTo(Integer.class)
                        .one()
        );
    }

    public boolean minus_Stock_quanlity(int comicId, int quanlity) {
        String sql  = "UPDATE comics SET stock_quantity = stock_quantity - :quantity WHERE id =:comicId AND  stock_quantity >= :quantity";
        return jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("comicId", comicId)
                        .bind("quanlity", quanlity)
                        .execute()
        )>0;
    }

}
