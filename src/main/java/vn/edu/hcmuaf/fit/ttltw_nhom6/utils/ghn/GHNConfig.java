package vn.edu.hcmuaf.fit.ttltw_nhom6.utils.ghn;

import org.cloudinary.json.JSONArray;
import org.cloudinary.json.JSONObject;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.Order;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class GHNConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = GHNConfig.class.getClassLoader()
                .getResourceAsStream("ghn.properties")) {
            if (is != null) props.load(is);
            else System.err.println("Không tìm thấy ghn.properties!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getApiUrl() {
        return props.getProperty("ghn.api.url");
    }

    public static String getToken() {
        return props.getProperty("ghn.api.token");
    }

    public static int getShopId() {
        return Integer.parseInt(props.getProperty("ghn.shop_id"));
    }

    public static int getSenderDistrictId() {
        return Integer.parseInt(props.getProperty("ghn.sender.district_id"));
    }

    public static String getSenderWardCode() {
        return props.getProperty("ghn.sender.ward_code");
    }

    public static String getSenderName() {
        return props.getProperty("ghn.sender.name");
    }

    public static String getSenderPhone() {
        return props.getProperty("ghn.sender.phone");
    }

    public static String getSenderAddress() {
        return props.getProperty("ghn.sender.address");
    }

    private static JSONObject sendPostRequest(String endpoint, JSONObject body) throws Exception {
        URL url = new URL(getApiUrl() + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Token", getToken());
        conn.setRequestProperty("ShopId", String.valueOf(getShopId()));
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
        }
        int responseCode = conn.getResponseCode();
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }
        StringBuilder responseStr = new StringBuilder();
        if (inputStream != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseStr.append(line);
                }
            }
        }
//        test nha
        System.out.println("GHN REQUEST");
        System.out.println("URL: " + url);
        System.out.println("BODY: " + body.toString());
        System.out.println("GHN RESPONSE");
        System.out.println("HTTP CODE: " + responseCode);
        System.out.println("BODY: " + responseStr);
        conn.disconnect();
        if (responseStr.length() == 0) {
            throw new RuntimeException(
                    "GHN trả về HTTP " + responseCode + " nhưng không có nội dung."
            );
        }
        return new JSONObject(responseStr.toString());
    }
    public static int getProvinceIdByName(String provinceName) throws Exception {
        JSONObject body = new JSONObject();
        JSONObject response = sendPostRequest("/master-data/province", body);
        JSONArray provinces = response.getJSONArray("data");
        for (int i = 0; i < provinces.length(); i++) {
            JSONObject province = provinces.getJSONObject(i);
            if (normalize(province.getString("ProvinceName")).equals(normalize(provinceName))) {
                return province.getInt("ProvinceID");
            }
        }
        throw new Exception("Province not found");
    }
    public static String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().replace("thanh pho", "").replace("tp", "").replace("tinh", "").replace("quan", "").replace("huyen", "").replace("phuong", "").replace("xa", "").replaceAll("\\s+", " ").trim();
    }
    public static int getDistrictIdByName(String districtName, int provinceId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("province_id", provinceId);
        JSONObject response = sendPostRequest("/master-data/district", body);
        JSONArray districts = response.getJSONArray("data");
        for (int i = 0; i < districts.length(); i++) {
            JSONObject district = districts.getJSONObject(i);
            if (normalize(district.getString("DistrictName")).equals(normalize(districtName))) {
                return district.getInt("DistrictID");
            }
        }
        throw new Exception("District not found");
    }
    public static String getWardCodeByName(String wardName, int districtId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("district_id", districtId);
        JSONObject response = sendPostRequest("/master-data/ward", body);
        JSONArray wards = response.getJSONArray("data");
        for (int i = 0; i < wards.length(); i++) {
            JSONObject ward = wards.getJSONObject(i);

            if (normalize(ward.getString("WardName")).equals(normalize(wardName))) {
                return ward.getString("WardCode");
            }
        }
        throw new Exception("Ward not found");
    }
    public static JSONArray getAvailableServices(int toDistrictId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("shop_id", getShopId());
        body.put("from_district", getSenderDistrictId());
        body.put("to_district", toDistrictId);
        JSONObject response = sendPostRequest("/v2/shipping-order/available-services", body);
        return response.getJSONArray("data");
    }
    public static int getServiceId(int toDistrictId) throws Exception {
        JSONArray services = getAvailableServices(toDistrictId);
        for (int i = 0; i < services.length(); i++) {
            JSONObject s = services.getJSONObject(i);
            int serviceType = s.optInt("service_type_id");
            if (serviceType == 2) {
                return s.getInt("service_id");
            }
        }
        if (services.length() > 0) {
            return services.getJSONObject(0).getInt("service_id");
        }
        throw new Exception("No service available");
    }

    public static String createOrder(Order order) {
        try {

            String shippingAddress = order.getShippingAddress();
            System.out.println("Shipping Address: " + shippingAddress);

            String[] address = shippingAddress.split(",");

            if (address.length < 4) {
                throw new RuntimeException(
                        "Địa chỉ phải có dạng: Số nhà, Xã/Phường, Huyện/Quận, Tỉnh/Thành phố"
                );
            }

            for (int i = 0; i < address.length; i++) {
                address[i] = address[i].trim();
            }

            String wardName = address[address.length - 3];
            String districtName = address[address.length - 2];
            String provinceName = address[address.length - 1];

            System.out.println("Ward: " + wardName);
            System.out.println("District: " + districtName);
            System.out.println("Province: " + provinceName);

            int toProvinceId = getProvinceIdByName(provinceName);

            int toDistrictId = getDistrictIdByName(
                    districtName,
                    toProvinceId
            );

            String toWardCode = getWardCodeByName(
                    wardName,
                    toDistrictId
            );

            int serviceId = getServiceId(toDistrictId);

            String phone = String.valueOf(order.getShippingPhone());

            if (!phone.startsWith("0")) {
                phone = "0" + phone;
            }

            JSONObject body = new JSONObject();

            body.put("payment_type_id", 2);
            body.put("required_note", "CHOXEMHANGKHONGTHU");

            body.put("client_order_code", String.valueOf(order.getId()));

            body.put("to_name", order.getRecipientName());
            body.put("to_phone", phone);

            body.put("to_ward_code", toWardCode);
            body.put("to_district_id", toDistrictId);

            body.put("cod_amount", (int) order.getTotalAmount());

            body.put("weight", 500);
            body.put("length", 15);
            body.put("width", 15);
            body.put("height", 15);

            body.put("service_id", serviceId);

            JSONArray items = new JSONArray();

            JSONObject item = new JSONObject();
            item.put("name", "Đơn hàng #" + order.getId());
            item.put("quantity", 1);
            item.put("price", (int) order.getTotalAmount());
            item.put("length", 15);
            item.put("width", 15);
            item.put("height", 15);
            item.put("weight", 500);

            items.put(item);

            body.put("items", items);

            System.out.println("===== GHN REQUEST =====");
            System.out.println(body.toString(2));

            JSONObject response = sendPostRequest(
                    "/v2/shipping-order/create",
                    body
            );
//test nha
            System.out.println("GHN RESPONSE");
            System.out.println(response.toString(2));

            if (response.has("code") && response.getInt("code") == 200) {
                return response.getJSONObject("data")
                        .getString("order_code");
            }

            throw new RuntimeException(
                    "GHN Error: " + response.toString()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
