<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${selectedCategory.nameCategories} - Comic Store</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/CatagoryPage.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="${pageContext.request.contextPath}/js/category.js"></script>

    <style>
        .slider-viewport {
            margin: 0;

        }

    </style>
</head>

<body>
<jsp:include page="/frontend/public/header.jsp"/>

<div class="container-content">

    <div class="container">
        <div id="filter-menu">
            <div class="title-filter">
                <h2>Bộ lọc sản phẩm</h2>
                <button class="btn-loc" onclick="applyFilters()">Lọc</button>
                <button class="btn-reset" onclick="resetFilters()">Đặt lại</button>
            </div>

            <form id="filterForm" method="get" action="${pageContext.request.contextPath}/userCategory">
                <input type="hidden" name="id" value="${selectedCategory.id}">
                <section>
                    <h3>GIÁ:</h3>
                    <ul>
                        <li>
                            <input type="checkbox" name="price" value="0-15000" id="price1"
                            ${selectedPrices.contains('0-15000') ? 'checked' : ''}>
                            <label for="price1">0đ - 15,000đ</label>
                        </li>
                        <li>
                            <input type="checkbox" name="price" value="15000-30000" id="price2"
                            ${selectedPrices.contains('15000-30000') ? 'checked' : ''}>
                            <label for="price2">15,000đ - 30,000đ</label>
                        </li>
                        <li>
                            <input type="checkbox" name="price" value="30000-50000" id="price3"
                            ${selectedPrices.contains('30000-50000') ? 'checked' : ''}>
                            <label for="price3">30,000đ - 50,000đ</label>
                        </li>
                        <li>
                            <input type="checkbox" name="price" value="50000-70000" id="price4"
                            ${selectedPrices.contains('50000-70000') ? 'checked' : ''}>
                            <label for="price4">50,000đ - 70,000đ</label>
                        </li>
                        <li>
                            <input type="checkbox" name="price" value="70000-100000" id="price5"
                            ${selectedPrices.contains('70000-100000') ? 'checked' : ''}>
                            <label for="price5">70,000đ - 100,000đ</label>
                        </li>
                        <li>
                            <input type="checkbox" name="price" value="100000+" id="price6"
                            ${selectedPrices.contains('100000+') ? 'checked' : ''}>
                            <label for="price6">Trên 100,000đ</label>
                        </li>
                    </ul>
                </section>
                <section>
                    <h3>TÁC GIẢ:</h3>
                    <ul>
                        <c:forEach var="author" items="${availableAuthors}" varStatus="status">
                            <li>
                                <input type="checkbox" name="author" value="${author}"
                                       id="author${status.index}"
                                    ${selectedAuthors.contains(author) ? 'checked' : ''}>
                                <label for="author${status.index}">${author}</label>
                            </li>
                        </c:forEach>
                        <c:if test="${empty availableAuthors}">
                            <li style="color: #999; font-style: italic;">Không có tác giả</li>
                        </c:if>
                    </ul>
                </section>
                <section>
                    <h3>NHÀ XUẤT BẢN:</h3>
                    <ul>
                        <c:forEach var="publisher" items="${availablePublishers}" varStatus="status">
                            <li>
                                <input type="checkbox" name="publisher" value="${publisher}"
                                       id="publisher${status.index}"
                                    ${selectedPublishers.contains(publisher) ? 'checked' : ''}>
                                <label for="publisher${status.index}">${publisher}</label>
                            </li>
                        </c:forEach>
                        <c:if test="${empty availablePublishers}">
                            <li style="color: #999; font-style: italic;">Không có nhà xuất bản</li>
                        </c:if>
                    </ul>
                </section>
                <section>
                    <h3>THỜI GIAN PHÁT HÀNH:</h3>
                    <ul>
                        <li>
                            <input type="checkbox" name="year" value="recent" id="time1"
                            ${selectedYears.contains('recent') ? 'checked' : ''}>
                            <label for="time1">Gần đây (năm nay)</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="2024" id="time2"
                            ${selectedYears.contains('2024') ? 'checked' : ''}>
                            <label for="time2">2024</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="2023" id="time3"
                            ${selectedYears.contains('2023') ? 'checked' : ''}>
                            <label for="time3">2023</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="2022" id="time4"
                            ${selectedYears.contains('2022') ? 'checked' : ''}>
                            <label for="time4">2022</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="2021" id="time5"
                            ${selectedYears.contains('2021') ? 'checked' : ''}>
                            <label for="time5">2021</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="2020" id="time6"
                            ${selectedYears.contains('2020') ? 'checked' : ''}>
                            <label for="time6">2020</label>
                        </li>
                        <li>
                            <input type="checkbox" name="year" value="before2020" id="time7"
                            ${selectedYears.contains('before2020') ? 'checked' : ''}>
                            <label for="time7">Trước 2020</label>
                        </li>
                    </ul>
                </section>
            </form>
        </div>
        <script>
            function applyFilters() {
                document.getElementById('filterForm').submit();
            }

            function resetFilters() {
                const checkboxes = document.querySelectorAll('#filterForm input[type="checkbox"]');
                checkboxes.forEach(cb => cb.checked = false);
                document.getElementById('filterForm').submit();
            }
        </script>
        <div id="items-listing">

            <h1>${selectedCategory.nameCategories}</h1>

            <div id="sum-items">
                <p><strong>Tổng sản phẩm hiển thị:</strong> ${totalComics}</p>
            </div>
            <section aria-label="Danh sách sản phẩm ${selectedCategory.nameCategories}">

                <c:choose>
                    <c:when test="${empty comicList}">
                        <div class="no-products">
                            <p>Hiện tại chưa có sản phẩm nào trong thể loại này.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="comic" items="${comicList}">
                            <article>
                                <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                                    <div style="position: relative;">
                                        <!-- Badge Flash Sale -->
                                        <c:if test="${comic.hasFlashSale}">
                                            <div class="flash-sale-badge">
                                                <i class="fas fa-bolt"></i> FLASH SALE
                                            </div>
                                        </c:if>

                                        <img src="${comic.thumbnailUrl}"
                                             alt="${comic.nameComics}"
                                             onerror="this.src='${pageContext.request.contextPath}/img/no-image.png'">
                                        <div class="detail-book">
                                            <p>${comic.nameComics}
                                            </p>
                                            <c:choose>
                                                <c:when test="${comic.hasFlashSale}">
                                                    <p class="product-price flash">
                                                        <fmt:formatNumber value="${comic.flashSalePrice}"
                                                                          pattern="#,###"/> đ
                                                    </p>
                                                    <p class="original-price-small">
                                                        <s><fmt:formatNumber value="${comic.price}" pattern="#,###"/>
                                                            đ</s>
                                                        <span class="discount-badge-small flash">
                                                -<fmt:formatNumber value="${comic.flashSaleDiscount}" pattern="#"/>%
                                            </span>
                                                    </p>
                                                </c:when>
                                                <c:otherwise>
                                                    <p class="product-price">
                                                        <fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ
                                                    </p>
                                                </c:otherwise>
                                            </c:choose>
                                            <c:choose>
                                                <c:when test="${comic.stockQuantity > 0}">
                                                    <p class="sold">Còn hàng: <strong>${comic.stockQuantity}</strong>
                                                    </p>
                                                </c:when>
                                                <c:otherwise>
                                                    <p class="sold out-of-stock">Hết hàng</p>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </a>
                            </article>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>

            </section>
            <div class="pagination">
                <c:if test="${totalPages > 1}">
                    <c:choose>
                        <c:when test="${currentPage > 1}">
                            <a class="page-btn"
                               href="${pageContext.request.contextPath}/category?id=${selectedCategory.id}&page=${currentPage - 1}<c:forEach var="p" items="${selectedPrices}">&price=${p}</c:forEach><c:forEach var="a" items="${selectedAuthors}">&author=${a}</c:forEach><c:forEach var="pub" items="${selectedPublishers}">&publisher=${pub}</c:forEach><c:forEach var="y" items="${selectedYears}">&year=${y}</c:forEach>">
                                <i class="fas fa-chevron-left"></i>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="page-btn disabled"><i class="fas fa-chevron-left"></i></span>
                        </c:otherwise>
                    </c:choose>
                    <c:forEach var="i" begin="1" end="${totalPages}">
                        <c:choose>
                            <c:when test="${i == 1 || i == totalPages || (i >= currentPage - 2 && i <= currentPage + 2)}">
                                <c:choose>
                                    <c:when test="${i == currentPage}">
                                        <span class="page-btn active">${i}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="page-btn"
                                           href="${pageContext.request.contextPath}/category?id=${selectedCategory.id}&page=${i}<c:forEach var="p" items="${selectedPrices}">&price=${p}</c:forEach><c:forEach var="a" items="${selectedAuthors}">&author=${a}</c:forEach><c:forEach var="pub" items="${selectedPublishers}">&publisher=${pub}</c:forEach><c:forEach var="y" items="${selectedYears}">&year=${y}</c:forEach>">
                                                ${i}
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:when test="${i == currentPage - 3 && currentPage > 4}">
                                <span class="page-btn dots">...</span>
                            </c:when>
                            <c:when test="${i == currentPage + 3 && currentPage < totalPages - 3}">
                                <span class="page-btn dots">...</span>
                            </c:when>
                        </c:choose>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${currentPage < totalPages}">
                            <a class="page-btn"
                               href="${pageContext.request.contextPath}/category?id=${selectedCategory.id}&page=${currentPage + 1}<c:forEach var="p" items="${selectedPrices}">&price=${p}</c:forEach><c:forEach var="a" items="${selectedAuthors}">&author=${a}</c:forEach><c:forEach var="pub" items="${selectedPublishers}">&publisher=${pub}</c:forEach><c:forEach var="y" items="${selectedYears}">&year=${y}</c:forEach>">
                                <i class="fas fa-chevron-right"></i>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="page-btn disabled"><i class="fas fa-chevron-right"></i></span>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </div>
        </div>


    </div>
    <c:if test="${not empty recommendations}">
        <div id="slider">
            <div class="suggest">
                <h2>
                    <c:choose>
                        <c:when test="${isPersonalized}">
                            <i class="fas fa-star"></i> Gợi ý dành riêng cho bạn
                        </c:when>
                        <c:otherwise>
                            <i class="fas fa-fire"></i> Gợi ý cho bạn
                        </c:otherwise>
                    </c:choose>
                </h2>
            </div>
            <c:forEach var="entry" items="${recommendations}">
                <c:if test="${not empty entry.value}">
                    <div class="recommendation-section">
                        <h3 class="recommendation-title">
                        </h3>

                        <div class="product-slider">
                            <button class="arrow prev" type="button" aria-label="Previous">&#10094;</button>
                            <div class="slider-viewport">
                                <div class="slider-track">
                                    <c:forEach var="comic" items="${entry.value}">
                                        <div class="product-item">
                                            <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                                                <div style="position: relative;">
                                                    <c:if test="${comic.hasFlashSale}">
                                                        <div class="flash-sale-badge-small">
                                                            <i class="fas fa-bolt"></i>Flash sale
                                                        </div>
                                                    </c:if>

                                                    <img src="${comic.thumbnailUrl}"
                                                         alt="${comic.nameComics}"
                                                         onerror="this.src='${pageContext.request.contextPath}/img/no-image.png'">
                                                </div>

                                                <p class="product-name">
                                                        ${comic.nameComics}
                                                </p>
                                                <c:choose>
                                                    <c:when test="${comic.hasFlashSale}">
                                                        <p class="product-price flash">
                                                            <fmt:formatNumber value="${comic.flashSalePrice}"
                                                                              pattern="#,###"/> đ
                                                        </p>
                                                        <p class="original-price-slider">
                                                            <s><fmt:formatNumber value="${comic.price}"
                                                                                 pattern="#,###"/> đ</s>
                                                            <span class="discount-tag-slider">
                                                    -<fmt:formatNumber value="${comic.flashSaleDiscount}" pattern="#"/>%
                                                </span>
                                                        </p>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <p class="product-price">
                                                            <fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ
                                                        </p>
                                                    </c:otherwise>
                                                </c:choose>

                                                <c:choose>
                                                    <c:when test="${comic.stockQuantity > 0}">
                                                        <p class="sold">Còn hàng:
                                                            <strong>${comic.stockQuantity}</strong></p>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <p class="sold out-of-stock">Hết hàng</p>
                                                    </c:otherwise>
                                                </c:choose>
                                            </a>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <button class="arrow next" type="button" aria-label="Next">&#10095;</button>
                        </div>
                    </div>
                </c:if>
            </c:forEach>

        </div>
    </c:if>


</div>
<jsp:include page="/frontend/public/Footer.jsp"/>

<style>
    .out-of-stock {
        color: #d32f2f !important;
        font-weight: bold;
    }

    section[aria-label] article img {
        background-color: #f5f5f5;
    }

    .recommendation-section {
        margin-bottom: 30px;
    }

    .recommendation-title {
        color: white;
        font-size: 20px;
        margin-bottom: 15px;
        padding-left: 10px;
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .recommendation-title i {
        font-size: 18px;
    }

    .product-item a {
        text-decoration: none;
        color: inherit;
        display: block;
    }

    .product-item a:hover {
        opacity: 0.9;
    }
</style>


</body>
</html>