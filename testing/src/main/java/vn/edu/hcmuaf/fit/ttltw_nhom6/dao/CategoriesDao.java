package vn.edu.hcmuaf.fit.ttltw_nhom6.dao;

import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category;


import java.util.ArrayList;
import java.util.List;

public class CategoriesDao {
    private Jdbi jdbi;

    public CategoriesDao() {
        try {
            this.jdbi = JdbiConnector.get();
            System.out.println("✓ CategoriesDao initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ ERROR initializing CategoriesDao:");
            e.printStackTrace();
            throw e;
        }
    }

    public List<Category> listCategories() {
        String sql = "SELECT id, " +
                "name_categories as nameCategories, " +
                "description, " +
                "is_deleted as isDeleted, " +
                "is_hidden AS is_hidden, " +

                "deleted_at as deletedAt, " +
                "created_at as createdAt " +
                "FROM categories WHERE is_deleted = 0 ORDER BY name_categories";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Category.class)
                        .list()
        );
    }

    public List<Category> getAllCategories() {
        String sql = "SELECT id, " +
                "name_categories as nameCategories, " +
                "description, " +
                "is_deleted as isDeleted, " +
                "is_hidden AS is_hidden, " +
                "deleted_at as deletedAt, " +
                "created_at as createdAt " +
                "FROM categories WHERE is_deleted = 0 ORDER BY name_categories ASC";

        return jdbi.withHandle(handle ->
                handle.createQuery(sql)
                        .mapToBean(Category.class)
                        .list()
        );
    }

    public int getTotalCategories() {
        String sql = "SELECT COUNT(*) FROM categories WHERE is_deleted = 0";

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .mapTo(Integer.class)
                            .findOne()
                            .orElse(0)
            );
        } catch (Exception e) {
            System.err.println("Error in getTotalCategories:");
            e.printStackTrace();
            return 0;
        }
    }

    public int countSearchResults(String keyword) {
        String sql = "SELECT COUNT(*) FROM categories " +
                "WHERE name_categories LIKE :keyword AND is_deleted = 0";

        try {
            return jdbi.withHandle(handle -> handle.createQuery(sql)
                    .bind("keyword", "%" + keyword + "%")
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(0)
            );
        } catch (Exception e) {
            System.err.println("Error in countSearchResults:");
            e.printStackTrace();
            return 0;
        }
    }

    public List<Category> searchCategoriesByName(String keyword, int page, int pageSize) {
        String sql = "SELECT id, " +
                "name_categories as nameCategories, " +
                "description, " +
                "is_deleted as isDeleted, " +
                "is_hidden AS is_hidden, " +
                "deleted_at as deletedAt, " +
                "created_at as createdAt " +
                "FROM categories " +
                "WHERE name_categories LIKE :keyword AND is_deleted = 0 " +
                "ORDER BY name_categories ASC " +
                "LIMIT :limit OFFSET :offset";

        int offset = (page - 1) * pageSize;

        try {
            return jdbi.withHandle(handle -> handle.createQuery(sql)
                    .bind("keyword", "%" + keyword + "%")
                    .bind("limit", pageSize)
                    .bind("offset", offset)
                    .mapToBean(Category.class)
                    .list()
            );
        } catch (Exception e) {
            System.err.println("Error in searchCategoriesByName:");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Lấy danh sách thể loại theo trang
     */
    public List<Category> getCategoriesByPage(int page, int pageSize) {
        String sql = "SELECT id, " +
                "name_categories as nameCategories, " +
                "description, " +
                "is_deleted as isDeleted, " +
                "is_hidden AS is_hidden, " +
                "deleted_at as deletedAt, " +
                "created_at as createdAt " +
                "FROM categories " +
                "WHERE is_deleted = 0 " +
                "ORDER BY id ASC " +
                "LIMIT :limit OFFSET :offset";

        int offset = (page - 1) * pageSize;

        try {
            System.out.println("Getting categories - Page: " + page + ", PageSize: " + pageSize + ", Offset: " + offset);
            List<Category> result = jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("limit", pageSize)
                            .bind("offset", offset)
                            .mapToBean(Category.class)
                            .list()
            );
            System.out.println("Retrieved " + result.size() + " categories");
            return result;
        } catch (Exception e) {
            System.err.println("Error in getCategoriesByPage:");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Lấy thể loại theo ID - SỬA LẠI SELECT *
     */
    public Category getCategoryById(int id) {
        String sql = "SELECT " +
                "id, " +
                "name_categories as nameCategories, " +
                "description, " +
                "is_deleted as isDeleted, " +
                "is_hidden AS is_hidden, " +
                "deleted_at as deletedAt, " +
                "created_at as createdAt " +
                "FROM categories " +
                "WHERE id = :id AND is_deleted = 0";

        try {
            System.out.println("=== getCategoryById called ===");
            System.out.println("ID: " + id);
            System.out.println("SQL: " + sql);

            Category result = jdbi.withHandle(handle -> {
                System.out.println("Handle created, executing query...");

                return handle.createQuery(sql)
                        .bind("id", id)
                        .mapToBean(Category.class)
                        .findOne()
                        .orElse(null);
            });

            if (result != null) {
                System.out.println("✓ Found category:");
                System.out.println("  - ID: " + result.getId());
                System.out.println("  - Name: " + result.getNameCategories());
                System.out.println("  - Description: " + result.getDescription());
            } else {
                System.out.println("✗ No category found with ID: " + id);
            }

            return result;

        } catch (Exception e) {
            System.err.println("✗✗✗ ERROR in getCategoryById ✗✗✗");
            System.err.println("ID parameter: " + id);
            e.printStackTrace();
            throw new RuntimeException("Error getting category by ID: " + id, e);
        }
    }

    /**
     * Thêm thể loại mới
     */
    public boolean addCategory(Category category) {
        String sql = "INSERT INTO categories (name_categories, description, created_at) " +
                "VALUES (:name, :description, NOW())";

        try {
            jdbi.useHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("name", category.getNameCategories())
                            .bind("description", category.getDescription())
                            .execute()
            );
            System.out.println("✓ Category added successfully: " + category.getNameCategories());
            return true;
        } catch (Exception e) {
            System.err.println("✗ Error adding category:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thể loại
     */
    public boolean updateCategory(Category category) {
        String sql = """
                    UPDATE categories
                    SET name_categories = :name,
                        description = :description
                    WHERE id = :id AND is_deleted = 0
                """;

        int rows = jdbi.withHandle(handle ->
                handle.createUpdate(sql)
                        .bind("id", category.getId())
                        .bind("name", category.getNameCategories())
                        .bind("description", category.getDescription())
                        .execute()
        );

        return rows > 0;
    }

    /**
     * Xóa mềm thể loại (soft delete)
     */
    public boolean deleteCategory(int id) {
        String sql = "UPDATE categories " +
                "SET is_deleted = 1, deleted_at = NOW() " +
                "WHERE id = :id";

        try {
            int rowsAffected = jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("id", id)
                            .execute()
            );
            System.out.println("Category deleted - Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (Exception e) {
            System.err.println("Error deleting category:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra tên thể loại đã tồn tại chưa (để validate)
     */
    public boolean isNameExists(String name, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM categories " +
                "WHERE name_categories = :name AND is_deleted = 0";

        if (excludeId != null) {
            sql += " AND id != :excludeId";
        }

        String finalSql = sql;

        try {
            int count = jdbi.withHandle(handle -> {
                var query = handle.createQuery(finalSql).bind("name", name);
                if (excludeId != null) {
                    query.bind("excludeId", excludeId);
                }
                return query.mapTo(Integer.class).findOne().orElse(0);
            });

            return count > 0;
        } catch (Exception e) {
            System.err.println("Error in isNameExists:");
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Đếm số lượng truyện trong một thể loại
     * Chỉ đếm những truyện không bị xóa, không bị ẩn và đang available
     */
    public int countComicsInCategory(int categoryId) {
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
            System.err.println("Error counting comics in category " + categoryId + ":");
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Lấy danh sách thể loại kèm số lượng truyện
     * Trả về List<Category> với thêm thuộc tính comicCount
     */
    public List<Category> listCategoriesWithCount() {
        String sql = """
                    SELECT 
                        c.id,
                        c.name_categories as nameCategories,
                        c.description,
                        c.is_deleted as isDeleted,
                        c.is_hidden AS is_hidden,
                        c.deleted_at as deletedAt,
                        c.created_at as createdAt,
                        COUNT(DISTINCT cm.id) as comicCount
                    FROM categories c
                    LEFT JOIN comics cm ON c.id = cm.category_id 
                        AND cm.is_deleted = 0 
                        AND cm.is_hidden = 0 
                        AND cm.status = 'available'
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                    GROUP BY c.id, c.name_categories, c.description, c.is_deleted, 
                             c.is_hidden, c.deleted_at, c.created_at
                    ORDER BY c.name_categories ASC
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .mapToBean(Category.class)
                            .list()
            );
        } catch (Exception e) {
            System.err.println("Error listing categories with count:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách thể loại có ít nhất 1 truyện
     */
    public List<Category> listCategoriesWithComics() {
        String sql = """
                    SELECT DISTINCT
                        c.id,
                        c.name_categories as nameCategories,
                        c.description,
                        c.is_deleted as isDeleted,
                        c.is_hidden AS is_hidden,
                        c.deleted_at as deletedAt,
                        c.created_at as createdAt
                    FROM categories c
                    INNER JOIN comics cm ON c.id = cm.category_id
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                      AND cm.is_deleted = 0 
                      AND cm.is_hidden = 0 
                      AND cm.status = 'available'
                    ORDER BY c.name_categories ASC
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .mapToBean(Category.class)
                            .list()
            );
        } catch (Exception e) {
            System.err.println("Error listing categories with comics:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Kiểm tra category có truyện hay không
     */
    public boolean hasComics(int categoryId) {
        return countComicsInCategory(categoryId) > 0;
    }


    /**
     * Toggle ẩn/hiện category
     */
    public boolean toggleHidden(int id, int hidden) {
        String sql = """
            UPDATE categories 
            SET is_hidden = :hidden 
            WHERE id = :id AND is_deleted = 0
            """;
        try {
            int rows = jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("id", id)
                            .bind("hidden", hidden)
                            .execute()
            );
            if (rows > 0) {
                hideComicsByCategory(id, hidden); // ẩn/hiện comic theo
            }
            return rows > 0;
        } catch (Exception e) {
            System.err.println("Error toggling category hidden status:");
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Lấy thể loại phổ biến nhất (có nhiều truyện nhất)
     */
    public List<Category> getTopCategories(int limit) {
        String sql = """
                    SELECT 
                        c.id,
                        c.name_categories as nameCategories,
                        c.description,
                        c.is_deleted as isDeleted,
                        c.is_hidden AS is_hidden,
                        c.deleted_at as deletedAt,
                        c.created_at as createdAt,
                        COUNT(DISTINCT cm.id) as comicCount
                    FROM categories c
                    INNER JOIN comics cm ON c.id = cm.category_id 
                        AND cm.is_deleted = 0 
                        AND cm.is_hidden = 0 
                        AND cm.status = 'available'
                    WHERE c.is_deleted = 0 
                      AND c.is_hidden = 0
                    GROUP BY c.id, c.name_categories, c.description, c.is_deleted, 
                             c.is_hidden, c.deleted_at, c.created_at
                    ORDER BY comicCount DESC
                    LIMIT :limit
                """;

        try {
            return jdbi.withHandle(handle ->
                    handle.createQuery(sql)
                            .bind("limit", limit)
                            .mapToBean(Category.class)
                            .list()
            );
        } catch (Exception e) {
            System.err.println("Error getting top categories:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        CategoriesDao dao = new CategoriesDao();

        System.out.println("=== Testing getCategoryById ===");
        Category cat = dao.getCategoryById(2);

        if (cat != null) {
            System.out.println("SUCCESS!");
            System.out.println("ID: " + cat.getId());
            System.out.println("Name: " + cat.getNameCategories());
            System.out.println("Description: " + cat.getDescription());
        } else {
            System.out.println("FAILED - returned null");
        }
    }
    public boolean hideComicsByCategory(int categoryId, int hidden) {
        String sql = """
            UPDATE comics 
            SET is_hidden = :hidden 
            WHERE category_id = :categoryId AND is_deleted = 0
            """;
        try {
            int rows = jdbi.withHandle(handle ->
                    handle.createUpdate(sql)
                            .bind("categoryId", categoryId)
                            .bind("hidden", hidden)
                            .execute()
            );
            return rows >= 0;
        } catch (Exception e) {
            System.err.println("Error hiding comics by category:");
            e.printStackTrace();
            return false;
        }
    }


}