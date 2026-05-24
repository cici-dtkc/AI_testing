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
    <style>
        #login-popup-overlay {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.45);
            z-index: 99999;
            align-items: center;
            justify-content: center;
        }
        .login-popup {
            background: #fff;
            border-radius: 12px;
            padding: 32px 28px;
            width: 320px;
            text-align: center;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
        }
        .login-popup-icon {
            font-size: 36px;
            color: #dc2e2e;
            margin-bottom: 12px;
        }
        .login-popup-title {
            font-size: 15px;
            font-weight: 600;
            color: #333;
            margin: 0 0 8px;
        }
        .login-popup-desc {
            font-size: 13px;
            color: #888;
            margin: 0 0 24px;
        }
        .login-popup-actions {
            display: flex;
            gap: 10px;
            justify-content: center;
        }
        .login-popup-close {
            padding: 9px 20px;
            border-radius: 20px;
            border: 1.5px solid #ddd;
            background: #fff;
            color: #555;
            font-weight: 600;
            cursor: pointer;
            font-size: 13px;
        }
        .login-popup-login {
            padding: 9px 20px;
            border-radius: 20px;
            background: #dc2e2e;
            color: #fff;
            font-weight: 600;
            font-size: 13px;
            text-decoration: none;
            display: inline-block;
        }
        .login-popup-login:hover {
            background: #b71c1c;
        }
    </style>
</head>

<body>
<jsp:include page="/frontend/public/header.jsp"/>

<div class="container">
    <div class="voucher-group">

        <div class="voucher-time">
            <h1>Ưu đãi khung giờ ngập tràn</h1>
            <c:forEach var="v" items="${productVouchers}">
                <c:set var="usedPct" value="${v.usedCount * 100 / v.quantity}"/>
                <div class="voucher-card ${v.expired ? 'expired-card' : ''}"
                     data-quantity="${v.quantity}">
                    <div class="voucher-side-bar product"></div>
                    <div class="voucher-card-inner">
                        <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm</span>
                            <c:if test="${v.expired}">
                                <span class="voucher-badge badge-status-expired">Hết hạn</span>
                            </c:if>
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
                            <span class="voucher-qty-text">Còn ${v.quantity - v.usedCount} lượt</span>
                        </div>
                        <div class="voucher-footer">
                        <span class="voucher-expiry ${v.expired ? 'expired-text' : ''}">
                            HSD: ${v.endDateFormatted}</span>
                            <c:choose>
                                <c:when test="${v.expired || v.quantity <= v.usedCount}">
                                    <button class="voucher-save-btn" disabled>Hết</button>
                                </c:when>
                                <c:otherwise>
                                    <button class="voucher-save-btn" data-id="${v.id}" data-code="${v.code}" onclick="saveVoucher(this)">Lưu</button>
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
                                <c:if test="${v.expired}">
                                    <span class="voucher-badge badge-status-expired">Hết hạn</span>
                                </c:if>
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
                            <span class="voucher-qty-text">Còn ${v.quantity - v.usedCount} lượt</span>
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
                                    <button class="voucher-save-btn" data-id="${v.id}" data-code="${v.code}" onclick="saveVoucher(this)">Lưu</button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>
</div>

<jsp:include page="/frontend/public/Footer.jsp"/>

<div id="login-popup-overlay" style="display: none;">
    <div class="login-popup">
        <i class="fa-solid fa-ticket login-popup-icon"></i>
        <p class="login-popup-title">Vui lòng đăng nhập</p>
        <p class="login-popup-desc">để lưu voucher về ví của bạn</p>
        <div class="login-popup-actions">
            <button class="login-popup-close" onclick="closeLoginPopup()">Đóng</button>
            <a class="login-popup-login" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
        </div>
    </div>
</div>
<script>
    function saveVoucher(btn) {
        if (!${not empty sessionScope.currentUser}) {
            showLoginPopup();
            return;
        }

        const promoId = btn.getAttribute('data-id');
        const code = btn.getAttribute('data-code');

        // btn.textContent = 'Đang lưu...';
        btn.disabled = true;
        fetch('${pageContext.request.contextPath}/user-voucher', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'promoId=' + promoId + '&code=' + encodeURIComponent(code)
        })
            .then(res => res.json())
            .then(data => {
                btn.disabled = false;
                if (data.success) {
                    showToast(data.message, 'success');
                } else {
                    showToast(data.message, 'warning');
                }
            })
            .catch(err => {
                console.error(err);
                btn.textContent = 'Lưu';
                btn.disabled = false;
                showToast('Lỗi kết nối!', 'error');
            });
    }
    function showLoginPopup() {
        document.getElementById('login-popup-overlay').style.display = 'flex';
    }
    function closeLoginPopup() {
        document.getElementById('login-popup-overlay').style.display = 'none';
    }

    function showToast(message, type) {
        const colors = {
            success: '#10b981',
            warning: '#f59e0b',
            error: '#f43f5e'
        };
        const icons = {
            success: '<i class="fa-solid fa-circle-check" style="color: #10b981; margin-right: 10px; font-size: 16px;"></i>',
            warning: '<i class="fa-solid fa-circle-exclamation" style="color: #f59e0b; margin-right: 10px; font-size: 16px;"></i>',
            error: '<i class="fa-solid fa-circle-xmark" style="color: #f43f5e; margin-right: 10px; font-size: 16px;"></i>'
        };
        const toast = document.createElement('div');
        toast.className = 'custom-toast';
        toast.innerHTML = icons[type] + '<span style="vertical-align: middle;">' + message + '</span>';
        toast.style.cssText = 'position: fixed; top: 24px; left: 50%; transform: translateX(-50%) translateY(-20px); ' +
            'background: #1e293b; color: #ffffff; padding: 14px 24px; border-radius: 10px; font-size: 14.5px; ' +
            'font-weight: 500; z-index: 99999; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.35), 0 8px 10px -6px rgba(0, 0, 0, 0.35); ' +
            'border-left: 4px solid ' + colors[type] + '; display: flex; align-items: center; opacity: 0; ' +
            'transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s ease;';
        
        document.body.appendChild(toast);
        
        // Trigger reflow
        toast.offsetHeight;
        
        // Animate in
        toast.style.transform = 'translateX(-50%) translateY(0)';
        toast.style.opacity = '1';
        
        // Animate out and remove
        setTimeout(() => {
            toast.style.transform = 'translateX(-50%) translateY(-20px)';
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }
</script>
</body>
</html>