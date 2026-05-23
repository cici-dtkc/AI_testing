<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher" %>
<%@ page import="java.util.List" %>
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
        boolean isExpired = v.getEndDate() != null && v.getEndDate().isBefore(java.time.LocalDateTime.now());
    %>
    <div class="uv-card">
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
            <div class="uv-expiry <%= isExpired ? "expired" : "" %>">
                <%= isExpired ? "Đã hết hạn" : "HSD:" %>
                <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getEndDate()) %>" pattern="dd/MM/yyyy"/>
            </div>
        </div>
        <div class="uv-card-action">
            <% if (isExpired) { %>
            <span class="uv-badge-expired">Hết hạn</span>
            <% } else { %>
            <button class="uv-btn-copy" data-code="<%= v.getCode() %>"
                    onclick="copyVoucherCode(this)">
                Sao chép
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
        boolean isExpired = v.getEndDate() != null && v.getEndDate().isBefore(java.time.LocalDateTime.now());
    %>
    <div class="uv-card">
        <div class="uv-card-left" style="background: linear-gradient(135deg, #4a9fd4, #72b8e8);">
            <i class="fa-solid fa-truck"></i>
            <span class="uv-type-label">Vận chuyển</span>
        </div>
        <div class="uv-card-body">
            <div class="uv-discount-value" style="color: #4a9fd4;">
                <% if ("percent".equalsIgnoreCase(v.getDiscountType())) { %>
                Giảm <%= v.getDiscountValue().stripTrailingZeros().toPlainString() %>%
                <% } else { %>
                Giảm <fmt:formatNumber value="<%= v.getDiscountValue() %>" type="number" groupingUsed="true"/>đ
                <% } %>
            </div>
            <div class="uv-code">Mã: <span><%= v.getCode() %></span></div>
            <div class="uv-expiry <%= isExpired ? "expired" : "" %>">
                <%= isExpired ? "Đã hết hạn" : "HSD:" %>
                <fmt:formatDate value="<%= java.sql.Timestamp.valueOf(v.getEndDate()) %>" pattern="dd/MM/yyyy"/>
            </div>
        </div>
        <div class="uv-card-action">
            <% if (isExpired) { %>
            <span class="uv-badge-expired">Hết hạn</span>
            <% } else { %>
            <button class="uv-btn-copy" data-code="<%= v.getCode() %>"
                    onclick="copyVoucherCode(this)"
                    style="background: #4a9fd4;">
                Sao chép
            </button>
            <% } %>
        </div>
    </div>
    <% } %>
    <% } %>

    <% } %>
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
</script>
</body>
</html>
