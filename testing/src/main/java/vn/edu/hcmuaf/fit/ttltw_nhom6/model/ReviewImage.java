package vn.edu.hcmuaf.fit.ttltw_nhom6.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class ReviewImage {
    private int id;
    @ColumnName("review_id")
    private int reviewId;
    @ColumnName("image_url")
    private String imageUrl;

    // Constructor mặc định
    public ReviewImage() {}

    // Constructor đầy đủ
    public ReviewImage(int id, int reviewId, String imageUrl) {
        this.id = id;
        this.reviewId = reviewId;
        this.imageUrl = imageUrl;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "ReviewImage{" +
                "id=" + id +
                ", reviewId=" + reviewId +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
