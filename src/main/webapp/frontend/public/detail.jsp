<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/detail.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/ReivewOfCus.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/SuggesstItem.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="${pageContext.request.contextPath}/js/detail.js"></script>
    <style>
        .heart-toggle {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            cursor: pointer;
        }

        .heart-toggle i {
            font-size: 22px;
            transition: transform 0.2s ease, opacity 0.2s ease;
        }

        .heart-toggle .fa-solid {
            display: none;
            color: #e53935;
        }

        .heart-toggle input:checked ~ .fa-regular {
            display: none;
        }

        .heart-toggle input:checked ~ .fa-solid {
            display: inline-block;
        }

    </style>
</head>

<body>

<jsp:include page="/frontend/public/header.jsp"/>

<input type="hidden" id="stockQuantity" value="${comic.stockQuantity}">
<input type="hidden" id="comicId" value="${comic.id}">

<c:if test="${not empty sessionScope.successMsg}">
    <div class="alert alert-success"
         style="position: fixed; top: 80px; right: 20px; z-index: 9999; background: #4CAF50; color: white; padding: 15px 20px; border-radius: 5px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); animation: slideIn 0.3s ease-out;">
        <i class="fas fa-check-circle"></i>
            ${sessionScope.successMsg}
    </div>
    <c:remove var="successMsg" scope="session"/>
    <script>
        setTimeout(function () {
            document.querySelector('.alert-success').style.animation = 'slideOut 0.3s ease-out';
            setTimeout(function () {
                const alert = document.querySelector('.alert-success');
                if (alert) alert.remove();
            }, 300);
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
            document.querySelector('.alert-error').style.animation = 'slideOut 0.3s ease-out';
            setTimeout(function () {
                const alert = document.querySelector('.alert-error');
                if (alert) alert.remove();
            }, 300);
        }, 3000);
    </script>
</c:if>

<div class="container-body">

    <div class="content-left">
        <div class="cont-left-body">

            <div id="img-container" class="img-container">
                <img id="img" src="${comic.thumbnailUrl}" alt="${comic.nameComics}">
            </div>
            <c:forEach var="image" items="${images}" varStatus="status">
                <div id="img-popup-${status.index}" class="img-container" style="display: none;">
                    <img class="img-small-popup" src="${image.imageUrl}" alt="${comic.nameComics}">
                </div>
            </c:forEach>

            <div class="warehouse-img">
                <img class="img-small" data-img-index="-1" src="${comic.thumbnailUrl}" alt="${comic.nameComics}">
                <c:forEach var="image" items="${images}" varStatus="status">
                    <img class="img-small" data-img-index="${status.index}"
                         src="${image.imageUrl}" alt="">
                </c:forEach>
            </div>


            <div class="actions-btn">
                <c:choose>
                    <c:when test="${comic.stockQuantity == 0}">
                        <button class="btn add-to-cart" disabled style="background-color: #ccc; cursor: not-allowed;">
                            Hết hàng
                        </button>
                        <button class="btn buy-now" disabled style="background-color: #ccc; cursor: not-allowed;">
                            Hết hàng
                        </button>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/cart?action=add&comicId=${comic.id}&quantity=1"
                           id="add-to-cart-link">
                            <button class="btn add-to-cart">Thêm vào giỏ hàng</button>
                        </a>
                        <a href="${pageContext.request.contextPath}/checkout?comicId=${comic.id}&quantity=1"
                           id="buy-now-link">
                            <button class="btn buy-now">Mua ngay</button>
                        </a>
                    </c:otherwise>
                </c:choose>
                <label class="heart-toggle" data-comic-id="${comic.id}">
                    <input id="hear_toggle" type="checkbox" hidden>
                    <i class="fa-regular fa-heart"></i>
                    <i class="fa-solid fa-heart"></i>
                    <p>Like</p>
                </label>

            </div>
        </div>
    </div>
    <div class="all-content">
        <div class="content">
            <h2>${comic.nameComics}
            </h2>
            <div class="information">

                <div class="line1">
                    <c:if test="${not empty comic.publisher}">

                        <p>Nhà xuất bản:
                            <strong>
                    <span class="publisher-link"
                          data-publisher="${comic.publisher}"
                          role="button"
                          tabindex="0">
                            ${comic.publisher}
                    </span>
                            </strong>
                        </p>
                    </c:if>
                </div>
                <div class="line2">
                    <c:if test="${not empty comic.author}">
                        <p>Tác giả:
                            <strong>
                    <span class="author-link"
                          data-author="${comic.author}"
                          role="button"
                          tabindex="0">
                            ${comic.author}
                    </span>
                            </strong>
                        </p>
                    </c:if>
                </div>
            </div>

            <p class="daban">Đã bán ${totalSell}</p>
<%--            <p class="available">Có sẳn ${availableComic}</p>--%>


            <div class="line3">
                <div class="star">
                    <i class="fas fa-star"></i>
                    <i class="fas fa-star"></i>
                    <i class="fas fa-star"></i>
                    <i class="fas fa-star"></i>
                    <i class="fas fa-star"></i>
                </div>
            </div>
            <div class="line4">
                <c:choose>
                    <c:when test="${comic.hasFlashSale}">
                        <p id="giamdagiam">
                            <fmt:formatNumber value="${comic.finalPrice}" pattern="#,###"/> đ
                        </p>
                        <p id="giagoc">
                            <fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ
                        </p>
                        <p id="khuyenmai">
                            -<fmt:formatNumber value="${comic.finalDiscountPercent}" pattern="#" maxFractionDigits="0"/>%
                        </p>
                    </c:when>

                    <c:when test="${comic.hasAnyDiscount() and not comic.hasFlashSale}">
                        <p id="giamdagiam">
                            <fmt:formatNumber value="${comic.finalPrice}" pattern="#,###"/> đ
                        </p>
                        <p id="giagoc">
                            <fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ
                        </p>
                        <p id="khuyenmai">
                            -<fmt:formatNumber value="${comic.finalDiscountPercent}" pattern="#" maxFractionDigits="0"/>%
                        </p>
                    </c:when>

                    <c:otherwise>
                        <p id="giamdagiam">
                            <fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="line5">
                <p>Chính sách khuyến mãi trên chỉ áp dụng tại Comic Store</p>
            </div>
            <div class="line6">
                <c:choose>
                    <c:when test="${comic.stockQuantity == 0}">
                        <p><em style="color: red;">Hết hàng</em></p>
                    </c:when>
                    <c:when test="${comic.stockQuantity > 0 and comic.stockQuantity < 10}">
                        <p><em>Sản phẩm gần hết hàng (còn ${comic.stockQuantity})</em></p>
                    </c:when>
                    <c:otherwise>
                        <p><em>Còn hàng</em></p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="container2">
            <div class="voucherbutton">
                <p>Số lượng: </p>
                <div class="quantity-control">
                    <button type="button" class="quantity-btn minus-btn">
                        <i class="fas fa-minus" id="btn-vol1"></i>
                    </button>
                    <span id="quantity-display">1</span>
                    <button type="button" class="quantity-btn plus-btn">
                        <i class="fas fa-plus" id="btn-vol2"></i>
                    </button>
                </div>
            </div>

            <c:if test="${not empty comic.seriesId && not empty seriesName}">
                <div style="display: flex; align-items: center; gap: 8px; font-family: sans-serif;">
                    <div style="background-color: #007bff; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px;">
                        Bộ
                    </div>
                    <div style="font-size: 18px; font-weight: bold;">
                        <a href="${pageContext.request.contextPath}/series-detail?id=${comic.seriesId}"
                           class="series-name-link">
                                ${seriesName}
                        </a>
                    </div>
                </div>
            </c:if>

            <c:if test="${not empty comic.description}">
                <div class="comic-description">
                    <h3>Mô tả</h3>
                    <p>${comic.description}</p>
                </div>
            </c:if>
        </div>


        <div class="container3">
            <section class="related-products">
                <h2>Sản phẩm tương tự</h2>
                <div class="product-grid">
                    <c:forEach var="relatedComic" items="${relatedComics}">
                        <div class="product-card">
                            <a href="${pageContext.request.contextPath}/comic-detail?id=${relatedComic.id}">
                                <!-- Flash Sale Badge -->
                                <c:if test="${relatedComic.hasFlashSale}">
                                    <div class="flash-sale-badge">
                                        <i class="fas fa-bolt"></i> FLASH SALE
                                    </div>
                                </c:if>

                                <img src="${relatedComic.thumbnailUrl}" alt="${relatedComic.nameComics}">
                                <h3>${relatedComic.nameComics}</h3>
                                <div class="priceFotmat" style="display: flex">
                                    <c:choose>
                                        <c:when test="${relatedComic.hasFlashSale}">
                                            <div class="price-section">
                                                <p class="flash-price">
                                                    <fmt:formatNumber value="${relatedComic.flashSalePrice}"
                                                                      pattern="#,###"/> đ
                                                </p>
                                                <p class="original-price">
                                                    <fmt:formatNumber value="${relatedComic.price}" pattern="#,###"/> đ
                                                </p>
                                            </div>
                                        </c:when>
                                        <c:when test="${relatedComic.hasDiscount()}">
                                            <div class="price-section">
                                                <p class="discount-price">
                                                    <fmt:formatNumber value="${relatedComic.discountPrice}"
                                                                      pattern="#,###"/> đ
                                                </p>
                                                <p class="original-price">
                                                    <fmt:formatNumber value="${relatedComic.price}" pattern="#,###"/> đ
                                                </p>
                                                <span class="discount-badge">
                                        -<fmt:formatNumber value="${relatedComic.discountPercent}" pattern="#"/>%
                                    </span>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="price">
                                                <fmt:formatNumber value="${relatedComic.price}" pattern="#,###"/> đ
                                            </p>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <p class="sold">Đã bán:
                                    <strong>${relatedComic.totalSold != null ? relatedComic.totalSold : 0}</strong></p>
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </section>
        </div>

    </div>
</div>


<div id="authorPopup" class="info-popup" style="display: none;">
    <div class="popup-overlay"></div>
    <div class="popup-content">
        <button class="popup-close">&times;</button>
        <h2 id="authorPopupTitle">Thông tin tác giả</h2>
        <div id="authorPopupBody" class="popup-body">
            <div class="loading">
                <i class="fas fa-spinner fa-spin"></i> Đang tải...
            </div>
        </div>
    </div>
</div>

<div id="publisherPopup" class="info-popup" style="display: none;">
    <div class="popup-overlay"></div>
    <div class="popup-content">
        <button class="popup-close">&times;</button>
        <h2 id="publisherPopupTitle">Thông tin nhà xuất bản</h2>
        <div id="publisherPopupBody" class="popup-body">
            <div class="loading">
                <i class="fas fa-spinner fa-spin"></i> Đang tải...
            </div>
        </div>
    </div>
</div>


<div class="rating-container">
    <div class="rating-left">
        <h3>Đánh giá sản phẩm</h3>

        <div class="rating-score">
            <div class="score-number">
                <fmt:formatNumber value="${avgRating}" maxFractionDigits="1"/><span>/5</span>
            </div>
            <div class="score-stars">
                <c:set var="fullStars"
                       value="${avgRating >= 1 ? (avgRating >= 2 ? (avgRating >= 3 ? (avgRating >= 4 ? (avgRating >= 5 ? 5 : 4) : 3) : 2) : 1) : 0}"/>
                <c:forEach begin="1" end="${fullStars}">★</c:forEach>
                <c:set var="hasHalfStar" value="${avgRating - fullStars >= 0.5}"/>
                <c:if test="${hasHalfStar}">★</c:if>
                <c:set var="emptyStars" value="${5 - fullStars - (hasHalfStar ? 1 : 0)}"/>
                <c:forEach begin="1" end="${emptyStars}">☆</c:forEach>
            </div>
            <div class="score-count">(${totalReviews} đánh giá)</div>
        </div>
        <div class="rating-bars">
            <%
                java.util.Map<Integer, Integer> dist = (java.util.Map<Integer, Integer>) request.getAttribute("ratingDistribution");
                Integer total = (Integer) request.getAttribute("totalReviews");

                if (dist == null) dist = new java.util.HashMap<>();
                if (total == null) total = 0;

                for (int star = 5; star >= 1; star--) {
                    Integer count = dist.get(star);
                    if (count == null) count = 0;
                    double percentage = (total > 0) ? (count * 100.0 / total) : 0;
            %>
            <div class="bar-row">
                <span><%= star %> sao</span>
                <div class="bar">
                    <div class="fill" style="width: <%= String.format("%.0f", percentage) %>%;"></div>
                </div>
                <span><%= String.format("%.0f", percentage) %>%</span>
            </div>
            <% } %>
        </div>
    </div>
</div>

</div>

<div class="comment">

    <div class="review-tabs">
        <label id="tab1" class="tab active">Tất cả đánh giá (${totalReviews})</label>
    </div>

    <div id="reviewed-person" class="reviewed-person">
        <c:choose>
            <c:when test="${empty reviews}">
                <p style="text-align: center; padding: 40px; color: #999;">
                    <i class="fas fa-inbox" style="font-size: 48px; display: block; margin-bottom: 10px;"></i>
                    Chưa có đánh giá nào.
                </p>
            </c:when>
            <c:otherwise>
                <c:forEach var="review" items="${reviews}">
                    <div class="review-item">
                        <div class="review-left">
                            <div class="avatar">
                                    ${fn:substring(review.username, 0, 2).toUpperCase()}
                            </div>
                            <div class="review-username">${review.username}</div>
                            <div class="review-date">
                                <fmt:formatDate value="${review.createdAt}" pattern="dd/MM/yyyy"/>
                            </div>
                        </div>

                        <div class="review-right">
                            <div class="review-stars">
                                <c:forEach begin="1" end="${review.rating}">★</c:forEach>
                                <c:forEach begin="${review.rating + 1}" end="5">☆</c:forEach>
                            </div>

                            <p class="review-text">${review.comment}</p>
                            <c:if test="${not empty review.images}">
                                <div class="review-images">
                                    <c:forEach var="image" items="${review.images}">
                                        <div class="review-image-wrapper">
                                            <img src="${image.imageUrl}"
                                                 alt="Review image"
                                                 class="review-image"
                                                 onclick="openImageModal('${image.imageUrl}')">
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div id="imageModal" class="image-modal" onclick="closeImageModal()">
    <span class="image-modal-close">&times;</span>
    <img class="image-modal-content" id="modalImage">
</div>
</div>
<div class="container-slider">
    <div id="slider-suggestions">
        <div class="suggest">
            <h2>Gợi ý cho bạn</h2>
        </div>
        <c:choose>
        <c:when test="${not empty suggestedComics}">
            <c:forEach var="row" begin="0" end="2" step="1">
                <c:set var="startIndex" value="${row * 8}"/>
                <c:set var="endIndex" value="${row * 8 + 7}"/>

                <c:if test="${startIndex < suggestedComics.size()}">
                    <div class="product-slider" data-row="${row}">
                        <button class="arrow prev" type="button" aria-label="Previous">
                            <i class="fas fa-chevron-left"></i>
                        </button>

                        <div class="slider-viewport">
                            <div class="slider-track">
                                <c:forEach var="suggested" items="${suggestedComics}"
                                           begin="${startIndex}"
                                           end="${endIndex > suggestedComics.size() - 1 ? suggestedComics.size() - 1 : endIndex}">
                                    <div class="product-item">
                                        <a href="${pageContext.request.contextPath}/comic-detail?id=${suggested.id}">
                                            <c:if test="${suggested.hasFlashSale}">
                                                <div class="flash-sale-badge-small">
                                                    <i class="fas fa-bolt"></i>
                                                    -<fmt:formatNumber value="${suggested.flashSaleDiscount}"
                                                                       pattern="#"/>%
                                                </div>
                                            </c:if>

                                            <img src="${suggested.thumbnailUrl}" alt="${suggested.nameComics}">
                                            <p class="product-name">${suggested.nameComics}</p>

                                            <c:choose>
                                                <c:when test="${suggested.hasFlashSale}">
                                                    <div class="price-wrapper">
                                                        <p class="product-price flash">
                                                            <fmt:formatNumber value="${suggested.flashSalePrice}"
                                                                              pattern="#,###"/> đ
                                                        </p>
                                                        <p class="original-price-small">
                                                            <fmt:formatNumber value="${suggested.price}"
                                                                              pattern="#,###"/> đ
                                                        </p>
                                                    </div>
                                                </c:when>
                                                <c:when test="${suggested.hasDiscount()}">
                                                    <div class="price-wrapper">
                                                        <p class="product-price">
                                                            <fmt:formatNumber value="${suggested.discountPrice}"
                                                                              pattern="#,###"/> đ
                                                        </p>
                                                        <p class="original-price-small">
                                                            <fmt:formatNumber value="${suggested.price}"
                                                                              pattern="#,###"/> đ
                                                        </p>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <p class="product-price">
                                                        <fmt:formatNumber value="${suggested.price}"
                                                                          pattern="#,###"/> đ
                                                    </p>
                                                </c:otherwise>
                                            </c:choose>

                                            <p class="sold">Đã bán:
                                                <strong>${suggested.totalSold != null ? suggested.totalSold : 0}</strong>
                                            </p>
                                        </a>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>

                        <button class="arrow next" type="button" aria-label="Next">
                            <i class="fas fa-chevron-right"></i>
                        </button>
                    </div>
                </c:if>
            </c:forEach>
        </c:when>
        <c:otherwise>
        <div style="text-align: center; padding: 40px; color: white;">
            <i class="fas fa-book" style="font-size: 48px; margin-bottom: 10px;"></i>
            <p>Chưa có gợi ý phù hợp</p>
        </div>
    </div>
    </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    window.contextPath = '${pageContext.request.contextPath}';
</script>


<script>
    document.addEventListener('DOMContentLoaded', function () {
        const contextPath = '${pageContext.request.contextPath}';

        const authorLinks = document.querySelectorAll('.author-link');

        authorLinks.forEach(link => {
            link.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                const authorName = this.getAttribute('data-author');
                showInfoPopup('author', authorName);
            });
        });

        const publisherLinks = document.querySelectorAll('.publisher-link');

        publisherLinks.forEach(link => {
            link.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                const publisherName = this.getAttribute('data-publisher');
                showInfoPopup('publisher', publisherName);
            });
        });

        document.addEventListener('click', function (e) {
            if (e.target.classList.contains('popup-close') ||
                e.target.classList.contains('popup-overlay')) {
                document.querySelectorAll('.info-popup').forEach(popup => {
                    popup.style.display = 'none';
                });
            }
        });

        function showInfoPopup(type, name) {


            const popupId = type === 'author' ? 'authorPopup' : 'publisherPopup';
            const popup = document.getElementById(popupId);

            if (!popup) {
                return;
            }

            const typeText = type === 'author' ? 'Tác giả' : 'Nhà xuất bản';
            const title = document.getElementById(popupId + 'Title');
            const body = document.getElementById(popupId + 'Body');

            title.textContent = typeText + ': ' + name;
            body.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
            popup.style.display = 'flex';

            const contextPath = window.contextPath || '';
            const protocol = window.location.protocol;
            const host = window.location.host;

            const url = protocol + '//' + host + contextPath + '/author-publisher-info?type=' +
                encodeURIComponent(type) + '&name=' + encodeURIComponent(name);

            fetch(url)
                .then(res => {


                    if (!res.ok) {
                        return res.text().then(text => {
                            throw new Error('HTTP ' + res.status + ': ' + text.substring(0, 100));
                        });
                    }

                    const contentType = res.headers.get('content-type');
                    if (!contentType || !contentType.includes('application/json')) {
                        return res.text().then(text => {
                            throw new Error('Response is not JSON');
                        });
                    }

                    return res.json();
                })
                .then(data => {

                    if (data.success && Array.isArray(data.comics) && data.comics.length > 0) {
                        renderComics(body, data, typeText);
                    } else {
                        body.innerHTML = '<p style="padding:20px">Không có truyện nào.</p>';
                    }
                })
                .catch(err => {
                    body.innerHTML = '<div class="loading" style="color: #e74c3c;">Lỗi: ' + err.message + '</div>';
                });
        }

        function renderComics(container, data, typeText) {

            const entityName = data.authorName || data.publisherName;
            const icon = data.authorName ? 'fa-pen-nib' : 'fa-building';

            let html =
                '<div class="popup-stats">' +
                '<i class="fas ' + icon + '"></i>' +
                '<div class="popup-stats-text">' +
                '<h3>' + entityName + '</h3>' +
                '<p>Tổng số ' + data.totalComics + ' tác phẩm</p>' +
                '</div>' +
                '</div>' +
                '<div class="comics-grid">';

            data.comics.forEach(comic => {
                html +=
                    '<a href="' + window.contextPath + '/comic-detail?id=' + comic.id + '" class="comic-card">' +
                    '<img src="' + comic.thumbnailUrl + '" ' +
                    'alt="' + comic.nameComics + '" ' +
                    'onerror="this.src=\'https://via.placeholder.com/140x180?text=No+Image\'">' +
                    '<div class="comic-card-info">' +
                    '<div class="comic-card-title" title="' + comic.nameComics + '">'
                    + comic.nameComics +
                    '</div>' +
                    (comic.seriesName
                        ? '<div class="comic-card-series"> ' + comic.seriesName + '</div>'
                        : '') +
                    '<div class="comic-card-price">' +
                    formatPrice(comic.price) + ' đ' +
                    '</div>' +
                    '</div>' +
                    '</a>';
            });

            html += '</div>';
            container.innerHTML = html;
        }
        function formatPrice(price) {
            return new Intl.NumberFormat('vi-VN').format(price);
        }
    });
</script>


<script>
    document.addEventListener('DOMContentLoaded', function () {
        const quantityDisplay = document.getElementById('quantity-display');
        const minusBtn = document.querySelector('.minus-btn');
        const plusBtn = document.querySelector('.plus-btn');

        const stockInput = document.getElementById('stockQuantity');
        const comicIdInput = document.getElementById('comicId');
        const maxStock = stockInput ? parseInt(stockInput.value) : 999;
        const comicId = comicIdInput ? comicIdInput.value : '';

        let currentQuantity = 1;

        function updateQuantity(newQuantity) {
            currentQuantity = newQuantity;
            quantityDisplay.textContent = currentQuantity;

            minusBtn.disabled = currentQuantity <= 1;
            plusBtn.disabled = currentQuantity >= maxStock;

            updateCartLinks();
        }

        function updateCartLinks() {
            const contextPath = window.contextPath || '';

            const addToCartLink = document.getElementById('add-to-cart-link');
            if (addToCartLink) {
                addToCartLink.setAttribute('href',
                    contextPath + '/cart?action=add&comicId=' + comicId + '&quantity=' + currentQuantity);
            }

            const buyNowLink = document.getElementById('buy-now-link');
            if (buyNowLink) {
                buyNowLink.setAttribute('href',
                    contextPath + '/checkout?comicId=' + comicId + '&quantity=' + currentQuantity);
            }
        }

        minusBtn.addEventListener('click', function (e) {
            e.preventDefault();
            if (currentQuantity > 1) {
                updateQuantity(currentQuantity - 1);
            }
        });

        plusBtn.addEventListener('click', function (e) {
            e.preventDefault();
            if (currentQuantity < maxStock) {
                updateQuantity(currentQuantity + 1);
            }
        });

        quantityDisplay.addEventListener('click', function () {
            const input = document.createElement('input');
            input.type = 'number';
            input.min = '1';
            input.max = maxStock;
            input.value = currentQuantity;
            input.style.width = '60px';
            input.style.textAlign = 'center';
            input.style.border = '1px solid #ddd';
            input.style.borderRadius = '4px';
            input.style.padding = '4px';
            input.style.fontSize = '16px';

            this.replaceWith(input);
            input.focus();
            input.select();

            function handleInput() {
                let newValue = parseInt(input.value);

                if (isNaN(newValue) || newValue < 1) {
                    newValue = 1;
                } else if (newValue > maxStock) {
                    newValue = maxStock;
                    alert('Số lượng tồn kho chỉ còn ' + maxStock + ' sản phẩm!');
                }

                input.replaceWith(quantityDisplay);
                updateQuantity(newValue);
            }

            input.addEventListener('blur', handleInput);
            input.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    handleInput();
                }
            });
        });

        updateQuantity(1);
    });
</script>

<script>
    const mainImg = document.getElementById("img");
    const imgSmalls = document.querySelectorAll(".img-small");

    imgSmalls.forEach((imgSmall) => {
        imgSmall.addEventListener("click", () => {
            const imgIndex = imgSmall.getAttribute("data-img-index");
            console.log("Clicked img-index:", imgIndex);
            const newSrc = imgSmall.getAttribute("src");
            mainImg.src = newSrc;

            console.log("Changed to:", newSrc);
            document.querySelectorAll(".img-container").forEach(container => {
                container.style.display = "none";
            });
            if (imgIndex === "-1") {
                const mainContainer = document.getElementById("img-container");
                if (mainContainer) {
                    mainContainer.style.display = "block";
                    mainContainer.style.visibility = "visible"; // Thêm dòng này
                }
            } else {
                const mainContainer = document.getElementById("img-container");
                if (mainContainer) {
                    mainContainer.style.display = "block";
                    mainContainer.style.visibility = "visible"; // Thêm dòng này
                }
            }
        });
    });
    const heart = document.getElementById("heart");
    if (heart) {  // ← Kiểm tra null
        heart.addEventListener('click', () => {
            heart.classList.toggle("fa-regular");
            heart.classList.toggle("fa-solid");
        });
    }
    document.querySelectorAll(".review-tabs .tab").forEach(tab => {
        tab.addEventListener("click", function () {
            document.querySelectorAll(".review-tabs .tab").forEach(t => t.classList.remove("active"));
            this.classList.add("active");
        });
    });
    const tab2 = document.getElementById("tab2");
    if (tab2) {
        tab2.addEventListener("click", function (event) {
            event.preventDefault();
            document.querySelector(".reviewed-person").style.display = "none";
            document.querySelector(".reviewed-person-popup").style.display = "block";
        });
    }
    const tab1 = document.getElementById("tab1");
    if (tab1) {
        tab1.addEventListener("click", function (event) {
            event.preventDefault();
            document.querySelector(".reviewed-person").style.display = "block";
            document.querySelector(".reviewed-person-popup").style.display = "none";
        });
    }

    const moreBtn = document.getElementById("more-btn-popup-slider");
    if (moreBtn) {
        moreBtn.addEventListener("click", function () {
            document.querySelector("#product-slider-popup").style.display = "block";
        });
    }
</script>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const heartLabel = document.querySelector('.heart-toggle');
        const checkbox = heartLabel ? heartLabel.querySelector('input[type="checkbox"]') : null;

        if (!heartLabel || !checkbox) {
            console.error('Không tìm thấy phần tử wishlist');
            return;
        }

        const comicId = heartLabel.getAttribute('data-comic-id');

        if (!comicId) {
            console.error('Không tìm thấy comic ID');
            return;
        }

        fetch('${pageContext.request.contextPath}/WishlistServlet?comic_id=' + comicId, {
            credentials: 'include'
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    checkbox.checked = data.inWishlist;
                }
            })
            .catch(err => {
                console.error('Lỗi kiểm tra wishlist:', err);
            });

        heartLabel.addEventListener('click', function (e) {
            e.preventDefault();

            if (heartLabel.classList.contains('loading')) {
                return;
            }

            heartLabel.classList.add('loading');

            const willBeChecked = !checkbox.checked;
            const action = willBeChecked ? 'add' : 'remove';

            fetch('${pageContext.request.contextPath}/WishlistServlet', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                credentials: 'include',
                body: 'action=' + action + '&comic_id=' + comicId
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        checkbox.checked = willBeChecked;
                        showToast(data.message);
                        updateWishlistCount(data.count);
                    } else {
                        if (data.message.includes('đăng nhập')) {
                            showLoginPopup();
                        } else {
                            showToast(data.message, 'error');
                        }
                    }
                })
                .catch(err => {
                    console.error('Lỗi:', err);
                    showToast('Lỗi kết nối, vui lòng thử lại', 'error');
                })
                .finally(() => {
                    heartLabel.classList.remove('loading');
                });
        });
    });


    function showToast(message, type = 'success') {
        const oldToast = document.querySelector('.wishlist-toast');
        if (oldToast) {
            oldToast.classList.remove('show');
            setTimeout(() => oldToast.remove(), 300);
        }

        const toast = document.createElement('div');
        toast.className = 'wishlist-toast ' + type;
        toast.textContent = message;
        document.body.appendChild(toast);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => toast.classList.add('show'));
        });

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 400);
        }, 3000);
    }

    function showLoginPopup() {
        const old = document.querySelector('.login-popup-overlay');
        if (old) old.remove();

        const overlay = document.createElement('div');
        overlay.className = 'login-popup-overlay';
        overlay.innerHTML = `
        <div class="login-popup-box">
            <h3>Bạn chưa đăng nhập</h3>
            <p>Vui lòng đăng nhập để thêm sản phẩm vào danh sách yêu thích.</p>
            <div class="login-popup-actions">
                <button class="btn-cancel">Hủy</button>
                <a href="${pageContext.request.contextPath}/login" class="btn-login">
                    <i class="fas fa-sign-in-alt"></i> Đăng nhập
                </a>
            </div>
        </div>
    `;

        document.body.appendChild(overlay);

        requestAnimationFrame(() => {
            requestAnimationFrame(() => overlay.classList.add('show'));
        });

        overlay.querySelector('.btn-cancel').addEventListener('click', () => {
            overlay.classList.remove('show');
            setTimeout(() => overlay.remove(), 300);
        });

        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                overlay.classList.remove('show');
                setTimeout(() => overlay.remove(), 300);
            }
        });

        const escHandler = (e) => {
            if (e.key === 'Escape') {
                overlay.classList.remove('show');
                setTimeout(() => overlay.remove(), 300);
                document.removeEventListener('keydown', escHandler);
            }
        };
        document.addEventListener('keydown', escHandler);
    }

    function updateWishlistCount(count) {
        const countElement = document.querySelector('.wishlist-count');
        if (countElement) {
            countElement.textContent = count;
            countElement.classList.add('bounce');
            setTimeout(() => countElement.classList.remove('bounce'), 500);
        }
    }
</script>

<style>
    .heart-toggle.loading {
        pointer-events: none;
        opacity: 0.6;
    }
</style>

<script>
    function openImageModal(imageUrl) {
        const modal = document.getElementById('imageModal');
        const modalImg = document.getElementById('modalImage');

        modal.style.display = 'block';
        modalImg.src = imageUrl;
    }

    function closeImageModal() {
        document.getElementById('imageModal').style.display = 'none';
    }

    // Đóng modal khi nhấn ESC
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeImageModal();
        }
    });
</script>

</body>

</html>
