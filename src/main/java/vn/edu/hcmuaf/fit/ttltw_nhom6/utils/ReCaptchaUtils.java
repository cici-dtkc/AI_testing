package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ReCaptchaUtils {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String DEFAULT_SECRET_KEY = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";

    public static boolean verify(String gRecaptchaResponse) {
        if (gRecaptchaResponse == null || gRecaptchaResponse.trim().isEmpty()) {
            return false;
        }
        try {
            String secretKey = System.getenv("RECAPTCHA_SECRET_KEY");
            if (secretKey == null || secretKey.trim().isEmpty()) {
                secretKey = DEFAULT_SECRET_KEY;
            }
            String postParams = "secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8) + "&response=" + URLEncoder.encode(gRecaptchaResponse, StandardCharsets.UTF_8);
            URL obj = URI.create(VERIFY_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postParams.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject jsonObject = new JSONObject(response.toString());
                return jsonObject.getBoolean("success");
            } else {
                System.err.println("[ReCaptchaUtils] Yêu cầu xác thực thất bại, HTTP code: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("[ReCaptchaUtils] Lỗi khi kết nối API reCAPTCHA: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
