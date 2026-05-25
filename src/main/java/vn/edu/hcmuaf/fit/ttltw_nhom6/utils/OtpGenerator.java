package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import java.security.SecureRandom;

public class OtpGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static OtpToken generateOtp(String email, int expiryMinutes) {
        // Generates a cryptographically secure 6-digit number between 100000 and 999999
        int num = 100000 + random.nextInt(900000);
        String code = String.valueOf(num);
        long expiryTime = System.currentTimeMillis() + ((long) expiryMinutes * 60 * 1000);
        return new OtpToken(code, email, expiryTime);
    }
}
