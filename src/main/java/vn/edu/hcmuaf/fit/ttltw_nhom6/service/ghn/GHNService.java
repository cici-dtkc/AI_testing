package vn.edu.hcmuaf.fit.ttltw_nhom6.service.ghn;

import com.google.gson.*;
import java.io.*;
import java.net.*;

public class GHNService {
    private static final String TOKEN     = "bb67bb35-1796-11f1-b2c0-1ab1fd37f3b7";
    private static final int    SHOP_ID   = 6302109;
    private static final String BASE_URL  = "https://online-gateway.ghn.vn/shiip/public-api";
    public static String createOrder(
            String toName, String toPhone,
            String toAddress, String toWardCode, int toDistrictId,
            int weight, long codAmount,
            int serviceId, String note) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("payment_type_id", 2);       // người gửi trả phí
        body.addProperty("note", note != null ? note : "");
        body.addProperty("required_note", "KHONGCHOXEMHANG");
        body.addProperty("to_name", toName);
        body.addProperty("to_phone", toPhone);
        body.addProperty("to_address", toAddress);
        body.addProperty("to_ward_code", toWardCode);
        body.addProperty("to_district_id", toDistrictId);
        body.addProperty("cod_amount", (int) codAmount);
        body.addProperty("weight", weight);
        body.addProperty("service_id", serviceId);
        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("name", "Truyện tranh");
        item.addProperty("quantity", 1);
        item.addProperty("weight", weight);
        items.add(item);
        body.add("items", items);
        JsonObject response = callPost(BASE_URL + "/v2/shipping-order/create", body, true);
        if (response.get("code").getAsInt() != 200) {
            throw new RuntimeException("GHN lỗi: " + response.get("message").getAsString());
        }
        return response.getAsJsonObject("data").get("order_code").getAsString();
    }
    public static JsonObject trackOrder(String ghnOrderCode) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("order_code", ghnOrderCode);

        JsonObject response = callPost(BASE_URL + "/v2/shipping-order/detail", body, false);
        if (response.get("code").getAsInt() != 200) {
            throw new RuntimeException("GHN lỗi: " + response.get("message").getAsString());
        }
        return response.getAsJsonObject("data");
    }
    private static JsonObject callPost(String url, JsonObject body, boolean needShopId)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Token", TOKEN);
        if (needShopId) {
            conn.setRequestProperty("ShopId", String.valueOf(SHOP_ID));
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }
}