<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Banner" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Voucher</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/getVourcher.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>

<body>
<jsp:include page="/frontend/public/header.jsp"/>

<div class="container">
    <div class="voucher-group">

        <div class="voucher-time">
            <h1>Ưu đãi khung giờ ngập tràn</h1>
            <c:forEach var="v" items="${productVouchers}">
                <c:set var="usedPct" value="${v.usedCount * 100 / v.quantity}"/>
                <div class="voucher-card ${v.expired ? 'expired-card' : ''}">
                    <div class="voucher-side-bar product"></div>
                    <div class="voucher-card-inner">
                        <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm</span>
                            <c:choose>
                                <c:when test="${v.expired}">
                                    <span class="voucher-badge badge-status-expired">Hết hạn</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${v.singleUse}">
                                <span class="voucher-badge badge-single-use">1 lần/khách</span>
                            </c:if>
                        </div>

                        <div class="voucher-title">
                            <c:choose>
                                <c:when test="${v.discountType == 'percent'}">
                                    Giảm <fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/>%
                                </c:when>
                                <c:otherwise>
                                    Giảm <fmt:formatNumber value="${v.discountValue}" type="number"
                                                           maxFractionDigits="0"/>đ
                                </c:otherwise>
                            </c:choose>
                            - ${v.applyScope == 'all' ? 'Toàn sàn' : v.applyScope}
                        </div>

                        <div class="voucher-desc">
                            <c:if test="${v.minOrderAmount != null && v.minOrderAmount > 0}">
                                Đơn từ <fmt:formatNumber value="${v.minOrderAmount}" type="number"
                                                         maxFractionDigits="0"/>đ ·
                            </c:if>
                            Áp dụng: ${v.applyScope == 'all' ? 'Toàn bộ sản phẩm' : (v.applyScope)}
                        </div>

                        <div class="voucher-code-box">${v.code}</div>
                        <div class="voucher-qty-wrap">
                            <div class="voucher-qty-bar">
                                <div class="voucher-qty-fill ${v.quantity <= v.usedCount ? 'depleted' : ''}"
                                     style="width:${usedPct}%"></div>
                            </div>
                            <span class="voucher-qty-text">Còn ${v.quantity - v.usedCount}/${v.quantity} lượt</span>
                        </div>
                        <div class="voucher-footer">
                        <span class="voucher-expiry ${v.expired ? 'expired-text' : ''}">
                            HSD: ${v.endDateFormatted}</span>
                            <c:choose>
                                <c:when test="${v.expired || v.quantity <= v.usedCount}">
                                    <button class="voucher-save-btn" disabled>Hết</button>
                                </c:when>
                                <c:otherwise>
                                    <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="voucher-ship">
            <h1>Ưu đãi vận chuyển ngập tràn</h1>
            <c:forEach var="v" items="${shippingVouchers}">
                <c:set var="usedPct" value="${v.usedCount * 100 / v.quantity}"/>
                <div class="voucher-card ${v.expired ? 'expired-card' : ''}">
                    <div class="voucher-side-bar shipping"></div>
                    <div class="voucher-card-inner">
                        <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                            <c:choose>
                                <c:when test="${v.expired}">
                                    <span class="voucher-badge badge-status-expired">Hết hạn</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${v.singleUse}">
                                <span class="voucher-badge badge-single-use">1 lần/khách</span>
                            </c:if>
                        </div>

                        <div class="voucher-title">
                            <c:choose>
                                <c:when test="${v.discountType == 'percent'}">
                                    Giảm <fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/>% phí ship
                                </c:when>
                                <c:when test="${v.discountValue == 100}">Miễn phí vận chuyển</c:when>
                                <c:otherwise>
                                    Giảm <fmt:formatNumber value="${v.discountValue}" type="number"
                                                           maxFractionDigits="0"/>đ phí ship
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div class="voucher-desc">
                            <c:choose>
                                <c:when test="${v.minOrderAmount != null && v.minOrderAmount > 0}">
                                    Đơn từ <fmt:formatNumber value="${v.minOrderAmount}" type="number"
                                                             maxFractionDigits="0"/>đ
                                </c:when>
                                <c:otherwise>Không yêu cầu giá trị đơn</c:otherwise>
                            </c:choose>
                            · Áp dụng: ${v.applyScope == 'all' ? 'Toàn sàn' : v.applyScope}
                        </div>

                        <div class="voucher-code-box">${v.code}</div>
                        <div class="voucher-qty-wrap">
                            <div class="voucher-qty-bar">
                                <div class="voucher-qty-fill ${v.quantity <= v.usedCount ? 'depleted' : ''}"
                                     style="width:${usedPct}%"></div>
                            </div>
                            <span class="voucher-qty-text">Còn ${v.quantity - v.usedCount}/${v.quantity} lượt</span>
                        </div>

                        <div class="voucher-footer">
                        <span class="voucher-expiry ${v.expired ? 'expired-text' : ''}">
                            HSD: ${v.endDateFormatted}
                            <c:if test="${v.expired}"> · Đã hết hạn</c:if>
                        </span>
                            <c:choose>
                                <c:when test="${v.expired || v.quantity <= v.usedCount}">
                                    <button class="voucher-save-btn" disabled>Hết</button>
                                </c:when>
                                <c:otherwise>
                                    <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>

<%--        <div class="voucher-other">--%>
<%--            <h1>Ưu đãi ngoại lệ ngập tràn</h1>--%>

<%--            <div class="voucher-card">--%>
<%--                <div class="voucher-side-bar product"></div>--%>
<%--                <div class="voucher-card-inner">--%>
<%--                    <div class="voucher-badge-row">--%>
<%--                        <span class="voucher-badge badge-target-product">--%>
<%--                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm</span>--%>
<%--                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-title">Giảm 10.000đ - Trinh thám</div>--%>
<%--                    <div class="voucher-desc">Áp dụng truyện trinh thám · Không áp dụng combo</div>--%>
<%--                    <div class="voucher-code-box">KID10K2025</div>--%>
<%--                    <div class="voucher-qty-wrap">--%>
<%--                        <div class="voucher-qty-bar">--%>
<%--                            <div class="voucher-qty-fill" style="width:50%"></div>--%>
<%--                        </div>--%>
<%--                        <span class="voucher-qty-text">Còn 50/100 lượt</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-footer">--%>
<%--                        <span class="voucher-expiry">HSD: 30/11/2025</span>--%>
<%--                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            </div>--%>

<%--            <div class="voucher-card">--%>
<%--                <div class="voucher-side-bar product"></div>--%>
<%--                <div class="voucher-card-inner">--%>
<%--                    <div class="voucher-badge-row">--%>
<%--                        <span class="voucher-badge badge-target-product">--%>
<%--                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm--%>
<%--                        </span>--%>
<%--                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-title">Giảm 15.000đ - Mua 3 cuốn</div>--%>
<%--                    <div class="voucher-desc">Mua từ 3 cuốn trở lên · Không áp dụng truyện đang giảm</div>--%>
<%--                    <div class="voucher-code-box">BUY3GET15K</div>--%>
<%--                    <div class="voucher-qty-wrap">--%>
<%--                        <div class="voucher-qty-bar">--%>
<%--                            <div class="voucher-qty-fill" style="width:35%"></div>--%>
<%--                        </div>--%>
<%--                        <span class="voucher-qty-text">Còn 65/100 lượt</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-footer">--%>
<%--                        <span class="voucher-expiry">HSD: 15/12/2025</span>--%>
<%--                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            </div>--%>

<%--            <div class="voucher-card">--%>
<%--                <div class="voucher-side-bar product"></div>--%>
<%--                <div class="voucher-card-inner">--%>
<%--                    <div class="voucher-badge-row">--%>
<%--                        <span class="voucher-badge badge-target-product">--%>
<%--                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm--%>
<%--                        </span>--%>
<%--                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-title">Giảm 20.000đ - Hành động</div>--%>
<%--                    <div class="voucher-desc">Áp dụng truyện hành động · Không áp dụng bản đặc biệt</div>--%>
<%--                    <div class="voucher-code-box">BL20K2025</div>--%>
<%--                    <div class="voucher-qty-wrap">--%>
<%--                        <div class="voucher-qty-bar">--%>
<%--                            <div class="voucher-qty-fill" style="width:60%"></div>--%>
<%--                        </div>--%>
<%--                        <span class="voucher-qty-text">Còn 40/100 lượt</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-footer">--%>
<%--                        <span class="voucher-expiry">HSD: 31/12/2025</span>--%>
<%--                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            </div>--%>

<%--            <!-- Card: Cuối tuần -->--%>
<%--            <div class="voucher-card">--%>
<%--                <div class="voucher-side-bar product"></div>--%>
<%--                <div class="voucher-card-inner">--%>
<%--                    <div class="voucher-badge-row">--%>
<%--                        <span class="voucher-badge badge-target-product">--%>
<%--                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm--%>
<%--                        </span>--%>
<%--                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-title">Giảm 12.000đ - Cuối tuần</div>--%>
<%--                    <div class="voucher-desc">Áp dụng Thứ 7 &amp; Chủ Nhật · Không áp dụng Flash Sale</div>--%>
<%--                    <div class="voucher-code-box">WEEKEND12K</div>--%>
<%--                    <div class="voucher-qty-wrap">--%>
<%--                        <div class="voucher-qty-bar">--%>
<%--                            <div class="voucher-qty-fill" style="width:45%"></div>--%>
<%--                        </div>--%>
<%--                        <span class="voucher-qty-text">Còn 55/100 lượt</span>--%>
<%--                    </div>--%>
<%--                    <div class="voucher-footer">--%>
<%--                        <span class="voucher-expiry">HSD: 01/01/2026</span>--%>
<%--                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            </div>--%>
<%--        </div>--%>

    </div>
</div>
<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    function saveVoucher(btn) {
        if (btn.classList.contains('saved')) return;
        btn.textContent = 'Đã lưu';
        btn.classList.add('saved');
    }
</script>
</body>
</html>