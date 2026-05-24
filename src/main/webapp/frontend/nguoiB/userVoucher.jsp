<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.Duration" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ví voucher</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/userVoucher.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>
<main>
    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>
    <%
        List<Voucher> productVouchers = (List<Voucher>) request.getAttribute("userProductVouchers");
        List<Voucher> shippingVouchers = (List<Voucher>) request.getAttribute("userShippingVouchers");
    %>
    <div class="user-voucher-wrapper">
    <h2>Voucher của tôi</h2>

    <%
        boolean hasAny = (productVouchers != null && !productVouchers.isEmpty())
                || (shippingVouchers != null && !shippingVouchers.isEmpty());
        if (!hasAny) {
    %>
    <div class="uv-empty">
        <i class="fa-regular fa-ticket"></i>
        <p>Bạn chưa lưu voucher nào</p>
        <a href="${pageContext.request.contextPath}/voucher">Khám phá voucher ngay</a>
    </div>
    <%
    } else {
    %>

    <%-- Voucher sản phẩm --%>
    <% if (productVouchers != null && !productVouchers.isEmpty()) { %>
    <div class="voucher-section-title">Voucher sản phẩm</div>
    <% for (Voucher v : productVouchers) {
        boolean isExpired = v.getEndDate() != null && v.getEndDate().isBefore(LocalDateTime.now());
        boolean isNotStarted = isNotStarted(v.getStartDate());
        String expiryLabel = getExpiryLabel(v.getEndDate());
        boolean isUrgent = expiryLabel != null && expiryLabel.startsWith("Hết hạn trong");
    %>
    <div class="uv-card uv-card-product <%= isUrgent ? "uv-card-urgent" : "" %>">
        <div class="uv-card-left">
            <i class="fa-solid fa-tag"></i>
            <span class="uv-type-label">Sản phẩm</span>
        </div>
        <div class="uv-card-body">
            <div class="uv-discount-value">
                <% if ("percent".equalsIgnoreCase(v.getDiscountType())) { %>
                Giảm <%= v.getDiscountValue().stripTrailingZeros().toPlainString() %>%
                <% } else { %>
                Giảm <fmt:formatNumber value="<%= v.getDiscountValue() %>" type="number" groupingUsed="true"/>đ
                <% } %>
            </div>
            <div class="uv-code">Mã: <span><%= v.getCode() %></span></div>

            <% if (v.getMinOrderAmount() != null && v.getMinOrderAmount().compareTo(java.math.BigDecimal.ZERO) > 0) { %>
            <div class="uv-min-order">
                Đơn tối thiểu: <fmt:formatNumber value="<%= v.getMinOrderAmount() %>" type="number" groupingUsed="true"/>đ
            </div>
            <% } %>

            <% if (isExpired) { %>
            <div class="uv-expiry expired">Đã hết hạn</div>
            <% } else if (isUrgent) { %>
            <div class="uv-expiry urgent"><i class="fa-solid fa-clock"></i> <%= expiryLabel %></div>
            <% } else { %>
            <div class="uv-expiry">
                HSD: <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getEndDate()) %>" pattern="dd/MM/yyyy"/>
            </div>
            <% } %>
        </div>
        <div class="uv-card-action">
            <% if (isExpired) { %>
            <span class="uv-badge-expired">Hết hạn</span>

            <% } else if (isNotStarted) { %>
            <span class="uv-later-text">
                <i class="fa-regular fa-clock"></i> Dùng sau
            </span>
            <span class="uv-start-date">
                Từ: <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getStartDate()) %>" pattern="dd/MM/yyyy"/>
            </span>

            <% } else { %>
            <button class="uv-btn-urgent"
                    data-code="<%= v.getCode() %>"
                    onclick="useVoucher(this)">
                Dùng ngay
            </button>
            <% } %>
        </div>
    </div>
    <% } %>
    <% } %>

    <%-- Voucher vận chuyển --%>
    <% if (shippingVouchers != null && !shippingVouchers.isEmpty()) { %>
    <div class="voucher-section-title">Voucher vận chuyển</div>
    <% for (Voucher v : shippingVouchers) {
        boolean isExpired = v.getEndDate() != null && v.getEndDate().isBefore(LocalDateTime.now());
        boolean isNotStarted = isNotStarted(v.getStartDate());
        String expiryLabel = getExpiryLabel(v.getEndDate());
        boolean isUrgent = expiryLabel != null && expiryLabel.startsWith("Hết hạn trong");
    %>
    <div class="uv-card uv-card-shipping <%= isUrgent ? "uv-card-urgent" : "" %>">
        <div class="uv-card-left">
            <i class="fa-solid fa-truck"></i>
            <span class="uv-type-label">Vận chuyển</span>
        </div>
        <div class="uv-card-body">
            <div class="uv-discount-value">
                <% if ("percent".equalsIgnoreCase(v.getDiscountType())) { %>
                Giảm <%= v.getDiscountValue().stripTrailingZeros().toPlainString() %>%
                <% } else { %>
                Giảm <fmt:formatNumber value="<%= v.getDiscountValue() %>" type="number" groupingUsed="true"/>đ
                <% } %>
            </div>
            <div class="uv-code">Mã: <span><%= v.getCode() %></span></div>

            <% if (v.getMinOrderAmount() != null && v.getMinOrderAmount().compareTo(java.math.BigDecimal.ZERO) > 0) { %>
            <div class="uv-min-order">
                Đơn tối thiểu: <fmt:formatNumber value="<%= v.getMinOrderAmount() %>" type="number" groupingUsed="true"/>đ
            </div>
            <% } %>

            <% if (isExpired) { %>
            <div class="uv-expiry expired">Đã hết hạn</div>
            <% } else if (isUrgent) { %>
            <div class="uv-expiry urgent"><i class="fa-solid fa-clock"></i> <%= expiryLabel %></div>
            <% } else { %>
            <div class="uv-expiry">
                HSD: <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getEndDate()) %>" pattern="dd/MM/yyyy"/>
            </div>
            <% } %>
        </div>
        <div class="uv-card-action">
            <% if (isExpired) { %>
            <span class="uv-badge-expired">Hết hạn</span>
            <% } else if (isNotStarted) { %>
            <span class="uv-later-text">
                <i class="fa-regular fa-clock"></i> Dùng sau
            </span>
            <span class="uv-start-date">
                Từ: <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getStartDate()) %>" pattern="dd/MM/yyyy"/>
            </span>

            <% } else { %>
            <button class="uv-btn-urgent"
                    data-code="<%= v.getCode() %>"
                    onclick="useVoucher(this)">
                Dùng ngay
            </button>
            <% } %>
        </div>
    </div>
    <% } %>
    <% } %>

    <% } %>
        <%!
            private boolean isNotStarted(java.time.LocalDateTime startDate) {
                if (startDate == null) return false;
                return startDate.isAfter(java.time.LocalDateTime.now());
            }

            private String getExpiryLabel(java.time.LocalDateTime endDate) {
                if (endDate == null) return null;
                LocalDateTime now = LocalDateTime.now();
                if (endDate.isBefore(now)) return "Đã hết hạn";

                Duration duration = Duration.between(now, endDate);
                long totalMinutes = duration.toMinutes();
                long totalHours   = duration.toHours();

                if (totalMinutes < 60) {
                    return "Hết hạn trong: " + totalMinutes + " phút";
                } else if (totalHours <= 1) {
                    return "Hết hạn trong: 1 giờ";
                } else if (totalHours < 24) {
                    return "Hết hạn trong: " + totalHours + " giờ";
                } else {
                    return null;
                }
            }
        %>
</div>
</main>
<jsp:include page="/frontend/public/Footer.jsp"/>
<script>
    function copyVoucherCode(btn) {
        const code = btn.getAttribute('data-code');
        navigator.clipboard.writeText(code).then(() => {
            btn.textContent = 'Đã sao chép!';
            btn.classList.add('copied');
            setTimeout(() => {
                btn.textContent = 'Sao chép';
                btn.classList.remove('copied');
            }, 2000);
        }).catch(() => {
            alert('Mã voucher: ' + code + '\nVui lòng copy thủ công.');
        });
    }

    function useVoucher(btn) {
        const code = btn.getAttribute('data-code');
        try {
            navigator.clipboard.writeText(code).catch(() => {});
        } catch (e) {}

        // showToast('Đã lưu mã ' + code + '! Đang chuyển đến giỏ hàng...', 'success');

        setTimeout(() => {
            window.location.href = '${pageContext.request.contextPath}/cart';
        }, 1500);
    }

    function showToast(message, type) {
        const colors = { success: '#28a745', warning: '#f59e0b', error: '#dc3545' };
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.style.cssText = `
        position:fixed; top:20px; left:50%; transform:translateX(-50%);
        background:${colors[type]}; color:white; padding:12px 24px;
        border-radius:8px; font-size:15px; z-index:99999;
        box-shadow:0 4px 12px rgba(0,0,0,0.2); white-space:nowrap;
    `;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }
</script>
</body>
</html>
