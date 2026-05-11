<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Quản lý người dùng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleUserMan.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>

<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Quản lý người dùng</h2>
        <div class="user-management">
            <div class="search-filter-container">
                <form method="get" action="${pageContext.request.contextPath}/admin/user-management" id="searchForm">
                    <div class="search-box">
                        <input type="text"
                               id="searchInput"
                               name="search"
                               placeholder="Tìm kiếm người mua..."
                               value="${param.search != null ? param.search : ''}">
                        <i class="fas fa-magnifying-glass"></i>
                    </div>
                    <select id="levelFilter" name="level" class="level-filter">
                        <option value="">Tất cả cấp độ</option>
                        <option value="Normal" ${param.level == 'Normal' ? 'selected' : ''}>Normal</option>
                        <option value="Silver" ${param.level == 'Silver' ? 'selected' : ''}>Silver</option>
                        <option value="Gold" ${param.level == 'Gold' ? 'selected' : ''}>Gold</option>
                        <option value="Platinum" ${param.level == 'Platinum' ? 'selected' : ''}>Platinum</option>
                    </select>
                </form>
            </div>
            <table id="userTable" class="user-table">
                <thead>
                <tr>
                    <th>Tên khách hàng</th>
                    <th>Email</th>
                    <th>Cấp thành viên</th>
                    <th>Tổng chi tiêu</th>
                    <th>Điểm xu</th>
                    <th></th>
                </tr>
                </thead>
                <tbody id="userTableBody">
                <c:choose>
                    <c:when test="${not empty users}">
                        <c:forEach var="user" items="${users}">
                            <tr data-user-id="${user.id}" class="${!user.isActive ? 'locked-row' : ''}">
                                <td>
                                        ${user.fullName != null ? user.fullName : 'Chưa cập nhật'}
                                    <c:if test="${!user.isActive}">
                                        <br><span class="locked-badge">🔒 Tài khoản đã bị khóa</span>
                                    </c:if>
                                </td>
                                <td>${user.email}</td>
                                <td>
            <span class="badge level-${user.membershipLevel != null ? user.membershipLevel : 'Normal'}">
                    ${user.membershipLevel != null ? user.membershipLevel : 'Normal'}
            </span>
                                </td>
                                <td>
                                    <c:set var="formattedSpent">
                                        <fmt:formatNumber value="${user.totalSpent != null ? user.totalSpent : 0}"
                                                          pattern="#,###"
                                                          groupingUsed="true"/>
                                    </c:set>
                                        ${fn:replace(formattedSpent, ',', '.')}đ
                                </td>
                                <td>${user.points} xu</td>
                                <td class="action-cell">
                                    <c:choose>
                                        <c:when test="${!user.isActive}">
                                            <span style="color: #999; font-size: 14px;">—</span>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="kebab-menu">
                                                <button class="kebab-btn">⋮</button>
                                                <div class="menu-dropdown">
                                                    <a href="#" class="menu-item view-detail"
                                                       data-id="${user.id}"
                                                       data-name="${user.fullName != null ? user.fullName : 'Chưa cập nhật'}"
                                                       data-email="${user.email}"
                                                       data-phone="${user.phone != null ? user.phone : 'Chưa cập nhật'}"
                                                       data-level="${user.membershipLevel != null ? user.membershipLevel : 'Normal'}"
                                                       data-spent="${user.totalSpent != null ? user.totalSpent : 0}"
                                                       data-points="${user.points}"
                                                       data-created-at="${user.createdAt}">
                                                        Xem chi tiết
                                                    </a>
                                                    <a href="#" class="menu-item upgrade-user"
                                                       data-id="${user.id}"
                                                       data-name="${user.fullName != null ? user.fullName : 'Chưa cập nhật'}"
                                                       data-current-level="${user.membershipLevel != null ? user.membershipLevel : 'Normal'}">
                                                        Nâng cấp
                                                    </a>
                                                    <a href="#" class="menu-item permanent-lock"
                                                       data-id="${user.id}"
                                                       data-name="${user.fullName != null ? user.fullName : 'Chưa cập nhật'}">
                                                        Khóa vĩnh viễn
                                                    </a>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="6" style="text-align: center; padding: 20px;">
                                Không tìm thấy người dùng nào
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                <tr class="pagination-row">
                    <td colspan="10">
                        <div class="pagination" id="tablePagination"></div>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div id="upgradePopup" class="popup-overlay">
            <div class="popup-box">
                <h3>Nâng cấp thành viên</h3>
                <input type="hidden" id="upgradeUserId" value="">
                <p id="popupUserName"></p>
                <select id="upgradeSelect">
                    <option value="Normal">Thường (0%)</option>
                    <option value="Silver">Bạc (5%)</option>
                    <option value="Gold">Vàng (10%)</option>
                    <option value="Platinum">Kim cương (15%)</option>
                </select>
                <div class="popup-actions">
                    <button id="upgradeConfirm">Xác nhận</button>
                    <button id="upgradeCancel">Hủy</button>
                </div>
            </div>
        </div>
        <div id="detailPopup" class="popup-overlay">
            <div class="popup-box">
                <div class="popup-header">
                    <h3>Thông tin khách hàng</h3>
                </div>
                <div class="popup-body">
                    <div class="info-row">
                <span class="info-label">
                    Họ tên:
                </span>
                        <span class="info-value" id="detailName"></span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    </i>Email:
                </span>
                        <span class="info-value" id="detailEmail"></span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    Số điện thoại:
                </span>
                        <span class="info-value" id="detailPhone">Chưa cập nhật</span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    Cấp thành viên:
                </span>
                        <span class="info-value">
                    <span class="level-badge" id="detailLevelBadge"></span>
                </span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    </i>Tổng chi tiêu:
                </span>
                        <span class="info-value" id="detailSpent" style="color: #28a745; font-weight: 600;"></span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    Điểm tích lũy:
                </span>
                        <span class="info-value" id="detailPoints" style="color: #ffc107; font-weight: 600;"></span>
                    </div>
                    <div class="info-row">
                <span class="info-label">
                    </i>Ngày tham gia:
                </span>
                        <span class="info-value" id="detailCreatedAt">01/01/2024</span>
                    </div>
                </div>
                <div class="popup-footer">
                    <button class="btn-close" onclick="document.getElementById('detailPopup').style.display='none'">
                        Đóng
                    </button>
                </div>
            </div>
        </div>
        <div id="lockPopup" class="popup-overlay">
            <div class="popup-box">
                <h3>Xác nhận khóa tài khoản</h3>
                <input type="hidden" id="lockUserId" value="">
                <div class="popup-message" id="lockMessage"></div>
                <div class="popup-actions">
                    <button class="btn-cancel" onclick="document.getElementById('lockPopup').style.display='none'">Hủy
                    </button>
                    <button class="btn-danger" id="confirmLock">Khóa vĩnh viễn</button>
                </div>
            </div>
        </div>

        <div id="errorPopup" class="popup-overlay">
            <div class="popup-box">
                <div class="error-icon"><i class="fas fa-triangle-exclamation"></i></div>
                <h3>Không thể nâng cấp</h3>
                <p id="errorPopupMessage"></p>
                <div class="popup-actions">
                    <button class="btn-cancel" onclick="document.getElementById('errorPopup').style.display='none'">
                        Đóng
                    </button>
                </div>
            </div>
        </div>

        <div id="successPopup" class="popup-overlay">
            <div class="popup-box">
                <div class="success-icon"><i class="fas fa-circle-check"></i></div>
                <h3 id="successPopupMessage"></h3>
            </div>
        </div>

    </div>
</div>
<script>
    const BASE_URL = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/js/userManagement.js"></script>

</body>
</html>