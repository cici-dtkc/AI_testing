<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Quản lý đơn hàng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleOrder.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<script>
    const BASE_URL = '${pageContext.request.contextPath}';
</script>

<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Quản lý đơn hàng</h2>
        <div class="order-tabs">
            <span class="tab-item active">Chờ xác nhận</span>
            <span class="tab-item">Chờ lấy hàng</span>
            <span class="tab-item">Đang giao</span>
            <span class="tab-item">Đã giao</span>
            <span class="tab-item">Trả hàng / Hoàn tiền</span>
            <span class="tab-item">Đơn bị hủy</span>
        </div>
        <div class="tab-content" id="tab-pending">
            <div class="order-controls">
                <div class="search-box">
                    <input type="text" id="pendingSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng..." class="search-input">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
                <button class="confirm-all-btn">Xác nhận tất cả</button>
            </div>
            <div class="table-scroll-wrapper">
                <div class="table-wrapper">
                    <table class="order-table">
                        <thead>
                        <tr>
                            <th>Mã đơn hàng</th>
                            <th>Khách hàng</th>
                            <th>Ngày đặt</th>
                            <th>Tổng tiền</th>
                            <th>Thanh toán</th>
                            <th>Sản phẩm</th>
                            <th>Địa chỉ giao hàng</th>
                            <th>Đơn vị vận chuyển</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody id="confirmTableBody">
                        <c:forEach items="${ordersByStatus['Pending']}" var="order" varStatus="status">
                            <tr>
                                <td>${order.orderCode}</td>
                                <td>${order.userName}</td>
                                <td><fmt:formatDate value="${order.orderDate}" pattern="dd/MM/yyyy"/></td>
                                <td>${order.formattedAmount}</td>
                                <td>${order.paymentMethodDisplay}</td>
                                <td class="product-cell">${order.productSummary}</td>
                                <td>${order.fullAddress}</td>
                                <td>${order.shippingProvider}</td>
                                <td>
                                    <button class="confirm-btn" data-order-id="${order.id}">Xác nhận</button>
                                    <button class="cancel-btn" data-order-id="${order.id}">Hủy</button>
                                </td>
                            </tr>
                        </c:forEach>
                        <tr class="pagination-row">
                            <td colspan="10">
                                <div class="pagination" id="tablePagination">
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="cancel-popup" style="display:none;">
                <div class="popup-box">
                    <h3>Lý do hủy đơn</h3>
                    <textarea placeholder="Nhập lý do..." rows="3"></textarea>
                    <div class="popup-actions">
                        <button class="close-popup">Đóng</button>
                        <button class="confirm-cancel">Xác nhận hủy</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="tab-content" id="tab-pickup" style="display:none;">
            <div class="order-controls">
                <div class="search-box">
                    <input type="text" id="pickupSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
            </div>
            <div class="table-wrapper">
                <table class="order-table">
                    <thead>
                    <tr>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Tổng tiền</th>
                        <th>Đơn vị vận chuyển</th>
                        <th>Địa chỉ giao hàng</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="pickupTableBody">
                    <c:forEach items="${ordersByStatus['AwaitingPickup']}" var="order">
                        <tr>
                            <td>${order.orderCode}</td>
                            <td>${order.userName}</td>
                            <td>${order.formattedAmount}</td>
                            <td>${order.shippingProvider}</td>
                            <td>${order.shippingAddress}</td>
                            <td>
                                <button class="ship-confirm-btn" data-order-id="${order.id}">
                                    Xác nhận đã giao cho ĐVVC
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr class="pagination-row-pickup">
                        <td colspan="10">
                            <div class="pagination" id="pickupPagination">
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="tab-content" id="tab-delivering" style="display:none;">
            <div class="order-controls delivering-controls">
                <div class="search-box">
                    <input type="text" id="deliverSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
            </div>
            <div class="table-wrapper">
                <table class="order-table">
                    <thead>
                    <tr>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Đơn vị vận chuyển</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="deliverTableBody">
                    <c:forEach items="${ordersByStatus['Shipping']}" var="order">
                        <tr>
                            <td>${order.orderCode}</td>
                            <td>${order.userName}</td>
                            <td>${order.shippingProvider}</td>
                            <td class="action-cell">
                                <button class="btn-de-detail" data-order-id="${order.id}">
                                    Xem chi tiết đơn
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr class="pagination-row-delivering">
                        <td colspan="10">
                            <div class="pagination" id="deliveringPagination">
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="tab-content" id="tab-delivered" style="display:none;">
            <div class="order-controls delivered-controls">
                <div class="search-box">
                    <input type="text" id="deliveredSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
            </div>
            <div class="table-wrapper">
                <table class="order-table">
                    <thead>
                    <tr>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Ngày giao thành công</th>
                        <th>Tổng tiền</th>
                        <th>Phương thức thanh toán</th>
                        <th>Mã giao dịch</th>
                        <th>Đánh giá</th>
                    </tr>
                    </thead>
                    <tbody id="deliveredTableBody">
                    <c:forEach items="${ordersByStatus['Completed']}" var="order">
                        <tr>
                            <td>${order.id}</td>
                            <td>${order.recipient_name}</td>
                            <td>${order.completed_at}</td>
                            <td>${order.formattedAmount}</td>
                            <td>${order.paymentMethodDisplay}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${order.hasTransaction}">
            <span style="font-family: monospace; font-size: 12px;
                         background: #f0f9ff; padding: 2px 6px;
                         border-radius: 4px; color: #0369a1;">
                    ${order.transactionId}
            </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color: #999;">-</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="stars">
                                <c:choose>
                                    <c:when test="${order.hasReview}">
                                        <c:forEach begin="1" end="${order.fullStars}">
                                            <i class="fa-solid fa-star"></i>
                                        </c:forEach>
                                        <c:if test="${order.hasHalfStar}">
                                            <i class="fa-solid fa-star-half-stroke"></i>
                                        </c:if>
                                        <c:forEach begin="1" end="${order.emptyStars}">
                                            <i class="fa-regular fa-star"></i>
                                        </c:forEach>
                                        <span class="rating-text">(${order.formattedRating})</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="no-rating">-</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr class="pagination-row-delivered">
                        <td colspan="10">
                            <div class="pagination" id="deliveredPagination">
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="tab-content" id="tab-return" style="display:none;">
            <div class="order-controls return-controls">
                <div class="search-box">
                    <input type="text" id="returnSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
            </div>
            <div class="table-wrapper">
                <table class="order-table">
                    <thead>
                    <tr>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Lý do hoàn trả</th>
                        <th>Số tiền hoàn</th>
                        <th>Tình trạng xử lý</th>
                        <th>Chi tiết</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="returnTableBody">
                    <c:choose>
                        <c:when test="${not empty ordersByStatus['Returns']}">
                            <c:forEach items="${ordersByStatus['Returns']}" var="returnOrder">
                                <tr data-status="${returnOrder.return_status}"
                                    data-return-id="${returnOrder.return_id}">
                                    <td>${returnOrder.order_code}</td>
                                    <td>${returnOrder.customer_name}</td>
                                    <td class="reason-cell">
                                        <c:choose>
                                            <c:when test="${fn:contains(returnOrder.reason, 'Lý do từ chối:')}">
                                                ${fn:substringBefore(returnOrder.reason, ' | ')}
                                            </c:when>
                                            <c:otherwise>
                                                ${returnOrder.reason}
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="amount-cell">${returnOrder.formatted_refund_amount}</td>
                                    <td>
                    <span class="status ${returnOrder.status_class}">
                            ${returnOrder.status_display}
                    </span>
                                    </td>
                                    <td>
                                        <button class="btn-detail"
                                                data-return-id="${returnOrder.return_id}"
                                                data-order-code="${returnOrder.order_code}"
                                                data-customer="${returnOrder.customer_name}"
                                                data-product="${returnOrder.product_name}"
                                                data-quantity="${returnOrder.quantity}"
                                                data-amount="${returnOrder.formatted_refund_amount}"
                                                data-date="${returnOrder.formatted_return_date}">
                                            Xem
                                        </button>
                                    </td>
                                    <td class="action-buttons">
                                        <c:choose>
                                            <c:when test="${returnOrder.return_status eq 'Pending'}">
                                                <button class="btn-refund"
                                                        data-return-id="${returnOrder.return_id}"
                                                        onclick="confirmRefund(this)">
                                                    Xác nhận hoàn tiền
                                                </button>
                                                <button class="btn-reject"
                                                        data-return-id="${returnOrder.return_id}"
                                                        data-order-code="${returnOrder.order_code}"
                                                        data-customer="${returnOrder.customer_name}"
                                                        onclick="openRejectPopup(this)">
                                                    Từ chối
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="action-buttons">
                                                    <span style="color: #999;">-</span>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr class="no-result-message">
                                <td colspan="7" style="text-align: center; padding: 30px; color: #999;">
                                    <i class="fas fa-inbox"
                                       style="font-size: 48px; margin-bottom: 10px; display: block;"></i>
                                    Chưa có yêu cầu hoàn trả nào
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    <tr class="pagination-row-return">
                        <td colspan="7">
                            <div class="pagination" id="returnPagination"></div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="popup-desk-overlay" id="rejectPopup">
            <div class="popup-desk-box">
                <h3>Từ chối yêu cầu hoàn tiền</h3>
                <div class="popup-desk-content">
                    <p><strong>Mã đơn:</strong> <span id="rejectOrderId"></span></p>
                    <p><strong>Khách hàng:</strong> <span id="rejectCustomer"></span></p>
                    <label>Lý do từ chối <span style="color: red;">*</span></label>
                    <textarea id="rejectReason"
                              placeholder="Nhập lý do chi tiết để gửi thông báo cho khách hàng..."
                              rows="4"></textarea>
                </div>
                <div class="popup-desk-actions">
                    <button type="button" class="btn-cancel"
                            onclick="closeRejectPopup()">Hủy
                    </button>
                    <button type="button" class="btn-save" onclick="confirmReject()">Xác nhận từ chối</button>
                </div>
            </div>
        </div>
        <div class="return-popup" id="returnPopup" style="display:none;">
            <div class="popup-header">
                <h3>Chi tiết đơn hoàn</h3>
                <span class="popup-close-btn" id="closePopup" onclick="closeReturnPopup()">&times;</span>
            </div>
            <div class="popup-content">
                <div class="popup-row">
                    <span class="label">Mã đơn hàng:</span>
                    <span class="value" id="detailOrderCode">-</span>
                </div>
                <div class="popup-row">
                    <span class="label">Khách hàng:</span>
                    <span class="value" id="detailCustomer">-</span>
                </div>
                <div class="popup-row">
                    <span class="label">Sản phẩm:</span>
                    <span class="value" id="detailProduct">-</span>
                </div>
                <div class="popup-row">
                    <span class="label">Số lượng:</span>
                    <span class="value" id="detailQuantity">-</span>
                </div>
                <div class="popup-row">
                    <span class="label">Lý do hoàn:</span>
                    <span class="value" id="detailReason">-</span>
                </div>
                <div class="popup-row" id="rejectReasonRow" style="display:none;">
                    <span class="label">Lý do từ chối:</span>
                    <span class="value" id="detailRejectReason"
                          style="color:#dc2626; font-weight:500;">
                    </span>
                </div>
                <div class="popup-row">
                    <span class="label">Minh chứng:</span>
                    <div class="value proof-images-container" id="detailProofImages">
                        <span style="color: #999;">Đang tải...</span>
                    </div>
                </div>
                <div class="popup-row">
                    <span class="label">Số tiền hoàn:</span>
                    <span class="value" id="detailAmount">-</span>
                </div>
                <div class="popup-row">
                    <span class="label">Ngày yêu cầu hoàn trả:</span>
                    <span class="value" id="detailDate">-</span>
                </div>
            </div>
            <div class="popup-footer">
                <button class="popup-close" onclick="closeReturnPopup()">Đóng</button>
            </div>
        </div>
        <div class="tab-content" id="tab-cancelled" style="display:none;">
            <div class="order-controls cancelled-controls">
                <div class="search-box">
                    <input type="text" id="cancelledSearch" class="search-input"
                           placeholder="Tìm kiếm theo mã đơn hoặc tên khách hàng...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
            </div>
            <div class="table-wrapper">
                <table class="order-table">
                    <thead>
                    <tr>
                        <th>Mã đơn hàng</th>
                        <th>Khách hàng</th>
                        <th>Ngày đặt</th>
                        <th>Lý do hủy</th>
                        <th>Người hủy</th>
                        <th>Ngày hủy</th>
                    </tr>
                    </thead>
                    <tbody id="cancelledTableBody">
                    <c:choose>
                        <c:when test="${not empty ordersByStatus['Cancelled']}">
                            <c:forEach items="${ordersByStatus['Cancelled']}" var="order">
                                <tr>
                                    <td>${order.orderCode}</td>
                                    <td>${order.userName}</td>
                                    <td><fmt:formatDate value="${order.orderDate}" pattern="dd/MM/yyyy"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty order.cancellationReason}">
                                                ${order.cancellationReason}
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: #999; font-style: italic;">Không có lý do</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty order.cancelledBy}">
                                                <c:choose>
                                                    <c:when test="${order.cancelledBy eq 'Admin'}">
                                                        <span style="color: #dc2626; font-weight: 500;">
                                                            <i class="fas fa-user-shield"></i> Admin
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${order.cancelledBy eq 'Customer'}">
                                                        <span style="color: #2563eb; font-weight: 500;">
                                                            <i class="fas fa-user"></i> Khách hàng
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${order.cancelledBy}
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: #999;">N/A</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><fmt:formatDate value="${order.cancelledAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr class="no-result-message">
                                <td colspan="6" style="text-align: center; padding: 30px; color: #999;">
                                    <i class="fas fa-inbox"
                                       style="font-size: 48px; margin-bottom: 10px; display: block;"></i>
                                    Chưa có đơn hàng nào bị hủy
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    <tr class="pagination-row-cancelled">
                        <td colspan="10">
                            <div class="pagination" id="cancelledPagination">
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="toast-notification" id="toastMsg">
            <i class="fas fa-check-circle" id="toastIcon"></i>
            <span id="toastText"></span>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/orderManagement.js"></script>


<script>
    const tabs = document.querySelectorAll(".tab-item");
    const tabContents = document.querySelectorAll(".tab-content");

    function showTab(index, saveState = true) {
        tabs.forEach(t => t.classList.remove("active"));
        if (tabs[index]) tabs[index].classList.add("active");
        tabContents.forEach((c, i) => {
            c.style.display = (i === index) ? "" : "none";
        });
        if (saveState && typeof saveCurrentTab === 'function') {
            saveCurrentTab(index);
        }
        if (typeof loadTabIfNeeded === 'function') {
            loadTabIfNeeded(index);
        }
    }

    tabs.forEach((tab, index) => {
        tab.addEventListener("click", () => {
            showTab(index, true);
        });
    });

    document.addEventListener('DOMContentLoaded', function () {
        let tabToShow = 0;
        if (typeof getSavedTab === 'function') {
            const savedTab = getSavedTab();
            if (savedTab !== null && savedTab >= 0 && savedTab < tabs.length) {
                tabToShow = savedTab;
            }
        }
        showTab(tabToShow, false);
    });
</script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const current = window.location.pathname.split("/").pop();
        const links = document.querySelectorAll(".sidebar li a");

        links.forEach(link => {
            const linkPage = link.getAttribute("href");

            if (linkPage === current) {
                link.classList.add("active");
            }
        });
    });
</script>

<script>
    document.querySelectorAll('.confirm-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const orderId = this.dataset.orderId;

            fetch(BASE_URL + '/admin/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'action=confirm&orderId=' + orderId
            })
                .then(async response => {
                    const text = await response.text();

                    try {
                        return JSON.parse(text);
                    } catch (e) {
                        throw new Error("Server response không hợp lệ");
                    }
                })
                .then(data => {
                    if (data.success) {
                        showToast(data.message || 'Xác nhận đơn hàng thành công!');
                        setTimeout(() => location.reload(), 1500);
                    } else {
                        showToast('Lỗi: ' + data.message, 'error');
                    }
                })
                .catch(error => showToast('Lỗi kết nối: ' + error, 'error'));
        });
    });

    // Hủy đơn hàng
    let currentCancelOrderId = null;

    document.querySelectorAll('.cancel-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            currentCancelOrderId = this.dataset.orderId;
            document.querySelector('.cancel-popup').style.display = 'flex';
            document.querySelector('.cancel-popup textarea').value = ''; // Reset textarea
        });
    });

    // Đóng popup
    document.querySelector('.close-popup').addEventListener('click', function () {
        document.querySelector('.cancel-popup').style.display = 'none';
        currentCancelOrderId = null;
    });

    // Xác nhận hủy
    document.querySelector('.confirm-cancel').addEventListener('click', function () {
        if (!currentCancelOrderId) {
            showToast('Không xác định được đơn hàng cần hủy', 'error');
            return;
        }

        const reason = document.querySelector('.cancel-popup textarea').value.trim();

        if (!reason) {
            showToast('Vui lòng nhập lý do hủy', 'error');
            return;
        }

        // Gửi request với lý do hủy
        fetch(BASE_URL + '/admin/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'action=cancel&orderId=' + currentCancelOrderId + '&reason=' + encodeURIComponent(reason)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    document.querySelector('.cancel-popup').style.display = 'none';
                    showToast(data.message || 'Hủy đơn hàng thành công!');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast('Lỗi: ' + data.message, 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showToast('Lỗi kết nối: ' + error, 'error');
            });
    });
</script>

<script>
    document.querySelector('.confirm-all-btn').addEventListener('click', function () {
        this.disabled = true;
        this.textContent = 'Đang xử lý...';
        const btn = this;

        // Gửi request xác nhận tất cả
        fetch(BASE_URL + '/admin/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: 'action=confirmAll'
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Đã xác nhận thành công tất cả đơn hàng!');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast('Lỗi: ' + (data.message || data.error), 'error');
                    btn.disabled = false;
                    btn.textContent = 'Xác nhận tất cả';
                }
            })
            .catch(error => {
                showToast('Lỗi kết nối: ' + error, 'error');
                btn.disabled = false;
                btn.textContent = 'Xác nhận tất cả';
            });
    });
</script>

<script>
    // Thêm effect khi scroll
    document.addEventListener('DOMContentLoaded', function () {
        const scrollWrapper = document.querySelector('.table-scroll-wrapper');
        if (scrollWrapper) {
            scrollWrapper.addEventListener('scroll', function () {
                if (this.scrollLeft > 0) {
                    this.classList.add('scrolled');
                } else {
                    this.classList.remove('scrolled');
                }
            });
        }
    });


    const wrapper = document.querySelector('#tab-pending .table-scroll-wrapper');
    const table = document.querySelector('#tab-pending .order-table');

    if (!wrapper) {
        console.error(' Không tìm thấy');
    } else {
        console.log('Tìm thấy wrapper');


        // Kiểm tra có overflow không
        const hasOverflow = wrapper.scrollWidth > wrapper.clientWidth;

        if (!hasOverflow) {
            console.warn('TABLE CHƯA ĐỦ RỘNG ĐỂ SCROLL!');
            console.log('Table cần rộng hơn:', wrapper.clientWidth + 'px');
            console.log('Table hiện tại:', table.offsetWidth + 'px');
        }
        const wrapperStyle = window.getComputedStyle(wrapper);
        const tableStyle = window.getComputedStyle(table);
        const columns = table.querySelectorAll('thead th').length;
        let totalWidth = 0;
        table.querySelectorAll('thead th').forEach((th, i) => {
            const w = th.offsetWidth;
            totalWidth += w;
            console.log(`  Cột ${i + 1}: ${w}px - ${th.textContent.trim()}`);
        });
        console.log('');
        if (!hasOverflow) {
            const suggestedWidth = wrapper.clientWidth + 500;
        }
    }
</script>

<script>
    let currentRejectReturnId = null;
    window.confirmRefund = function (btn) {
        const returnId = btn.getAttribute('data-return-id');
        if (!confirm('Xác nhận hoàn tiền cho đơn này?')) return;
        fetch(BASE_URL + '/admin/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'action=confirmRefund&returnId=' + returnId
        })
            .then(response => response.json())
            .then(data => {
                console.log('Response:', data);
                if (data.success) {
                    alert(data.message);
                    location.reload();
                } else {
                    alert('Lỗi: ' + (data.message || data.error));
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Lỗi kết nối: ' + error);
            });
    };

    window.openRejectPopup = function (btn) {
        currentRejectReturnId = btn.getAttribute('data-return-id');
        const orderCode = btn.getAttribute('data-order-code');
        const customer = btn.getAttribute('data-customer');

        document.getElementById('rejectOrderId').textContent = orderCode;
        document.getElementById('rejectCustomer').textContent = customer;
        document.getElementById('rejectReason').value = '';

        document.getElementById('rejectPopup').style.display = 'flex';
    };

    window.closeRejectPopup = function () {
        document.getElementById('rejectPopup').style.display = 'none';
        currentRejectReturnId = null;
    };


    window.confirmReject = function () {
        const reason = document.getElementById('rejectReason').value.trim();
        if (!reason) {
            alert('Vui lòng nhập lý do từ chối!');
            return;
        }
        fetch(BASE_URL + '/admin/orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'action=rejectRefund&returnId=' + currentRejectReturnId +
                '&reason=' + encodeURIComponent(reason)
        })
            .then(response => response.json())
            .then(data => {
                console.log('Response:', data);
                if (data.success) {
                    alert(data.message);
                    closeRejectPopup();
                    location.reload();
                } else {
                    alert('Lỗi: ' + (data.message || data.error));
                }
            })
            .catch(error => {
                console.error(' Error:', error);
                alert('Lỗi kết nối: ' + error);
            });
    };
    document.addEventListener("click", function (e) {
        if (e.target.classList.contains("btn-detail")) {
            const btn = e.target;
            const returnId = btn.getAttribute('data-return-id');

            console.log('Loading return detail for ID:', returnId);
            fetch(BASE_URL + '/admin/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'action=getReturnDetail&returnId=' + returnId
            })
                .then(response => response.json())
                .then(data => {
                    console.log('Return detail response:', data);

                    if (data.success && data.return) {
                        const returnDetail = data.return;

                        // Điền thông tin vào popup
                        document.getElementById('detailOrderCode').textContent = returnDetail.order_code || '-';
                        document.getElementById('detailCustomer').textContent = returnDetail.customer_name || '-';
                        document.getElementById('detailProduct').textContent = returnDetail.product_name || '-';
                        document.getElementById('detailQuantity').textContent = returnDetail.quantity || '-';
                        document.getElementById('detailReason').textContent = returnDetail.reason || '-';
                        document.getElementById('detailAmount').textContent = returnDetail.formatted_refund_amount || '-';
                        document.getElementById('detailDate').textContent = returnDetail.formatted_return_date || '-';

                        // Lý do từ chối
                        const rejectRow = document.getElementById('rejectReasonRow');
                        const rejectReasonEl = document.getElementById('detailRejectReason');

                        rejectRow.style.display = 'none';
                        rejectReasonEl.textContent = '';

                        if (returnDetail.return_status === 'Rejected'
                            && returnDetail.reason
                            && returnDetail.reason.includes('Lý do từ chối:')) {

                            const parts = returnDetail.reason.split('Lý do từ chối:');
                            if (parts.length > 1) {
                                rejectReasonEl.textContent = parts[1].trim();
                                rejectRow.style.display = 'flex';
                            }
                        } else {
                            rejectRow.style.display = 'none';
                            rejectReasonEl.textContent = '';
                        }
                        const proofContainer = document.getElementById('detailProofImages');
                        proofContainer.innerHTML = ''; // Xóa nội dung cũ

                        if (returnDetail.proof_images && returnDetail.proof_images.length > 0) {
                            console.log('📸 Found ' + returnDetail.proof_images.length + ' proof images');

                            returnDetail.proof_images.forEach((image, index) => {
                                console.log('Image ' + (index + 1) + ':', image);
                                let imageUrl = image.urlImg;
                                if (!imageUrl.startsWith('http') && !imageUrl.startsWith(BASE_URL)) {
                                    if (!imageUrl.startsWith('/')) {
                                        imageUrl = '/' + imageUrl;
                                    }
                                    imageUrl = BASE_URL + imageUrl;
                                }
                                console.log('Final URL:', imageUrl);
                                const imgWrapper = document.createElement('div');
                                imgWrapper.className = 'proof-image-item';
                                const img = document.createElement('img');
                                img.src = imageUrl;
                                img.alt = 'Minh chứng ' + (index + 1);
                                img.onerror = function () {
                                    console.error('Failed to load:', this.src);
                                    this.parentElement.innerHTML =
                                        '<div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; background: #f3f4f6; color: #9ca3af;">' +
                                        '<i class="fas fa-image" style="font-size: 48px; margin-bottom: 8px;"></i>' +
                                        '<span>Không thể tải ảnh</span>' +
                                        '</div>';
                                };
                                img.onclick = function () {
                                    openImageViewer(imageUrl);
                                };
                                imgWrapper.appendChild(img);
                                proofContainer.appendChild(imgWrapper);
                            });
                        } else {
                            console.log('No proof images found');
                            proofContainer.innerHTML = '<span style="color: #999;">Không có ảnh minh chứng</span>';
                        }

                        // Hiển thị popup
                        document.getElementById("returnPopup").style.display = "block";
                    } else {
                        alert('Không thể tải chi tiết đơn hoàn: ' + (data.message || data.error));
                    }
                })
                .catch(error => {
                    console.error('Error loading return detail:', error);
                    alert('Lỗi kết nối: ' + error);
                });
        }
    });
    window.handleImageError = function (img) {
        console.error('Failed to load image:', img.src);
        img.parentElement.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; background: #f3f4f6; color: #9ca3af;">
            <i class="fas fa-image" style="font-size: 48px; margin-bottom: 8px;"></i>
            <span>Không thể tải ảnh</span>
        </div>
    `;
    };

    window.openImageViewer = function (imageUrl) {
        console.log(' Opening image viewer for:', imageUrl);

        const viewer = document.createElement('div');
        viewer.className = 'image-viewer-overlay';
        viewer.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.9);
        z-index: 10000;
        display: flex;
        align-items: center;
        justify-content: center;
    `;
        const content = document.createElement('div');
        content.className = 'image-viewer-content';
        content.style.cssText = `
        position: relative;
        max-width: 90%;
        max-height: 90%;
    `;

        const closeBtn = document.createElement('span');
        closeBtn.className = 'close-viewer';
        closeBtn.innerHTML = '&times;';
        closeBtn.style.cssText = `
        position: absolute;
        top: -40px;
        right: 0;
        font-size: 40px;
        color: white;
        cursor: pointer;
        font-weight: 300;
        z-index: 10001;
    `;
        closeBtn.onclick = function () {
            viewer.remove();
        };
        closeBtn.onmouseover = function () {
            this.style.color = '#ff4444';
        };
        closeBtn.onmouseout = function () {
            this.style.color = 'white';
        };

        const img = document.createElement('img');
        img.src = imageUrl;
        img.alt = 'Ảnh minh chứng';
        img.style.cssText = `
        max-width: 100%;
        max-height: 90vh;
        object-fit: contain;
        display: block;
    `;

        img.onload = function () {
            console.log('Image viewer loaded successfully');
        };

        img.onerror = function () {
            content.innerHTML = `
            <div style="text-align: center; color: white; padding: 40px;">
                <i class="fas fa-exclamation-triangle" style="font-size: 64px; margin-bottom: 20px; color: #ff4444;"></i>
                <h3 style="margin-bottom: 10px;">Không thể tải ảnh này</h3>
                <p style="color: #ccc; font-size: 14px; word-break: break-all; max-width: 600px;">${imageUrl}</p>
                <button onclick="this.closest('.image-viewer-overlay').remove()"
                        style="margin-top: 20px; padding: 10px 20px; background: #ff4444; color: white; border: none; border-radius: 4px; cursor: pointer;">
                    Đóng
                </button>
            </div>
        `;
        };
        content.appendChild(closeBtn);
        content.appendChild(img);
        viewer.appendChild(content);
        document.body.appendChild(viewer);
        viewer.addEventListener('click', function (e) {
            if (e.target === viewer) {
                viewer.remove();
            }
        });
        const closeOnEsc = function (e) {
            if (e.key === 'Escape' && document.body.contains(viewer)) {
                viewer.remove();
                document.removeEventListener('keydown', closeOnEsc);
            }
        };
        document.addEventListener('keydown', closeOnEsc);
    };

    window.closeReturnPopup = function () {
        document.getElementById("returnPopup").style.display = "none";
    };

    window.addEventListener('click', function (event) {
        const returnPopup = document.getElementById('returnPopup');
        if (event.target === returnPopup) {
            closeReturnPopup();
        }
    });
    let searchTimeout = null;
    document.getElementById('returnSearch').addEventListener('input', function (e) {
        const keyword = e.target.value.trim();
        // Debounce: Chờ 500ms sau khi user ngừng gõ
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function () {

            fetch(BASE_URL + '/admin/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'action=searchReturns&keyword=' + encodeURIComponent(keyword)
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        updateReturnTable(data.returns);
                    } else {
                        console.error('Search failed:', data.error);
                    }
                })
                .catch(error => {
                    console.error('Search error:', error);
                });
        }, 500);
    });

    /**
     * Cập nhật bảng hiển thị kết quả tìm kiếm
     */
    function updateReturnTable(returns) {
        const tbody = document.getElementById('returnTableBody');
        const rows = tbody.querySelectorAll('tr:not(.pagination-row-return)');
        rows.forEach(row => row.remove());
        if (returns.length === 0) {
            const noResultRow = document.createElement('tr');
            noResultRow.className = 'no-result-message';
            noResultRow.innerHTML =
                '<td colspan="7" style="text-align: center; padding: 30px; color: #999;">' +
                '<i class="fas fa-search" style="font-size: 48px; margin-bottom: 10px; display: block;"></i>' +
                'Không tìm thấy kết quả' +
                '</td>';
            tbody.insertBefore(noResultRow, tbody.querySelector('.pagination-row-return'));
            return;
        }
        returns.forEach(returnOrder => {
            const row = document.createElement('tr');
            row.setAttribute('data-status', returnOrder.return_status);
            row.setAttribute('data-return-id', returnOrder.return_id);

            const isPending = returnOrder.return_status === 'Pending';
            let html = '<td>' + returnOrder.order_code + '</td>';
            html += '<td>' + returnOrder.customer_name + '</td>';
            let displayReason = returnOrder.reason || '';
            if (displayReason.includes('Lý do từ chối:')) {
                displayReason = displayReason.split(' | ')[0];
            }
            html += '<td class="reason-cell" title="' + displayReason + '">'
                + displayReason +
                '</td>';
            html += '<td class="amount-cell">' + returnOrder.formatted_refund_amount + '</td>';
            html += '<td><span class="status ' + returnOrder.status_class + '">' + returnOrder.status_display + '</span></td>';
            // Nút Xem chi tiết
            html += '<td><button class="btn-detail" ' +
                'data-return-id="' + returnOrder.return_id + '" ' +
                'data-order-code="' + returnOrder.order_code + '" ' +
                'data-customer="' + returnOrder.customer_name + '" ' +
                'data-product="' + returnOrder.product_name + '" ' +
                'data-quantity="' + returnOrder.quantity + '" ' +
                'data-reason="' + returnOrder.reason + '" ' +
                'data-amount="' + returnOrder.formatted_refund_amount + '" ' +
                'data-date="' + (returnOrder.formatted_return_date || '') + '">' +
                'Xem</button></td>';
            html += '<td class="action-buttons">';
            if (isPending) {
                html += '<button class="btn-refund" data-return-id="' + returnOrder.return_id + '" ' +
                    'onclick="confirmRefund(this)">Xác nhận hoàn tiền</button>';
                html += '<button class="btn-reject" data-return-id="' + returnOrder.return_id + '" ' +
                    'data-order-code="' + returnOrder.order_code + '" ' +
                    'data-customer="' + returnOrder.customer_name + '" ' +
                    'onclick="openRejectPopup(this)">Từ chối</button>';
            } else {
                html += '<span style="color: #999;">-</span>';
            }
            html += '</td>';
            row.innerHTML = html;
            tbody.insertBefore(row, tbody.querySelector('.pagination-row-return'));
        });
    }

    window.addEventListener('click', function (event) {
        const rejectPopup = document.getElementById('rejectPopup');
        if (event.target === rejectPopup) {
            closeRejectPopup();
        }
        const returnPopup = document.getElementById('returnPopup');
        if (event.target === returnPopup) {
            closeReturnPopup();
        }
    });
</script>

<script>
    function showToast(message, type = 'success') {
        const toast = document.getElementById('toastMsg');
        const icon = document.getElementById('toastIcon');
        const text = document.getElementById('toastText');

        text.textContent = message;
        toast.className = 'toast-notification show' + (type === 'error' ? ' error' : '');
        icon.className = type === 'error' ? 'fas fa-times-circle' : 'fas fa-check-circle';

        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }
</script>
</body>
</html>