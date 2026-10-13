package vn.edu.hcmuaf.fit.ttltw_nhom6.utils.momo;
import java.util.Map;

public class MoMoUtils {
    public static final String PARTNER_CODE = "MOMO";
    public static final String ACCESS_KEY   = "F8BBA842ECF85";
    public static final String SECRET_KEY   = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    public static final String ENDPOINT     = "https://test-payment.momo.vn/v2/gateway/api/create";
    public static final String REDIRECT_URL = "http://localhost:8080/TTLTW_Nhom6/momo-return";
    public static final String IPN_URL      = "http://localhost:8080/TTLTW_Nhom6/momo-return";
    public static final String REQUEST_TYPE  = "captureWallet";


    public static String createPaymentUrl(int orderId, long amount, String orderInfo) throws Exception {
        String requestId   = PARTNER_CODE + System.currentTimeMillis();
        String orderIdStr  = "ORDER_" + orderId + "_" + System.currentTimeMillis();
        String extraData   = "";
        String lang        = "vi";

        String rawSignature =
                "accessKey="    + ACCESS_KEY   +
                        "&amount="      + amount       +
                        "&extraData="   + extraData    +
                        "&ipnUrl="      + IPN_URL      +
                        "&orderId="     + orderIdStr   +
                        "&orderInfo="   + orderInfo    +
                        "&partnerCode=" + PARTNER_CODE +
                        "&redirectUrl=" + REDIRECT_URL +
                        "&requestId="   + requestId    +
                        "&requestType=" + REQUEST_TYPE;

        String signature = hmacSHA256(SECRET_KEY, rawSignature);

        // Build JSON body
        String jsonBody = "{"
                + "\"partnerCode\":\""  + PARTNER_CODE  + "\","
                + "\"accessKey\":\""    + ACCESS_KEY     + "\","
                + "\"requestId\":\""    + requestId      + "\","
                + "\"amount\":"         + amount         + ","
                + "\"orderId\":\""      + orderIdStr     + "\","
                + "\"orderInfo\":\""    + orderInfo      + "\","
                + "\"redirectUrl\":\""  + REDIRECT_URL   + "\","
                + "\"ipnUrl\":\""       + IPN_URL        + "\","
                + "\"lang\":\""         + lang           + "\","
                + "\"extraData\":\""    + extraData      + "\","
                + "\"requestType\":\""  + REQUEST_TYPE   + "\","
                + "\"signature\":\""    + signature      + "\""
                + "}";

        String responseStr = sendPost(ENDPOINT, jsonBody);
        String payUrl = extractField(responseStr, "payUrl");

        if (payUrl == null || payUrl.isEmpty()) {
            String errorMsg = extractField(responseStr, "message");
            throw new RuntimeException("MoMo error: " + errorMsg + " | Response: " + responseStr);
        }
        return payUrl;
    }

    private static String hmacSHA256(String key, String data) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String sendPost(String url, String jsonBody) throws Exception {
        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        try (java.util.Scanner sc = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8)) {
            return sc.useDelimiter("\\A").next();
        }
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    public static String getParam(Map<String, String[]> params, String key) {
        String[] v = params.get(key);
        return (v != null && v.length > 0) ? v[0] : null;
    }
}


