package vn.edu.hcmuaf.fit.ttltw_nhom6.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

/**
 * Cấu hình Cloudinary để upload ảnh
 */
public class CloudinaryConfig {

    private static Cloudinary cloudinary;


    private static final String CLOUD_NAME = "dj4bwkuul";
    private static final String API_KEY = "437582248686893";
    private static final String API_SECRET = "RZEOAFHqLWdHMFJtJTCQnGio5Go";

    /**
     * Lấy instance Cloudinary (Singleton pattern)
     */
    public static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", CLOUD_NAME,
                    "api_key", API_KEY,
                    "api_secret", API_SECRET,
                    "secure", true
            ));
        }
        return cloudinary;
    }
}