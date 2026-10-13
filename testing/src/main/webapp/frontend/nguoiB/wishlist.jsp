<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.dao.WishlistDAO" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Comic" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.User" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sản phẩm yêu thích</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>
<main>
    <%
        WishlistDAO wishlistDAO = new WishlistDAO();
        User currentUser = (User) session.getAttribute("currentUser");

        List<Comic> wishlistComics = null;
        int wishlistCount = 0;

        if (currentUser != null) {
            wishlistComics = wishlistDAO.getWishlistComics(currentUser.getId());
            wishlistCount = wishlistComics != null ? wishlistComics.size() : 0;
        }
    %>

    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>
    <div class="wishlist-container">
        <c:if test="${not empty sessionScope.successMsg}">
            <div class="alert alert-success"
                 style="position: fixed; top: 80px; right: 20px; z-index: 9999; background: #4CAF50; color: white; padding: 15px 20px; border-radius: 5px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); animation: slideIn 0.3s ease-out;">
                <i class="fas fa-check-circle"></i>
                    ${sessionScope.successMsg}
            </div>
            <c:remove var="successMsg" scope="session"/>
            <script>
                setTimeout(function () {
                    const alert = document.querySelector('.alert-success');
                    if (alert) {
                        alert.style.animation = 'slideOut 0.3s ease-out';
                        setTimeout(function () {
                            alert.remove();
                        }, 300);
                    }
                }, 3000);
            </script>
        </c:if>
        <c:if test="${not empty sessionScope.errorMsg}">
            <div class="alert alert-error"
                 style="position: fixed; top: 80px; right: 20px; z-index: 9999; background: #f44336; color: white; padding: 15px 20px; border-radius: 5px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); animation: slideIn 0.3s ease-out;">
                <i class="fas fa-exclamation-circle"></i>
                    ${sessionScope.errorMsg}
            </div>
            <c:remove var="errorMsg" scope="session"/>
            <script>
                setTimeout(function () {
                    const alert = document.querySelector('.alert-error');
                    if (alert) {
                        alert.style.animation = 'slideOut 0.3s ease-out';
                        setTimeout(function () {
                            alert.remove();
                        }, 300);
                    }
                }, 3000);
            </script>
        </c:if>


        <h2>Sản phẩm yêu thích (<%= currentUser != null ? wishlistCount : 0 %> sản phẩm)</h2>

        <% if (currentUser == null) { %>
        <div style="text-align:center; padding:80px 20px; color:#666;">
            <i class="fa-regular fa-heart" style="font-size:80px; color:#ddd; margin-bottom:20px; display:block;"></i>
            <p style="font-size:18px; margin:10px 0; font-weight:500;">Vui lòng đăng nhập để xem danh sách yêu thích</p>
            <p style="font-size:14px; color:#999; margin:10px 0;">Lưu trữ những truyện yêu thích để không bao giờ bỏ
                lỡ!</p>
            <a href="${pageContext.request.contextPath}/login"
               style="display:inline-block; padding:12px 30px; background:#007bff; color:white;
                      text-decoration:none; border-radius:6px; margin-top:20px; font-weight:500;
                      transition: all 0.3s ease;">
                Đăng nhập ngay
            </a>
        </div>

        <% } else if (wishlistComics == null || wishlistComics.isEmpty()) { %>
        <div style="text-align:center; padding:80px 20px; color:#666;">
            <i class="fa-regular fa-heart" style="font-size:80px; color:#ddd; margin-bottom:20px; display:block;"></i>
            <p style="font-size:18px; margin:10px 0; font-weight:500;">Danh sách yêu thích còn trống</p>
            <p style="font-size:14px; color:#999; margin:10px 0;">Hãy thêm những truyện bạn yêu thích vào đây!</p>
            <a href="${pageContext.request.contextPath}/home"
               style="display:inline-block; padding:12px 30px; background:#007bff; color:white;
                      text-decoration:none; border-radius:6px; margin-top:20px; font-weight:500;
                      transition: all 0.3s ease;">
                Khám phá ngay!
            </a>
        </div>

        <% } else { %>
        <div class="wishlist-grid">
            <% for (Comic comic : wishlistComics) { %>
            <div class="wishlist-item" data-comic-id="<%= comic.getId() %>">
                <a href="${pageContext.request.contextPath}/comic-detail?id=<%= comic.getId() %>"
                   style="text-decoration: none;">
                    <img src="<%= comic.getThumbnailUrl() != null ? comic.getThumbnailUrl() : "" %>"
                         alt="<%= comic.getNameComics() %>"
                         class="wishlist-img"
                         onerror="this.src='${pageContext.request.contextPath}/images/default-comic.jpg'">
                </a>
                <div class="wishlist-info">
                    <h3>
                        <a href="${pageContext.request.contextPath}/comic-detail?id=<%= comic.getId() %>">
                            <%= comic.getNameComics() %>
                        </a>
                    </h3>
                    <div class="wishlist-price">
                        <span class="current-price">
                            <fmt:formatNumber value="<%= comic.getDiscountPrice() %>"
                                              type="number"
                                              groupingUsed="true"/> đ
                        </span>
                        <% if (comic.hasDiscount()) { %>
                        <span class="original-price">
                            <fmt:formatNumber value="<%= comic.getPrice() %>"
                                              type="number"
                                              groupingUsed="true"/> đ
                        </span>
                        <span class="discount-badge">
                            -<%= Math.round(comic.getDiscountPercent()) %>%
                        </span>
                        <% } %>
                    </div>

                    <% if (comic.getStockQuantity() > 0) { %>
                    <p class="stock-status available">
                        <i class="fas fa-check-circle"></i> Còn hàng
                    </p>
                    <% } else { %>
                    <p class="stock-status out-of-stock">
                        <i class="fas fa-times-circle"></i> Hết hàng
                    </p>
                    <% } %>
                    <div class="wishlist-actions">
                        <% if (comic.getStockQuantity() > 0) { %>
                        <a href="${pageContext.request.contextPath}/cart?action=add&comicId=<%= comic.getId() %>&quantity=1&returnUrl=wishlist"
                           class="add-to-cart-btn">
                            <i class="fas fa-shopping-cart"></i> Thêm vào giỏ
                        </a>
                        <% } else { %>
                        <button class="add-to-cart-btn" disabled>
                            <i class="fas fa-shopping-cart"></i> Hết hàng
                        </button>
                        <% } %>

                        <button class="remove-wishlist-btn"
                                data-comic-id="<%= comic.getId() %>">
                            <i class="fas fa-trash-alt"></i> Xóa
                        </button>
                    </div>

                </div>
            </div>
            <% } %>
        </div>
        <% } %>
    </div>

    <div class="cart-success-popup" style="display: none;">
        <div class="cart-popup-overlay"></div>
        <div class="cart-popup-content">
            <div class="cart-popup-icon">
                <i class="fas fa-check-circle"></i>
            </div>
            <h3>Đã thêm vào giỏ hàng!</h3>
            <p class="product-name"></p>
            <div class="cart-info">
                <span>Giỏ hàng hiện có: <strong class="cart-count-display">0 sản phẩm</strong></span>
            </div>
            <div class="cart-popup-actions">
                <button class="btn-continue" onclick="closeCartPopup()">
                    <i class="fas fa-arrow-left"></i> Tiếp tục mua
                </button>
                <a href="${pageContext.request.contextPath}/cart" class="btn-view-cart">
                    <i class="fas fa-shopping-cart"></i> Xem giỏ hàng
                </a>
            </div>
        </div>
    </div>
</main>

<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    document.addEventListener('click', function (e) {
        if (e.target.classList.contains('remove-wishlist-btn') ||
            e.target.closest('.remove-wishlist-btn')) {

            const btn = e.target.classList.contains('remove-wishlist-btn') ?
                e.target : e.target.closest('.remove-wishlist-btn');
            const comicId = btn.dataset.comicId;
            const item = btn.closest('.wishlist-item');

            showConfirmPopup().then(confirmed => {
                if (!confirmed) return;

                btn.disabled = true;
                const originalHTML = btn.innerHTML;
                btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xóa...';

                fetch('${pageContext.request.contextPath}/WishlistServlet', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    credentials: 'include',
                    body: 'action=remove&comic_id=' + comicId
                })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            item.style.transition = 'all 0.3s ease';
                            item.style.opacity = '0';
                            item.style.transform = 'scale(0.8)';

                            setTimeout(() => {
                                item.remove();
                                const titleElement = document.querySelector('.wishlist-container h2');
                                if (titleElement) {
                                    titleElement.textContent = 'Sản phẩm yêu thích (' + data.count + ' sản phẩm)';
                                }
                                const grid = document.querySelector('.wishlist-grid');
                                if (grid && grid.children.length === 0) {
                                    location.reload();
                                }
                                showWishlistToast(data.message, 'success');
                            }, 300);
                        } else {
                            showWishlistToast(data.message, 'error');
                            btn.disabled = false;
                            btn.innerHTML = originalHTML;
                        }
                    })
                    .catch(err => {
                        console.error('Lỗi:', err);
                        showWishlistToast('Lỗi kết nối, vui lòng thử lại', 'error');
                        btn.disabled = false;
                        btn.innerHTML = originalHTML;
                    });
            });
        }
    });

    function showWishlistToast(message, type = 'success') {
        const oldToast = document.querySelector('.wishlist-toast');
        if (oldToast) oldToast.remove();

        const icons = {
            success: '<i class="fas fa-check-circle"></i>',
            error: '<i class="fas fa-exclamation-circle"></i>',
            info: '<i class="fas fa-info-circle"></i>'
        };

        const toast = document.createElement('div');
        toast.className = 'wishlist-toast ' + type;
        toast.innerHTML = icons[type] + ' ' + message;
        document.body.appendChild(toast);

        setTimeout(() => toast.classList.add('show'), 10);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }


    function showConfirmPopup(message = 'Bạn có chắc muốn xóa sản phẩm này khỏi danh sách yêu thích?') {
        if (!document.getElementById('wl-popup-style')) {
            const s = document.createElement('style');
            s.id = 'wl-popup-style';
            s.textContent = `
    .wl-overlay{position:fixed;inset:0;background:transparent;z-index:9999;display:flex;align-items:center;justify-content:center;opacity:0;transition:opacity .2s ease}
    .wl-overlay.show{opacity:1}
    .wl-popup{background:#fff;border-radius:12px;padding:28px 24px 20px;width:100%;max-width:340px;box-shadow:0 20px 60px rgba(0,0,0,.18);text-align:center;transform:translateY(16px) scale(.97);transition:transform .25s ease,opacity .25s ease;opacity:0}
    .wl-overlay.show .wl-popup{transform:translateY(0) scale(1);opacity:1}
    .wl-popup p{margin:0 0 20px;font-size:13px;color:#666;line-height:1.5}
    .wl-popup-actions{display:flex;gap:10px}
    .wl-btn-cancel,.wl-btn-confirm{flex:1;padding:9px 0;border-radius:7px;border:none;font-size:14px;font-weight:500;cursor:pointer;transition:all .2s ease}
    .wl-btn-cancel{background:#f3f3f3;color:#555}
    .wl-btn-cancel:hover{background:#e8e8e8}
    .wl-btn-confirm{background:linear-gradient(90deg,#dc3545,#c82333);color:#fff;box-shadow:0 3px 10px rgba(220,53,69,.3)}
    .wl-btn-confirm:hover{background:linear-gradient(90deg,#c82333,#a71d2a);transform:translateY(-1px)}
`;
            document.head.appendChild(s);
        }

        return new Promise(resolve => {
            const overlay = document.createElement('div');
            overlay.className = 'wl-overlay';
            overlay.innerHTML = `
    <div class="wl-popup">
        <p>Bạn có chắc chắn xóa sản phẩm này khỏi wishlist?</p>
        <div class="wl-popup-actions">
            <button class="wl-btn-cancel">Hủy</button>
            <button class="wl-btn-confirm">Xác nhận</button>
        </div>
    </div>`;
            document.body.appendChild(overlay);
            requestAnimationFrame(() => overlay.classList.add('show'));

            function close(result) {
                overlay.classList.remove('show');
                setTimeout(() => overlay.remove(), 250);
                resolve(result);
            }

            overlay.querySelector('.wl-btn-confirm').addEventListener('click', () => close(true));
            overlay.querySelector('.wl-btn-cancel').addEventListener('click', () => close(false));
            overlay.addEventListener('click', e => {
                if (e.target === overlay) close(false);
            });
            document.addEventListener('keydown', function esc(e) {
                if (e.key === 'Escape') {
                    document.removeEventListener('keydown', esc);
                    close(false);
                }
            });
        });
    }
</script>

</body>
</html>