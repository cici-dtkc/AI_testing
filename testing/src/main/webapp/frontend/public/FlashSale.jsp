<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Flash Sale - Giảm Giá Khủng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FlashSale.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/img/favicon.ico">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>

<div class="container">
    <div class="banner-header">
        <img src="${pageContext.request.contextPath}/img/BannerFlashSale.png" alt="Flash Sale Banner">
    </div>
</div>

<div class="flash-banner">
    <div class="flash-title">
        <span class="badge">FLASH SALE</span>
    </div>

    <div class="contain-flash">
        <div class="countdown-wrap">
            <c:choose>
                <c:when test="${activeFlashSale != null && flashSaleEndTimeMillis != null}">
                    <span class="countdown-label">Kết thúc trong</span>
                    <div class="countdown" id="countdown">
                        <div class="time-box" id="hours">00</div>
                        <span class="sep">:</span>
                        <div class="time-box" id="minutes">00</div>
                        <span class="sep">:</span>
                        <div class="time-box" id="seconds">00</div>
                    </div>
                    <input type="hidden" id="flashSaleEndTimeMillis" value="${flashSaleEndTimeMillis}">
                </c:when>
                <c:otherwise>
                    <span class="countdown-label">Hiện không có Flash Sale nào đang diễn ra</span>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="slot-row" id="slotRow">
            <c:forEach items="${upcomingFlashSales}" var="fs" varStatus="status">
                <div class="slot ${fs.active ? 'active' : ''}"
                     data-flashsale-id="${fs.id}"
                     data-status="${fs.status}">
                    <div class="slot-time">
                            ${fs.startTimeFormatted}
                    </div>
                    <div class="slot-status">
                            ${fs.statusLabel}
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>
</div>

<c:if test="${activeFlashSale != null}">
    <div class="flash-sale-header">
        <h2 class="flash-sale-name">${activeFlashSale.name}</h2>
        <p class="flash-sale-description">Giảm giá đến <strong><fmt:formatNumber
                value="${activeFlashSale.discountPercent}" pattern="#"/>%</strong></p>
    </div>
</c:if>
<div class="container-item" id="flashSaleProducts">

    <c:choose>
        <c:when test="${not empty activeComics}">
            <c:forEach items="${activeComics}" var="comic">
                <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                    <div class="product-card">
                        <img src="${comic.image_url}"
                             alt="${comic.name}"
                             class="product-image"
                             onerror="this.src='${pageContext.request.contextPath}/img/no-image.png'"/>

                        <h3 class="product-title">${comic.name}</h3>

                        <div class="price-section">
    <span class="price">
        <fmt:formatNumber value="${comic.flash_price}" pattern="#,###"/> đ
    </span>
                            <span class="discount">
        -<fmt:formatNumber value="${comic.discount_percent}" pattern="#" maxFractionDigits="0"/>%
    </span>
                        </div>

                        <div class="price-section">
                            <span class="old-price">
                                <fmt:formatNumber value="${comic.original_price}" pattern="#,###"/> đ
                            </span>
                        </div>

                        <div class="progress-bar">
                            <c:set var="soldPercent" value="${(comic.sold_count / 100) * 100}"/>
                            <c:if test="${soldPercent > 100}">
                                <c:set var="soldPercent" value="100"/>
                            </c:if>
                            <div class="progress-fill" style="width: ${soldPercent}%;"></div>
                        </div>
<%--                        <div class="sold-text">Đã bán ${comic.sold_count}</div>--%>
                        <div class="sold-text">Đã bán <strong>${comic.sold_count}</strong></div>

                        <button class="add-to-cart" onclick="addToCart(${comic.id}, event)">
                            <i class="fas fa-cart-plus"></i> Thêm giỏ hàng
                        </button>
                    </div>
                </a>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="no-products">
                <i class="fas fa-box-open"></i>
                <p>Không có sản phẩm Flash Sale nào đang diễn ra</p>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/frontend/public/Footer.jsp"/>

<script src="${pageContext.request.contextPath}/js/flash-sale.js"></script>


</body>
</html>