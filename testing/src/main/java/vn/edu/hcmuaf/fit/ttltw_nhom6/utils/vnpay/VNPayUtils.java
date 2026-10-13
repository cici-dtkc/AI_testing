package vn.edu.hcmuaf.fit.ttltw_nhom6.utils.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class VNPayUtils {
    public static  String VNP_TMN_CODE ;
    public static  String VNP_HASH_SECRET;
    public static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_CURRENCY = "VND";
    public static final String VNP_LOCALE = "vn";

    static {
        try {
            Properties prop = new Properties();
            prop.load(VNPayUtils.class.getClassLoader().getResourceAsStream("vnpay.properties"));
            VNP_TMN_CODE = prop.getProperty("vnp_TmnCode");
            VNP_HASH_SECRET = prop.getProperty("vnp_HashSecret");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String createPaymentUrl(int orderId, long amount, String orderInfo, String returnUrl, String ipAddress) {
        String cleanOrderInfo = removeAccent(orderInfo);
        cleanOrderInfo = cleanOrderInfo.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (cleanOrderInfo.isEmpty()) cleanOrderInfo = "Thanh toan don hang";
        if (cleanOrderInfo.length() > 255) cleanOrderInfo = cleanOrderInfo.substring(0, 255);
        // tao ma giao dich duy nhat
        String txnRef = orderId + "_" + System.currentTimeMillis();
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", VNP_COMMAND);
        params.put("vnp_TmnCode", VNP_TMN_CODE);
//vnpay yeu cau nhan 100
        params.put("vnp_Amount", String.valueOf(amount * 100L));
        params.put("vnp_CurrCode", VNP_CURRENCY);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", cleanOrderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", VNP_LOCALE);
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createDate);
        // Tạo chuỗi hash data và query string
        StringBuilder hashData = new StringBuilder();
        StringBuilder queryString = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    queryString.append('&');
                }
                hashData.append(URLEncoder.encode(key, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                queryString.append(URLEncoder.encode(key, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                first = false;
            }
        }

        String secureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        queryString.append("&vnp_SecureHash=").append(secureHash);

        return VNP_URL + "?" + queryString;
    }

    //    xac minh chu ky call back tuu vnpay
    public static boolean verifySignature(Map<String, String[]> params) {
        String vnpSecureHash = getParam(params, "vnp_SecureHash");
        if (vnpSecureHash == null) return false;

        Map<String, String> signParams = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                signParams.put(key, entry.getValue()[0]);
            }
        }

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            if (!first) hashData.append('&');
            // Encode giống hệt lúc tạo URL
            hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            first = false;
        }

        String expectedHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        return expectedHash.equalsIgnoreCase(vnpSecureHash);
    }

    //    Lấy orderId gốc từ vnp_TxnRef (format: "orderId_timestamp")
    public static int extractOrderId(String txnRef) {
        if (txnRef == null || !txnRef.contains("_")) return 0;
        try {
            return Integer.parseInt(txnRef.split("_")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static String getParam(Map<String, String[]> params, String key) {
        String[] values = params.get(key);
        return (values != null && values.length > 0) ? values[0] : null;
    }
    public static String hmacSHA512(String key, String data) {

        try {

            Mac mac = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            return "";
        }

    }
    public static String removeAccent(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^\\x00-\\x7F]", "")
                .trim();
    }

}
