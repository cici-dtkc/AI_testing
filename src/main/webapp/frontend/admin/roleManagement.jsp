<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản lý phân quyền</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleRoleMan.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>

<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Quản lý phân quyền</h2>
        <table class="permission-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Họ tên</th>
                <th>Email</th>
                <th>Role hiện tại</th>
                <th>Thao tác</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${users}" var="user">
                <tr>
                    <td>${user.id}</td>
                    <td>${user.fullName}</td>
                    <td>${user.email}</td>
                    <td><span class="role-badge">${user.roleName}</span></td>
                    <td>
                        <button
                                class="btn-assign"
                                data-id="${user.id}"
                                data-name="${user.fullName}"
                                data-role="${user.roleId}">
                            Gán quyền
                        </button>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>


<div id="rolePopup" class="popup-overlay">
    <div class="popup-box">
        <h3>Gán quyền người dùng</h3>
        <input type="hidden" id="userId">
        <p id="userName"></p>
        <select id="roleId">
            <c:forEach items="${roles}" var="role">
                <option value="${role.id}">${role.roleName}</option>
            </c:forEach>
        </select>
        <div class="popup-actions">
            <button id="saveRoleBtn">Lưu</button>
            <button onclick="closePopup()">Hủy</button>
        </div>
    </div>
</div>

<script>

    document.querySelectorAll(".btn-assign")
        .forEach(btn => {

            btn.addEventListener("click", function () {

                document.getElementById("userId").value =
                    this.dataset.id;

                document.getElementById("userName").innerText =
                    this.dataset.name;

                document.getElementById("roleId").value =
                    this.dataset.role;

                document.getElementById("rolePopup")
                    .style.display = "flex";
            });
        });

    function closePopup() {
        document.getElementById("rolePopup")
            .style.display = "none";
    }

    document.getElementById("saveRoleBtn")
        .addEventListener("click", function () {

            const userId =
                document.getElementById("userId").value;

            const roleId =
                document.getElementById("roleId").value;

            fetch("${pageContext.request.contextPath}/admin/permission-management", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },
                body:
                    "userId=" + userId +
                    "&roleId=" + roleId

            }).then(() => {
                location.reload();
            });
        });
</script>

</body>
</html>