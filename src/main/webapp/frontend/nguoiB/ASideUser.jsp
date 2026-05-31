<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="currentUser" value="${sessionScope.currentUser}"/>
<c:if test="${empty currentUser}">
    <c:redirect url="${pageContext.request.contextPath}/login"/>
</c:if>
<c:set var="currentURI" value="${pageContext.request.requestURI}"/>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>

<div class="profile-container">
    <div class="profile-header">
        <form id="avatarForm" action="${contextPath}/upload-avatar" method="post" enctype="multipart/form-data">
            <div class="avatar" style="cursor: pointer;">
                <c:choose>
                    <c:when test="${not empty currentUser.avatarUrl}">
                        <img src="${currentUser.avatarUrl}" alt="${currentUser.fullName}">
                    </c:when>
                    <c:when test="${currentUser.gender == 'female'}">
                        <img src="https://mayweddingstudio.vn/wp-content/uploads/anh-dai-dien-facebook-nu-9.webp"
                             alt="Avatar">
                    </c:when>
                    <c:otherwise>
                        <img src="https://cellphones.com.vn/sforum/wp-content/uploads/2023/10/avatar-trang-4.jpg"
                             alt="Avatar">
                    </c:otherwise>
                </c:choose>
            </div>
            <input type="file" id="avatarInput" name="avatar" accept="image/*" hidden>
        </form>
        <div class="user-info">
            <h2>${not empty currentUser.fullName ? currentUser.fullName : currentUser.username}</h2>
            <p class="membership">
                <c:choose>
                    <c:when test="${currentUser.membershipLevel == 'Silver'}">Thành viên Bạc</c:when>
                    <c:when test="${currentUser.membershipLevel == 'Gold'}">Thành viên Vàng</c:when>
                    <c:when test="${currentUser.membershipLevel == 'Platinum'}">Thành viên Kim Cương</c:when>
                    <c:otherwise>Thành viên Thường</c:otherwise>
                </c:choose>
            </p>
            <p class="C-point">C-Point tích lũy: <fmt:formatNumber value="${currentUser.points}" pattern="#,###"/></p>
        </div>
    </div>
    <div class="menu">
        <ul>
            <li class="${fn:contains(currentURI, 'updateUser') ? 'active' : ''}">
                <a href="${contextPath}/updateUser" data-page="updateUser">Thông tin tài khoản</a>
            </li>
            <li class="${fn:contains(currentURI, 'change-password') ? 'active' : ''}">
                <a href="${contextPath}/change-password" data-page="change-password">Đổi mật khẩu</a>
            </li>
            <li class="${fn:contains(currentURI, 'points') ? 'active' : ''}">
                <a href="${contextPath}/pointHistory" data-page="points">Ưu đãi C-Point</a>
            </li>
            <li class="${fn:contains(currentURI, 'order-history') ? 'active' : ''}">
                <a href="${contextPath}/order-history?filter=pending" data-page="order-history">Đơn hàng của tôi</a>
            </li>
            <li class="${fn:contains(currentURI, 'notifications') ? 'active' : ''}">
                <a href="${contextPath}/frontend/nguoiB/notifications.jsp" data-page="notifications">Thông báo</a>
            </li>
            <li class="${fn:contains(currentURI, 'user-voucher') ? 'active' : ''}">
                <a href="${contextPath}/user-voucher" data-page="user-voucher">Ví voucher</a>
            </li>
            <li class="${fn:contains(currentURI, 'wishlist') ? 'active' : ''}">
                <a href="${contextPath}/wishlist" data-page="wishlist">Sản phẩm yêu thích</a>
            </li>

            <li class="${fn:contains(currentURI, 'status-login') ? 'active' : ''}">
                <a href="${contextPath}/status-login" data-page="status-login">Trạng thái đăng nhập</a>
            </li>

        </ul>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const currentPath = window.location.pathname;
        const menuItems = document.querySelectorAll(".menu ul li");
        let hasActive = false;
        menuItems.forEach(li => {
            if (li.classList.contains('active')) {
                hasActive = true;
            }
        });
        if (!hasActive) {
            menuItems.forEach(li => {
                const link = li.querySelector('a');
                const dataPage = link.getAttribute('data-page');
                if (currentPath.includes(dataPage)) {
                    li.classList.add('active');
                }
            });
        }
    });
</script>
<script>
    document.addEventListener("DOMContentLoaded", function (){
        const avatar = document.querySelector(".avatar");
        const input = document.getElementById("avatarInput");
        const preview = document.getElementById("avatarPreview");
        const form = document.getElementById("avatarForm");
        avatar.addEventListener("click", () => {
            input.click();
        });
        input.addEventListener("change",()=>{
            const  file = input.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = function (e) {
                preview.src = e.target.result;
            };
            reader.readAsDataURL(file);
            form.submit();
        });

    });
</script>
