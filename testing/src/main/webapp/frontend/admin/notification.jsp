<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thông báo</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleNotification.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
        <div class="main-content">
            <%@ include file="HeaderAdmin.jsp" %>

            <div class="admin-content">
                <h2>Thông báo vi phạm</h2>
                <div class="noti-filter-tabs">
                    <a href="?filter=" class="${empty currentFilter ? 'active' : ''}">Tất cả</a>
                    <a href="?filter=login" class="${currentFilter == 'login' ? 'active' : ''}">
                        <i class="fa-solid fa-lock"></i> Đăng nhập sai
                    </a>
                    <a href="?filter=cancel" class="${currentFilter == 'cancel' ? 'active' : ''}">
                        <i class="fa-solid fa-ban"></i> Hủy đơn hàng
                    </a>
                </div>
                <div class="noti-full-list">
                    <c:choose>
                        <c:when test="${empty notifications}">
                            <div class="noti-empty">
                                <i class="fa-regular fa-bell-slash"></i>
                                <p>Không có thông báo vi phạm nào</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="noti" items="${notifications}">
                                <div class="noti-row ${noti.read ? '' : 'unread'}"
                                     onclick="openDetail(${noti.id}, `${fn:escapeXml(noti.message)}`)">

                                    <div class="noti-row-icon">
                                        <c:choose>
                                            <c:when test="${noti.type == 'USER_VIOLATION_LOGIN'}">
                                                <i class="fa-solid fa-lock" style="color:#e67e22;"></i>
                                            </c:when>
                                            <c:otherwise>
                                                <i class="fa-solid fa-ban" style="color:#e74c3c;"></i>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <div class="noti-row-content">
                                        <p>${fn:escapeXml(noti.message)}</p>
                                        <span class="noti-time">${noti.formattedCreatedAt}</span>
                                    </div>

                                    <c:if test="${!noti.read}">
                                        <span class="unread-dot"></span>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <c:if test="${totalPages > 1}">
                    <div class="pagination">
                        <c:forEach begin="1" end="${totalPages}" var="p">
                            <a href="?filter=${currentFilter}&page=${p}"
                               class="${p == currentPage ? 'active' : ''}">${p}</a>
                        </c:forEach>
                    </div>
                </c:if>
            </div>
        </div>
</div>

<div class="noti-modal-overlay" id="noti-modal" onclick="closeModal()">
    <div class="noti-modal-box" onclick="event.stopPropagation()">
        <div class="noti-modal-header">
            <i class="fa-solid fa-bell"></i>
            <h3>Chi tiết thông báo</h3>
            <button class="noti-modal-close" onclick="closeModal()">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>
        <div class="noti-modal-body">
            <p id="modal-message"></p>
        </div>
        <div class="noti-modal-footer">
            <button class="btn-close-modal" onclick="closeModal()">Đóng</button>
        </div>
    </div>
</div>

<script>
    function openDetail(id, message) {
        document.getElementById('modal-message').innerText = message;
        document.getElementById('noti-modal').classList.add('show');
        fetch('${pageContext.request.contextPath}/admin/mark-notification-read?id=' + id)
            .then(() => {
                document.querySelectorAll('.noti-row').forEach(row => {
                    if (row.getAttribute('onclick')?.includes('(' + id + ',')) {
                        row.classList.remove('unread');
                        const dot = row.querySelector('.unread-dot');
                        if (dot) dot.remove();
                    }
                });
            });
    }

    function closeModal() {
        document.getElementById('noti-modal').classList.remove('show');
    }

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeModal();
    });
</script>
</body>
</html>
