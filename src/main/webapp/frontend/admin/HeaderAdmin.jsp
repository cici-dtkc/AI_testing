<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
<header class="admin-header">
    <div class="header-right">
        <div class="admin-notification">
            <a href="javascript:void(0)" onclick="toggleNotificationDropdown()">
                <i class="fa-regular fa-bell"></i>
                <c:if test="${unreadNotificationCount > 0}">
                    <span class="notification-badge">
                            ${unreadNotificationCount}
                    </span>
                </c:if>
            </a>

            <div id="notificationDropdown" class="notification-dropdown">
                <div class="notification-header">
                    <h4>Thông báo (${fn:length(notifications)})</h4>
                    <a href="${pageContext.request.contextPath}/admin/notifications">Xem tất cả</a>
                </div>

                <c:choose>
                    <c:when test="${empty notifications}">
                        <div class="notification-empty">
                            <i class="fa-regular fa-bell-slash"></i>
                            <p>Không có thông báo</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="notification-list">
                            <c:forEach var="noti" items="${notifications}">
                                <div class="notification-item ${noti.read ? 'read' : 'unread'}"
                                     onclick="openNotificationDetail(${noti.id}, `${fn:escapeXml(noti.message)}`)">

                                    <div class="noti-icon">
                                        <c:choose>
                                            <c:when test="${noti.type == 'USER_VIOLATION'}">
                                                <i class="fa-solid fa-triangle-exclamation" style="color: #e74c3c;"></i>
                                            </c:when>
                                            <c:otherwise>
                                                <i class="fa-solid fa-bell" style="color: #3498db;"></i>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <div class="noti-content">
                                        <p class="noti-message">
                                                ${fn:substring(noti.message, 0,
                                                        fn:length(noti.message) > 60 ? 60 : fn:length(noti.message))}
                                            <c:if test="${fn:length(noti.message) > 60}">...</c:if>
                                        </p>
                                        <span class="noti-time">${noti.formattedCreatedAt}</span>
                                    </div>

                                    <c:if test="${!noti.read}">
                                        <span class="unread-dot"></span>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="admin-profile">
            <a href="${pageContext.request.contextPath}/frontend/admin/profileAdmin.jsp">
                <img src="${pageContext.request.contextPath}/img/admin.png" class="admin-avatar" alt="Admin">
            </a>
            <span class="admin-name">Admin</span>
        </div>
        <button class="btn-logout" title="Đăng xuất">
            <a href="${pageContext.request.contextPath}/logout">
                <i class="fa-solid fa-right-from-bracket"></i>
            </a>
        </button>
    </div>
</header>

<div class="noti-modal-overlay" id="header-noti-modal" onclick="closeHeaderNotiModal()">
    <div class="noti-modal-box" onclick="event.stopPropagation()">
        <div class="noti-modal-header">
            <i class="fa-solid fa-bell"></i>
            <h3>Chi tiết thông báo</h3>
            <button class="noti-modal-close" onclick="closeHeaderNotiModal()">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>
        <div class="noti-modal-body">
            <p id="header-noti-modal-message"></p>
        </div>
        <div class="noti-modal-footer">
            <button class="btn-close-modal" onclick="closeHeaderNotiModal()">Đóng</button>
        </div>
    </div>
</div>

<script>
    function toggleNotificationDropdown() {
        const dropdown = document.getElementById("notificationDropdown");
        dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
    }

    window.addEventListener('click', function (e) {
        const dropdown = document.getElementById('notificationDropdown');
        const notificationDiv = document.querySelector('.admin-notification');

        if (!notificationDiv.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });

    function openNotificationDetail(id, message) {
        document.getElementById('header-noti-modal-message').innerText = message;
        document.getElementById('header-noti-modal').classList.add('show');
        document.getElementById('notificationDropdown').style.display = 'none';
        markAsRead(id, null);
    }

    function closeHeaderNotiModal() {
        document.getElementById('header-noti-modal').classList.remove('show');
        location.reload();
    }

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeHeaderNotiModal();
    });

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    function markAsRead(notificationId, callback) {
        fetch('${pageContext.request.contextPath}/admin/mark-notification-read?id=' + notificationId)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    if (callback) callback();
                } else {
                    console.error('Failed to mark as read:', data.error);
                }
            })
            .catch(err => {
                console.error('Error marking as read:', err);
            });
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    function markAllAsRead() {
        if (confirm('Đánh dấu tất cả thông báo đã đọc?')) {
            fetch('${pageContext.request.contextPath}/admin/mark-all-notifications-read')
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        console.log('Marked all notifications as read');
                        location.reload();
                    } else {
                        console.error('Failed to mark all as read:', data.error);
                    }
                })
                .catch(err => {
                    console.error('Error marking all as read:', err);
                });
        }
    }
</script>


