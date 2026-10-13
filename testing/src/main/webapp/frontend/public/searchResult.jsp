<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết quả tìm kiếm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/search.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/homePage.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<jsp:include page="/frontend/public/header.jsp"/>

<div class="container">
    <h2>
        Kết quả tìm kiếm cho:
        <span class="keyword">"${keyword}"</span>
    </h2>

    <c:if test="${not empty totalComics && totalComics > 0}">
        <p class="search-info">
            Tìm thấy <strong>${totalComics}</strong> truyện
            <c:if test="${not empty searchType && searchType != 'all'}">
                (tìm theo
                <c:choose>
                    <c:when test="${searchType == 'name'}">tên truyện</c:when>
                    <c:when test="${searchType == 'author'}">tác giả</c:when>
                    <c:when test="${searchType == 'publisher'}">nhà xuất bản</c:when>
                    <c:when test="${searchType == 'category'}">thể loại</c:when>
                </c:choose>)
            </c:if>
        </p>
    </c:if>

    <c:choose>
        <c:when test="${empty comics}">
            <p class="empty">Không tìm thấy truyện phù hợp.</p>
        </c:when>

        <c:otherwise>
            <div class="comic-list">
                <c:forEach var="comic" items="${comics}">
                    <div class="product-item">
                        <a href="${pageContext.request.contextPath}/comic-detail?id=${comic.id}">
                            <c:if test="${comic.hasFlashSale}">
                                <div class="flash-sale-badge">
                                    <i class="fas fa-bolt"></i> FLASH SALE
                                </div>
                            </c:if>

                            <img src="${comic.thumbnailUrl}"
                                 alt="${comic.nameComics}"
                                 onerror="this.src='${pageContext.request.contextPath}/img/no-image.png'">
                            <p class="product-name">${comic.nameComics}</p>
                        </a>
                        <c:choose>
                            <c:when test="${comic.hasFlashSale}">
                                <p class="product-price flash">
                                    <fmt:formatNumber value="${comic.flashSalePrice}" pattern="#,###"/> đ
                                </p>
                                <p class="original-price">
                                    <s><fmt:formatNumber value="${comic.price}" pattern="#,###"/> đ</s>
                                    <span class="discount-badge flash">
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

                        <p class="sold">
                            Đã bán: <strong>${comic.totalSold}</strong>
                        </p>
                    </div>
                </c:forEach>
            </div>
            <c:if test="${totalPages > 1}">
                <div class="pagination">
                    <c:choose>
                        <c:when test="${currentPage <= 1}">
                            <span class="disabled page-arrow">&#8249;</span>
                        </c:when>
                        <c:otherwise>
                            <a class="page-arrow" href="?keyword=${keyword}&searchType=${searchType}&page=${currentPage - 1}">&#8249;</a>
                        </c:otherwise>
                    </c:choose>

                        <%-- Các số trang --%>
                    <c:forEach begin="1" end="${totalPages}" var="i">
                        <c:choose>
                            <c:when test="${i == currentPage}">
                                <span class="active">${i}</span>
                            </c:when>
                            <c:otherwise>
                                <a href="?keyword=${keyword}&searchType=${searchType}&page=${i}">${i}</a>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>

                    <c:choose>
                        <c:when test="${currentPage >= totalPages}">
                            <span class="disabled page-arrow">&#8250;</span>
                        </c:when>
                        <c:otherwise>
                            <a class="page-arrow" href="?keyword=${keyword}&searchType=${searchType}&page=${currentPage + 1}">&#8250;</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>

        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/frontend/public/Footer.jsp"/>
</body>
</html>