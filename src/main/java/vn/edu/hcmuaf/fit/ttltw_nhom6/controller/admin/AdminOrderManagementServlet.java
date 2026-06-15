package vn.edu.hcmuaf.fit.ttltw_nhom6.controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.NotificationService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.OrderService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.utils.gson.GsonConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/orders")
public class AdminOrderManagementServlet extends HttpServlet {
    private OrderService orderService;
    private NotificationService notificationService;
    private Gson gson;

    @Override
    public void init() throws ServletException {

        try {
            orderService = new OrderService();
            notificationService = NotificationService.getInstance();
            gson = GsonConfig.getGson();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");

        try {
            if (action == null) {
                displayOrderManagement(req, resp);
            } else {
                switch (action) {
                    case "detail":
                        getOrderDetail(req, resp);
                        break;
                    case "search":
                        searchOrders(req, resp);
                        break;
                    case "searchByTab":
                        searchOrdersByTab(req, resp);
                        break;
                    case "stats":
                        getStatistics(req, resp);
                        break;
                    case "loadTab":
                        loadTabData(req, resp);
                        break;
                    case "searchSuggestions":
                        searchSuggestions(req, resp);
                        break;
                    default:
                        displayOrderManagement(req, resp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void searchSuggestions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String keyword = req.getParameter("keyword");
        String status  = req.getParameter("status");

        if (keyword == null || keyword.trim().isEmpty()) {
            resp.getWriter().write(gson.toJson(Map.of("success", true, "suggestions", new ArrayList<>())));
            return;
        }

        try {
            List<Map<String, Object>> orders;
            switch (status != null ? status : "") {
                case "Pending":
                    orders = orderService.searchPendingOrders(keyword.trim());
                    break;
                case "AwaitingPickup":
                    orders = orderService.searchAwaitingPickupOrders(keyword.trim());
                    break;
                case "Shipping":
                    orders = orderService.searchShippingOrders(keyword.trim());
                    break;
                case "Completed":
                    orders = orderService.searchCompletedOrders(keyword.trim());
                    break;
                case "Returned":
                    orders = orderService.searchReturnedOrders(keyword.trim());
                    break;
                case "Cancelled":
                    orders = orderService.searchCancelledOrders(keyword.trim());
                    break;
                default:
                    orders = new ArrayList<>();
            }

            List<Map<String, Object>> suggestions = orders.stream()
                    .limit(8)
                    .map(order -> {
                        Map<String, Object> s = new HashMap<>();
                        Object code = order.get("orderCode");
                        if (code == null) code = order.get("order_code");
                        if (code == null) code = order.get("id");
                        s.put("orderCode", code != null ? String.valueOf(code) : "");

                        Object name = order.get("userName");
                        if (name == null) name = order.get("user_name");
                        if (name == null) name = order.get("customer_name");
                        if (name == null) name = order.get("recipientName");
                        s.put("userName", name != null ? String.valueOf(name) : "");

                        return s;
                    })
                    .collect(java.util.stream.Collectors.toList());

            resp.getWriter().write(gson.toJson(Map.of("success", true, "suggestions", suggestions
            )));

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(gson.toJson(Map.of("success", false, "suggestions", new ArrayList<>()
            )));
        }
    }

    private void loadTabData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");

        String status = req.getParameter("status");

        if (status == null || status.isEmpty()) {
            resp.getWriter().write(gson.toJson(Map.of(
                    "success", false,
                    "message", "Missing status parameter"
            )));
            return;
        }

        try {
            List<Map<String, Object>> orders;

            switch (status) {
                case "AwaitingPickup":
                    orders = orderService.getOrdersWithDetailsByStatus("AwaitingPickup");
                    break;
                case "Shipping":
                    orders = orderService.getOrdersWithDetailsByStatus("Shipping");
                    break;
                case "Completed":
                    orders = orderService.searchCompletedOrders("Completed");
                    break;
                case "Returned":
                    orders = orderService.getAllReturnsWithDetails();
                    break;
                case "Cancelled":
                    orders = orderService.getOrdersWithDetailsByStatus("Cancelled");
                    break;
                default:
                    orders = new ArrayList<>();
            }

            resp.getWriter().write(gson.toJson(Map.of(
                    "success", true,
                    "orders", orders
            )));

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(gson.toJson(Map.of(
                    "success", false,
                    "message", e.getMessage()
            )));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String action = req.getParameter("action");

        if (action == null) {
            sendJsonError(resp, "Missing action parameter");
            return;
        }

        try {
            Map<String, Object> result;

            switch (action) {
                case "confirm":
                    result = confirmOrder(req);
                    break;
                case "confirmAll":
                    result = confirmAllOrders(req);
                    break;
                case "cancel":
                    result = cancelOrder(req);
                    break;
                case "confirmShipped":
                    result = confirmShipped(req);
                    break;
                case "confirmDelivered":
                    result = confirmDelivered(req);
                    break;
                case "updateStatus":
                    result = updateOrderStatusWithSync(req);
                    break;
                case "processRefund":
                    result = processRefund(req);
                    break;
                case "confirmRefund":
                    result = handleConfirmRefund(req);
                    break;
                case "rejectRefund":
                    result = handleRejectRefund(req);
                    break;
                case "searchReturns":
                    result = handleSearchReturns(req);
                    break;
                case "getReturnDetail":
                    result = handleGetReturnDetail(req);
                    break;
                default:
                    result = Map.of("success", false, "message", "Invalid action");
            }

            sendJson(resp, result);

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(resp, Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> handleGetReturnDetail(HttpServletRequest req) {
        try {
            int returnId = Integer.parseInt(req.getParameter("returnId"));
            return orderService.getReturnDetail(returnId);

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid return ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> handleSearchReturns(HttpServletRequest req) {
        try {
            String keyword = req.getParameter("keyword");
            if (keyword == null) keyword = "";

            List<Map<String, Object>> returns = orderService.searchReturns(keyword);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("returns", returns);
            result.put("count", returns.size());

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> handleRejectRefund(HttpServletRequest req) {
        try {
            int returnId = Integer.parseInt(req.getParameter("returnId"));
            String rejectReason = req.getParameter("reason");

            Map<String, Object> result = orderService.rejectRefund(returnId, rejectReason);

            if ((Boolean) result.get("success")) {
                Map<String, Object> returnInfo = orderService.getReturnBasicInfo(returnId);

                if (returnInfo != null && !returnInfo.isEmpty()) {
                    Object userIdObj = returnInfo.get("userId");
                    Object orderCodeObj = returnInfo.get("orderCode");

                    if (userIdObj != null && orderCodeObj != null) {
                        try {
                            int userId = ((Number) userIdObj).intValue();
                            String orderCode = String.valueOf(orderCodeObj);
                            notificationService.notifyRefundRejected(userId, orderCode, rejectReason);
                        } catch (Exception e) {
                            System.err.println("Error sending notification: " + e.getMessage());
                        }
                    }
                }
            }
            return result;

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid return ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> handleConfirmRefund(HttpServletRequest req) {
        try {
            int returnId = Integer.parseInt(req.getParameter("returnId"));

            Map<String, Object> result = orderService.confirmRefund(returnId);

            if ((Boolean) result.get("success")) {
                Map<String, Object> returnInfo = orderService.getReturnBasicInfo(returnId);

                if (returnInfo != null && !returnInfo.isEmpty()) {
                    Object userIdObj = returnInfo.get("userId");
                    Object orderCodeObj = returnInfo.get("orderCode");
                    Object refundAmountObj = returnInfo.get("formattedRefundAmount");

                    if (userIdObj != null && orderCodeObj != null) {
                        try {
                            int userId = ((Number) userIdObj).intValue();
                            String orderCode = String.valueOf(orderCodeObj);
                            String refundAmount = refundAmountObj != null ?
                                    String.valueOf(refundAmountObj) : "N/A";

                            notificationService.notifyRefundApproved(userId, orderCode, refundAmount);
                        } catch (Exception e) {
                            System.err.println("Error sending notification: " + e.getMessage());
                        }
                    }
                }
            }

            return result;

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid return ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }


    private void displayOrderManagement(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            Map<String, Object> data = orderService.getAllOrdersWithDetails();


            if ((Boolean) data.get("success")) {
                @SuppressWarnings("unchecked")
                Map<String, List<Map<String, Object>>> ordersByStatus =
                        (Map<String, List<Map<String, Object>>>) data.get("ordersByStatus");

                req.setAttribute("ordersByStatus", ordersByStatus);

                List<Map<String, Object>> returnOrders = orderService.getAllReturnsWithDetails();
                ordersByStatus.put("Returns", returnOrders);

                Map<String, Object> stats = orderService.getOrderStatistics();
                req.setAttribute("stats", stats);

            } else {
                req.setAttribute("message", data.get("message"));
            }

            req.getRequestDispatcher("/frontend/admin/order.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("message", "Lỗi khi tải danh sách đơn hàng: " + e.getMessage());
            req.getRequestDispatcher("/frontend/admin/order.jsp")
                    .forward(req, resp);
        }
    }

    private void getOrderDetail(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            Map<String, Object> result = orderService.getOrderDetail(orderId);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(result));

        } catch (NumberFormatException e) {
            sendJsonError(resp, "Invalid order ID");
        } catch (Exception e) {
            sendJsonError(resp, e.getMessage());
        }
    }


    private void searchOrders(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            String keyword = req.getParameter("keyword");
            String status = req.getParameter("status");

            List<Map<String, Object>> orders = orderService.searchOrders(keyword, status);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(Map.of(
                    "success", true,
                    "orders", orders
            )));

        } catch (Exception e) {
            sendJsonError(resp, e.getMessage());
        }
    }


    private void getStatistics(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            Map<String, Object> stats = orderService.getOrderStatistics();

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(gson.toJson(Map.of(
                    "success", true,
                    "stats", stats
            )));

        } catch (Exception e) {
            sendJsonError(resp, e.getMessage());
        }
    }


    private Map<String, Object> confirmAllOrders(HttpServletRequest req) {
        try {
            List<Map<String, Object>> pendingOrders = orderService.getOrdersWithDetailsByStatus("Pending");

            if (pendingOrders.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "Không có đơn hàng nào cần xác nhận"
                );
            }

            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (Map<String, Object> order : pendingOrders) {
                try {
                    int orderId = (Integer) order.get("id");
                    Map<String, Object> result = orderService.confirmOrder(orderId, null);

                    if ((Boolean) result.get("success")) {
                        successCount++;

                        int userId = (Integer) order.get("userId");
                        String orderCode = (String) order.get("orderCode");
                        notificationService.notifyOrderConfirmed(userId, orderCode);
                    } else {
                        failCount++;
                        errors.add("Đơn #" + orderId + ": " + result.get("message"));
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add("Lỗi xử lý đơn: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", successCount > 0);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("totalOrders", pendingOrders.size());

            if (successCount > 0 && failCount == 0) {
                response.put("message", "Đã xác nhận thành công " + successCount + " đơn hàng!");
            } else if (successCount > 0 && failCount > 0) {
                response.put("message", "Xác nhận thành công " + successCount + "/" + pendingOrders.size() + " đơn hàng");
                response.put("errors", errors);
            } else {
                response.put("message", "Không thể xác nhận đơn hàng nào. Vui lòng thử lại!");
                response.put("errors", errors);
            }

            return response;

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi xử lý: " + e.getMessage()
            );
        }
    }


    private Map<String, Object> confirmOrder(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String shippingProvider = req.getParameter("shippingProvider");

            Map<String, Object> result = orderService.confirmOrder(orderId, shippingProvider);

            if ((Boolean) result.get("success")) {
                Map<String, Object> orderInfo = orderService.getOrderBasicInfo(orderId);

                if (orderInfo != null) {
                    Object userIdObj = orderInfo.get("user_id");
                    Object orderCodeObj = orderInfo.get("ordercode");

                    if (userIdObj != null && orderCodeObj != null) {
                        int userId = ((Number) userIdObj).intValue();
                        String orderCode = String.valueOf(orderCodeObj);

                        notificationService.notifyOrderConfirmed(userId, orderCode);
                    }
                }
            }

            Map<String, Object> mutableResult = new HashMap<>(result);
            if (!mutableResult.containsKey("message")) {
                mutableResult.put("message", Boolean.TRUE.equals(mutableResult.get("success"))
                        ? "Xác nhận đơn hàng thành công!"
                        : "Xác nhận đơn hàng thất bại");
            }
            return mutableResult;

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> cancelOrder(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String reason = req.getParameter("reason");

            Map<String, Object> result = orderService.cancelOrder(orderId, reason);

            if ((Boolean) result.get("success")) {
                Map<String, Object> orderInfo = orderService.getOrderBasicInfo(orderId);

                if (orderInfo != null) {
                    Object userIdObj = orderInfo.get("user_id");
                    Object orderCodeObj = orderInfo.get("ordercode");

                    if (userIdObj != null && orderCodeObj != null) {
                        int userId = ((Number) userIdObj).intValue();
                        String orderCode = String.valueOf(orderCodeObj);

                        notificationService.notifyOrderCancelled(userId, orderCode, reason);
                    }
                }
            }

            return result;

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }


    private Map<String, Object> confirmShipped(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            Map<String, Object> result = orderService.confirmShipped(orderId);

            if ((Boolean) result.get("success")) {
                Map<String, Object> orderInfo = orderService.getOrderBasicInfo(orderId);

                if (orderInfo != null) {
                    Object userIdObj = orderInfo.get("user_id");
                    Object orderCodeObj = orderInfo.get("ordercode");

                    if (userIdObj != null && orderCodeObj != null) {
                        int userId = ((Number) userIdObj).intValue();
                        String orderCode = String.valueOf(orderCodeObj);

                        String shippingProvider = (String) orderInfo.get("shipping_provider");

                        notificationService.notifyOrderShipped(userId, orderCode, shippingProvider);
                    }
                }
            }

            return result;

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }


    private Map<String, Object> confirmDelivered(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            return orderService.confirmDelivered(orderId);

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        }
    }


    private Map<String, Object> updateOrderStatus(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String newStatus = req.getParameter("status");

            return orderService.updateOrderStatus(orderId, newStatus);

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        }
    }


    private Map<String, Object> processRefund(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            return orderService.processRefund(orderId);

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        }
    }


    private void sendJsonError(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(Map.of(
                "success", false,
                "message", message
        )));
    }


    private void searchOrdersByTab(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");

        try {
            String keyword = req.getParameter("keyword");
            String status = req.getParameter("status");

            if (status == null || status.isEmpty()) {
                resp.getWriter().write(gson.toJson(Map.of(
                        "success", false,
                        "message", "Missing status parameter"
                )));
                return;
            }

            if (keyword == null) {
                keyword = "";
            }

            List<Map<String, Object>> orders;

            switch (status) {
                case "Pending":
                    orders = orderService.searchPendingOrders(keyword);
                    break;
                case "AwaitingPickup":
                    orders = orderService.searchAwaitingPickupOrders(keyword);
                    break;
                case "Shipping":
                    orders = orderService.searchShippingOrders(keyword);
                    break;
                case "Completed":
                    orders = orderService.searchCompletedOrders("");
                    break;
                case "Returned":
                    orders = orderService.searchReturnedOrders(keyword);
                    break;
                case "Cancelled":
                    orders = orderService.searchCancelledOrders(keyword);
                    break;
                default:
                    orders = new ArrayList<>();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("count", orders.size());

            String jsonResponse = gson.toJson(response);

            resp.getWriter().write(jsonResponse);
            resp.getWriter().flush();

        } catch (Exception e) {
            e.printStackTrace();

            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errorResponse.put("errorType", e.getClass().getSimpleName());

                resp.getWriter().write(gson.toJson(errorResponse));
                resp.getWriter().flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private Map<String, Object> updateOrderStatusWithSync(HttpServletRequest req) {
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String newStatus = req.getParameter("status");

            boolean success = orderService.updateOrderStatusAndSync(orderId, newStatus);

            if (success) {
                return Map.of(
                        "success", true,
                        "message", "Đã cập nhật trạng thái đơn hàng"
                );
            } else {
                return Map.of(
                        "success", false,
                        "message", "Cập nhật thất bại"
                );
            }

        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "Invalid order ID");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private void sendJson(HttpServletResponse resp,
                          Map<String, Object> result) throws IOException {

        Map<String, Object> mutableResult = new HashMap<>(result != null ? result : new HashMap<>());

        if (mutableResult.isEmpty()) {
            mutableResult.put("success", false);
            mutableResult.put("message", "Empty response from server");
        }

        if (!mutableResult.containsKey("message")) {
            mutableResult.put("message", mutableResult.containsKey("success") &&
                    Boolean.TRUE.equals(mutableResult.get("success"))
                    ? "Thành công" : "Có lỗi xảy ra");
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(mutableResult));
        resp.getWriter().flush();
    }
}