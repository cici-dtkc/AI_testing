package vn.edu.hcmuaf.fit.ttltw_nhom6.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.ttltw_nhom6.config.CloudinaryConfig;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Service để upload ảnh lên Cloudinary
 */
public class CloudinaryService {

    private static final Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

    /**
     * Upload ảnh từ Part (multipart/form-data) lên Cloudinary
     */
    public static String uploadImage(Part filePart, String folder) throws IOException {
        if (filePart == null || filePart.getSize() == 0) {
            return null;
        }

        // Validate file type
        String contentType = filePart.getContentType();
        if (!isValidImageType(contentType)) {
            throw new IOException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WEBP)");
        }

        // Validate file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (filePart.getSize() > maxSize) {
            throw new IOException("Kích thước ảnh không được vượt quá 10MB");
        }

        try {
            // Tạo tên file unique
            String publicId = folder + "/" + UUID.randomUUID().toString();

            // Upload lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    filePart.getInputStream().readAllBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", false,
                            "transformation", new com.cloudinary.Transformation()
                                    .quality("auto")  // Tự động tối ưu chất lượng
                                    .fetchFormat("auto")  // Tự động chọn format tốt nhất
                    )
            );

            // Lấy URL của ảnh
            String imageUrl = (String) uploadResult.get("secure_url");

            return imageUrl;

        } catch (Exception e) {
            System.err.println("Error uploading to Cloudinary: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Lỗi khi upload ảnh: " + e.getMessage());
        }
    }

    /**
     * Xóa ảnh trên Cloudinary theo public_id
     */
    public static boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            return "ok".equals(resultStatus);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy public_id từ URL Cloudinary
     */
    public static String getPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }

        // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
            int lastDot = withoutVersion.lastIndexOf('.');
            if (lastDot > 0) {
                return withoutVersion.substring(0, lastDot);
            }
            return withoutVersion;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kiểm tra file có phải ảnh hợp lệ không
     */
    private static boolean isValidImageType(String contentType) {
        if (contentType == null) return false;

        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp");
    }
}