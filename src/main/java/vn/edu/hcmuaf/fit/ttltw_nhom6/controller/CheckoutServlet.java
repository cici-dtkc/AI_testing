package vn.edu.hcmuaf.fit.ttltw_nhom6.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jdbi.v3.core.Jdbi;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CartDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.ComicDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.FlashSaleComicsDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.UserShippingAddressDAO;
import vn.edu.hcmuaf.fit.ttltw_nhom6.db.JdbiConnector;
import vn.edu.hcmuaf.fit.ttltw_nhom6.model.*;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.ComicService;
import vn.edu.hcmuaf.fit.ttltw_nhom6.service.cache.CacheService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import vn.edu.hcmuaf.fit.ttltw_nhom6.dao.VoucherDao;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {
    private UserShippingAddressDAO shippingAddressDAO;
    private CartDAO cartDAO;
    private VoucherDao voucherDao;

    @Override
    public void init() throws ServletException {
        Jdbi jdbi = JdbiConnector.get();
        shippingAddressDAO = new UserShippingAddressDAO(jdbi);
        cartDAO = new CartDAO();
        voucherDao = new VoucherDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String comicIdParam = request.getParameter("comicId");
        if (comicIdParam != null) {
            try {
                int comicId = Integer.parseInt(comicIdParam);
                int quantity = request.getParameter("quantity") != null
                        ? Integer.parseInt(request.getParameter("quantity")) : 1;

                ComicService comicService = new ComicService();
                Comic comic = comicService.getComicById(comicId);

                if (comic == null || comic.getStockQuantity() < quantity) {
                    session.setAttribute("errorMsg", "Sản phẩm không đủ hàng");
                    response.sendRedirect(request.getContextPath() + "/comic-detail?id=" + comicId);
                    return;
                }

                FlashSaleComicsDAO flashSaleDAO = new FlashSaleComicsDAO();
                Map<String, Object> flashInfo = flashSaleDAO.getFlashSaleInfoByComicId(comicId);
                Integer flashSaleId = null;
                Double flashSalePrice = null;
                if (flashInfo != null) {
                    flashSaleId = (Integer) flashInfo.get("flashsale_id");
                    Object discountObj = flashInfo.get("discount_percent");
                    if (discountObj instanceof Number) {
                        double pct = ((Number) discountObj).doubleValue();
                        flashSalePrice = comic.getPrice() * (1 - pct / 100.0);
                    }
                }

                int cartId = cartDAO.getOrCreateCartByUserId(user.getId());
                cartDAO.addItem(cartId, comicId, quantity);

                CartItem tempItem = new CartItem(comic, quantity, flashSaleId, flashSalePrice);
                double subtotal = tempItem.getFinalPrice() * quantity;


                double shippingFee = 25000;
                String shippingFeeParam = request.getParameter("shippingFee");
                if (shippingFeeParam != null && !shippingFeeParam.isEmpty()) {
                    try {
                        shippingFee = Double.parseDouble(shippingFeeParam);
                    } catch (NumberFormatException ignored) {}
                }

                List<CartItem> selectedItems = new ArrayList<>();
                selectedItems.add(tempItem);

                session.setAttribute("selectedItems", selectedItems);
                session.setAttribute("checkoutSubtotal", subtotal);
                session.setAttribute("shippingFee", shippingFee);
                session.setAttribute("checkoutTotal", subtotal + shippingFee);

            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/home");
                return;
            }
        }

        Boolean justOrdered = (Boolean) session.getAttribute("justOrdered");
        if (justOrdered != null && justOrdered) {
            session.removeAttribute("justOrdered");

            String orderSuccess = (String) session.getAttribute("orderSuccess");
            if (orderSuccess != null) {
                request.setAttribute("orderSuccess", orderSuccess);
                session.removeAttribute("orderSuccess");
            }

            request.setAttribute("user", user);
            request.getRequestDispatcher("/frontend/nguoiB/checkout.jsp").forward(request, response);
            return;
        }

        @SuppressWarnings("unchecked")
        List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("selectedItems");

        if (selectedItems == null || selectedItems.isEmpty()) {
            session.setAttribute("cartError", "Vui lòng chọn sản phẩm để thanh toán");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        FlashSaleComicsDAO flashSaleDAOCheck = new FlashSaleComicsDAO();
        boolean priceUpdated = false;

        for (CartItem item : selectedItems) {
            if (item.getFlashSaleId() != null) {
                Map<String, Object> flashInfo = flashSaleDAOCheck
                        .getFlashSaleInfoByComicId(item.getComic().getId());

                if (flashInfo == null) {
                    item.removeFlashSale();
                    priceUpdated = true;
                }
            }
        }

        if (priceUpdated) {
            double newSubtotal = selectedItems.stream()
                    .mapToDouble(i -> i.getFinalPrice() * i.getQuantity())
                    .sum();
            double shippingFee = session.getAttribute("shippingFee") != null
                    ? ((Number) session.getAttribute("shippingFee")).doubleValue()
                    : 25000;

            session.setAttribute("selectedItems",    selectedItems);
            session.setAttribute("checkoutSubtotal", newSubtotal);
            session.setAttribute("checkoutTotal",    newSubtotal + shippingFee);
        }

        Optional<UserShippingAddress> defaultAddress = shippingAddressDAO.getDefaultAddress(user.getId());

        if (defaultAddress.isPresent()) {
            UserShippingAddress address = defaultAddress.get();

            if (address.getDistrict() == null || address.getDistrict().trim().isEmpty()) {
                List<UserShippingAddress> allAddresses = shippingAddressDAO.getAddressesByUserId(user.getId());
                address = allAddresses.stream()
                        .filter(a -> a.getDistrict() != null && !a.getDistrict().trim().isEmpty())
                        .findFirst()
                        .orElse(address);
            }
            request.setAttribute("defaultAddress", address);
            request.setAttribute("defaultRecipientName", address.getRecipientName());
            request.setAttribute("defaultPhone", address.getPhone());
            request.setAttribute("defaultProvince", address.getProvince());
            request.setAttribute("defaultDistrict", address.getDistrict());
            request.setAttribute("defaultWard", address.getWard());
            request.setAttribute("defaultStreetAddress", address.getStreetAddress());
        }

        String orderError = (String) session.getAttribute("orderError");
        System.out.println(orderError);
        if (orderError != null) {
            request.setAttribute("orderError", orderError);
            session.removeAttribute("orderError");
        }
        request.setAttribute("user", user);

        List<Voucher> vouchers = voucherDao.getVouchersByUserId(user.getId());

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOf, ctx) ->
                                new com.google.gson.JsonPrimitive(
                                        src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .create();

        request.setAttribute("vouchersJson", gson.toJson(vouchers));

        request.getRequestDispatcher("/frontend/nguoiB/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        if ("place".equals(action)) {
            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("selectedItems");

            if (selectedItems == null || selectedItems.isEmpty()) {
                int cartIdTemp = cartDAO.getOrCreateCartByUserId(user.getId());
                selectedItems = cartDAO.getCartItems(cartIdTemp);
                session.setAttribute("selectedItems", selectedItems);
            }

            if (selectedItems == null || selectedItems.isEmpty()) {
                session.setAttribute("orderError", "Không có sản phẩm nào để đặt hàng");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }

            String recipientName  = request.getParameter("recipientName");
            String shippingPhone  = request.getParameter("shippingPhone");
            String shippingAddress = request.getParameter("shippingAddress");
            String shippingProvider = request.getParameter("shippingProvider");
            String addressIdParam = request.getParameter("shippingAddressId");
            String pointsParam    = request.getParameter("pointsUsed");
            String feeParam       = request.getParameter("shippingFee");

            if (recipientName == null || recipientName.trim().isEmpty()
                    || shippingPhone == null || shippingPhone.trim().isEmpty()
                    || shippingAddress == null || shippingAddress.trim().isEmpty()) {
                session.setAttribute("orderError", "Vui lòng điền đầy đủ thông tin giao hàng");
                response.sendRedirect(request.getContextPath() + "/checkout");
                return;
            }

            int shippingAddressId = (addressIdParam != null && !addressIdParam.isEmpty())
                    ? Integer.parseInt(addressIdParam) : 0;
            int pointsUsed = (pointsParam != null && !pointsParam.isEmpty())
                    ? Integer.parseInt(pointsParam) : 0;
            double shippingFee = (feeParam != null && !feeParam.isEmpty())
                    ? Double.parseDouble(feeParam) : 25000;

            int cartId = cartDAO.getOrCreateCartByUserId(user.getId());
            try {
                CacheService.cartCache.remove(cartId);

                System.out.println("[DEBUG] Bắt đầu checkout cartId=" + cartId + " userId=" + user.getId());

                int orderId = cartDAO.checkout(
                        cartId,
                        user.getId(),
                        shippingAddressId,
                        recipientName.trim(),
                        shippingPhone.trim(),
                        shippingAddress.trim(),
                        shippingProvider != null ? shippingProvider : "standard",
                        shippingFee,
                        pointsUsed
                );

                session.removeAttribute("selectedItems");
                session.removeAttribute("cartItems");
                session.removeAttribute("checkoutSubtotal");
                session.removeAttribute("checkoutTotal");
                session.removeAttribute("shippingFee");

                session.setAttribute("justOrdered",  true);
                session.setAttribute("orderSuccess",
                        "Đặt hàng thành công! Mã đơn hàng: #" + orderId);

                Cart newCart = new Cart();
                session.setAttribute("cart", newCart);

                response.sendRedirect(request.getContextPath() + "/checkout");

            } catch (Exception e) {
                session.setAttribute("orderError", e.getMessage());
                response.sendRedirect(request.getContextPath() + "/checkout");
            }
            return;
        }


        String[] selectedComicIds = request.getParameterValues("selectedComics");

        if (selectedComicIds == null || selectedComicIds.length == 0) {
            session.setAttribute("checkoutError", "Vui lòng chọn ít nhất một sản phẩm để thanh toán");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        int cartId = cartDAO.getOrCreateCartByUserId(user.getId());
        List<CartItem> cartItems = cartDAO.getCartItems(cartId);

        if (cartItems == null || cartItems.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        List<CartItem> selectedItems = new ArrayList<>();
        double subtotal = 0;

        for (String comicId : selectedComicIds) {
            int id = Integer.parseInt(comicId);
            for (CartItem item : cartItems) {
                if (item.getComic().getId() == id) {
                    selectedItems.add(item);
                    subtotal += item.getFinalPrice() * item.getQuantity();
                    break;
                }
            }
        }

        if (selectedItems.isEmpty()) {
            session.setAttribute("checkoutError", "Không tìm thấy sản phẩm đã chọn");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        double shippingFee = 25000;
        double totalAmount = subtotal + shippingFee;

        session.setAttribute("selectedItems", selectedItems);
        session.setAttribute("checkoutSubtotal", subtotal);
        session.setAttribute("shippingFee", shippingFee);
        session.setAttribute("checkoutTotal", totalAmount);

        response.sendRedirect(request.getContextPath() + "/checkout");
    }
}