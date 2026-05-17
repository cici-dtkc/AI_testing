<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Banner" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/homePage.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="${pageContext.request.contextPath}/js/homePage.js"></script>
</head>

<body>

<jsp:include page="/frontend/public/header.jsp"/>

<c:if test="${not empty sessionScope.successMessage}">
    <script type="text/javascript">
        window.onload = function () {
            alert("${sessionScope.successMessage}");
        };
    </script>

    <%-- Xóa ngay để không hiện lại khi refresh --%>
    <c:remove var="successMessage" scope="session"/>
</c:if>


<%--thông báo khi vô trang admin--%>
<%
    String msg = (String) session.getAttribute("errorMessage");
    if (msg != null) {
%>
<script type="text/javascript">
    window.onload = function () {
        alert("<%= msg %>");
    };
</script>
<%
        session.removeAttribute("errorMessage");
    }
%>


<div class="container-content">
    <%--banner --%>
    <%
        List<Banner> banners = (List<Banner>) request.getAttribute("banners");
    %>

    <div class="bannder-des">
        <div class="banner">
            <div class="list-images">
                <% if (banners != null && !banners.isEmpty()) {
                    for (Banner b : banners) { %>
                <img class="img" src="<%= b.getImageUrl() %>" alt="banner">
                <% }
                } else { %>
                <img class="img" src="${pageContext.request.contextPath}/img/banner1.jpg" alt="banner">
                <% } %>
            </div>

            <div class="btn prev">&#10094;</div>
            <div class="btn next">&#10095;</div>

            <div class="dots">
                <% if (banners != null) {
                    for (int i = 0; i < banners.size(); i++) { %>
                <div class="dot <%= (i==0 ? "active" : "") %>"></div>
                <% }
                } %>
            </div>
        </div>
    </div>

    <!-- chỗ này là danh mục -->
    <div class="danh-muc-icon">
        <h2>Tiện ích</h2>
        <div class="icon-list">
            <a href="${pageContext.request.contextPath}/flash-sale">
                <div class="icon-item">
                    <i class="fa-solid fa-bolt-lightning"></i>
                    <p>Flash Sale</p>
                </div>
            </a>


            <a href="${pageContext.request.contextPath}/flash-sale">
                <div class="icon-item">
                    <i class="fa-solid fa-money-bill"></i>
                    <p>Săn voucher</p>
                </div>
            </a>

            <%--            <a href="../nguoiB/profile.jsp">--%>
            <%--                <div class="icon-item">--%>
            <%--                    <i class="fa-solid fa-wallet"></i>--%>
            <%--                    <p>Ví Voucher</p>--%>
            <%--                </div>--%>
            <%--            </a>--%>
            <a href="${pageContext.request.contextPath}/wishlist">
                <div class="icon-item">
                    <i class="fa-solid fa-list"></i>
                    <p>WishList</p>
                </div>
            </a>
        </div>

        <div class="flash-sale">
            <div class="titile-flash-sale">
                <h2>FLASH SALE</h2>

                <c:choose>
                    <c:when test="${not empty flashSale && flashSale.status == 'active'}">
                        <p id="flash-sale-countdown"
                           data-end-time="${flashSaleEndTimeMillis}">
                            Đang tải thời gian...
                        </p>

                        <%-- DEBUG: Kiểm tra giá trị --%>
                        <script>
                            console.log("Flash Sale End Time Millis: ${flashSaleEndTimeMillis}");
                            console.log("Flash Sale Status: ${flashSale.status}");
                        </script>
                    </c:when>
                    <c:otherwise>
                        <p>Hiện không có Flash Sale</p>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="list-product-sale">
                <c:choose>
                    <c:when test="${not empty flashSaleComics}">
                        <c:forEach var="comic" items="${flashSaleComics}" varStatus="status">
                            <div>
                                <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                                    <img src="${comic.image_url}"
                                         alt="${comic.name}"
                                         style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px; margin-bottom: 10px;"
                                         onerror="this.src='${pageContext.request.contextPath}/img/no-image.png'">
                                    <h3>${comic.name}</h3>
                                    <p class="flash-price">
                                        Giá khuyến mãi:
                                        <fmt:formatNumber value="${comic.flash_price}" pattern="#,##0"/> đ
                                    </p>
                                    <p class="original-price">
                                        Giá gốc:
                                        <s><fmt:formatNumber value="${comic.original_price}" pattern="#,##0"/> đ</s>
                                    </p>
                                    <p class="discount-tag">
                                        Giảm giá:
                                        <fmt:formatNumber value="${comic.discount_percent}" pattern="#"
                                                          maxFractionDigits="0"/>%
                                    </p>
                                </a>
                            </div>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div style="grid-column: 1 / -1; text-align: center; padding: 40px;">
                            <p style="color: #999; font-size: 16px;">
                                <i class="fa-solid fa-box-open"
                                   style="font-size: 48px; margin-bottom: 10px; display: block;"></i>
                                Hiện không có sản phẩm Flash Sale
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="more-btn">
                <a href="${pageContext.request.contextPath}/flash-sale">
                    <button>Xem thêm</button>
                </a>
            </div>
        </div>


        <div id="slider">
            <div class="suggest">
                <h2>
                    <c:choose>
                        <c:when test="${isLoggedIn}">
                            <i class="fa-solid fa-star"></i> Dành riêng cho bạn
                        </c:when>
                        <c:otherwise>
                            Gợi ý cho bạn
                        </c:otherwise>
                    </c:choose>
                </h2>
            </div>

            <c:if test="${not empty recommendedComics}">
                <c:forEach var="i" begin="0" end="${(recommendedComics.size() - 1) / 8}" step="1">
                    <div class="product-slider">
                        <div class="arrow prev">&#10094;</div>
                        <div class="slider-track">
                            <c:forEach var="comic" items="${recommendedComics}"
                                       begin="${i * 8}"
                                       end="${i * 8 + 7}">
                                <div class="product-item">
                                    <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                                        <img src="${comic.thumbnailUrl}"
                                             alt="${comic.nameComics}"
                                             onerror="this.src='${pageContext.request.contextPath}/img/no-image.jpg'">
                                        <p class="product-name">${comic.nameComics}</p>
                                    </a>
                                    <c:choose>
                                        <c:when test="${comic.hasAnyDiscount()}">
                                            <c:if test="${comic.hasFlashSale}">
                                                <div class="flash-sale-badge-small">
                                                    <i class="fas fa-bolt"></i> Flash Sale
                                                </div>
                                            </c:if>
                                            <p class="product-price">
                                                <fmt:formatNumber value="${comic.finalPrice}"
                                                                  pattern="#,##0"/> đ
                                            </p>
                                            <p class="original-price">
                                                <s><fmt:formatNumber value="${comic.price}"
                                                                     pattern="#,##0"/> đ</s>
                                                <span class="discount-badge ${comic.hasFlashSale ? 'flash-badge' : ''}">
                -<fmt:formatNumber value="${comic.finalDiscountPercent}"
                                   pattern="#" maxFractionDigits="0"/>%
            </span>
                                            </p>
                                        </c:when>
                                        <c:otherwise>
                                            <%-- Không có giảm giá --%>
                                            <p class="product-price">
                                                <fmt:formatNumber value="${comic.price}"
                                                                  pattern="#,##0"/> đ
                                            </p>
                                        </c:otherwise>
                                    </c:choose>

                                    <p class="sold">Đã bán: <strong>${comic.totalSold}</strong></p>
                                </div>
                            </c:forEach>
                        </div>
                        <div class="arrow next">&#10095;</div>
                    </div>
                </c:forEach>
            </c:if>

            <c:if test="${empty recommendedComics}">
                <div class="no-products">
                    <p>Chưa có sản phẩm gợi ý</p>
                </div>
            </c:if>
        </div>


        <div class="top-truyen">
            <h2>Top sách bán chạy trong tuần</h2>
            <div class="content-top">
                <div class="list-book">
                    <ol>
                        <c:forEach var="comic" items="${topComics}" varStatus="status">
                            <li id="item-pop-${status.index + 1}"
                                onclick="showTopComicDetail(${status.index + 1})">
                                <div class="sach-item">
                                        <%-- Badge Flash Sale (nếu có) --%>
                                    <c:if test="${comic.hasFlashSale}">
                                        <div class="flash-sale-badge-top">
                                            <i class="fas fa-bolt"></i>
                                        </div>
                                    </c:if>

                                    <img src="${comic.thumbnailUrl}"
                                         alt="${comic.nameComics}"
                                         onerror="this.src='${pageContext.request.contextPath}/img/no-image.jpg'">

                                    <div class="sach-info">
                                        <h3>${comic.nameComics}</h3>

                                            <%-- Hiển thị giá --%>
                                        <c:choose>
                                            <c:when test="${comic.hasAnyDiscount()}">
                                                <p class="price-top">
                                    <span class="current-price">
                                        <fmt:formatNumber value="${comic.finalPrice}"
                                                          pattern="#,##0"/> đ
                                    </span>
                                                    <span class="original-price-top">
                                        <s><fmt:formatNumber value="${comic.price}"
                                                             pattern="#,##0"/> đ</s>
                                    </span>
                                                </p>
                                                <p class="discount-top ${comic.hasFlashSale ? 'flash-discount' : ''}">
                                                    -<fmt:formatNumber value="${comic.finalDiscountPercent}"
                                                                       pattern="#" maxFractionDigits="0"/>%
                                                </p>
                                            </c:when>
                                            <c:otherwise>
                                                <p class="price-top">
                                                    <fmt:formatNumber value="${comic.price}"
                                                                      pattern="#,##0"/> đ
                                                </p>
                                            </c:otherwise>
                                        </c:choose>

                                        <p class="sold-top">Đã bán: <strong>${comic.totalSold}</strong></p>
                                    </div>
                                </div>
                            </li>
                        </c:forEach>
                    </ol>
                </div>

                <%-- Chi tiết sách bên phải (DYNAMIC) --%>
                <div class="sach-chi-tiet-container">
                    <c:forEach var="comic" items="${topComics}" varStatus="status">
                        <div class="sach-chi-tiet pop-detail-home${status.index + 1}"
                             style="display: ${status.index == 0 ? 'flex' : 'none'};">

                            <div class="item-top-1">
                                    <%-- Badge Flash Sale --%>
                                <c:if test="${comic.hasFlashSale}">
                                    <div class="flash-sale-badge-detail">
                                        <i class="fas fa-bolt"></i> FLASH SALE
                                    </div>
                                </c:if>

                                <img src="${comic.thumbnailUrl}"
                                     alt="${comic.nameComics}"
                                     onerror="this.src='${pageContext.request.contextPath}/img/no-image.jpg'">
                            </div>
                            <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                                <div class="detail-top-1">
                                    <h3>${comic.nameComics}</h3>

                                        <%-- Tác giả (nếu có) --%>
                                    <c:if test="${not empty comic.author}">
                                        <p>Tác giả: ${comic.author}</p>
                                    </c:if>

                                        <%-- Nhà xuất bản (nếu có) --%>
                                    <c:if test="${not empty comic.publisher}">
                                        <p>Nhà xuất bản: ${comic.publisher}</p>
                                    </c:if>

                                        <%-- Giá --%>
                                    <c:choose>
                                        <c:when test="${comic.hasAnyDiscount()}">
                                            <p class="sale-price">
                                                <fmt:formatNumber value="${comic.finalPrice}"
                                                                  pattern="#,##0"/> đ
                                            </p>
                                            <div class="sale-item">
                                                <p><s><fmt:formatNumber value="${comic.price}"
                                                                        pattern="#,##0"/> đ</s></p>
                                                <div class="sale-percent ${comic.hasFlashSale ? 'flash-percent' : ''}">
                                                    -<fmt:formatNumber value="${comic.finalDiscountPercent}"
                                                                       pattern="#" maxFractionDigits="0"/>%
                                                </div>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="sale-price">
                                                <fmt:formatNumber value="${comic.price}"
                                                                  pattern="#,##0"/> đ
                                            </p>
                                        </c:otherwise>
                                    </c:choose>
                                    <c:if test="${not empty comic.description}">
                                        <p class="description">
                                                ${comic.description}
                                        </p>
                                    </c:if>
                                </div>
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>

    </div>
</div>

<jsp:include page="/frontend/public/Footer.jsp"/>
</body>

</html>