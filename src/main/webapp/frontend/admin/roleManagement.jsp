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
                <th></th>
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

                        <button class="btn-override"
                                data-id="${user.id}"
                                data-name="${user.fullName}">
                            Override Permission
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

<div id="overridePopup" class="popup-overlay">
    <div class="popup-box" style="min-width: 550px">
        <h3>Override Permission — <span id="overrideUserName"></span></h3>
        <input type="hidden" id="overrideUserId">

        <table class="permission-table" style="margin-top: 12px">
            <thead>
            <tr>
                <th>Permission</th>
                <th>Mô tả</th>
                <th>Từ Role</th>
                <th>Override</th>
                <th></th>
            </tr>
            </thead>
            <tbody id="overrideTableBody">
            <tr>
                <td colspan="5">Đang tải...</td>
            </tr>
            </tbody>
        </table>

        <div class="popup-actions" style="margin-top: 12px">
            <button id="resetAllBtn" style="background:#e74c3c; color:white">
                Reset tất cả
            </button>
            <button onclick="closeOverridePopup()">Đóng</button>
        </div>
    </div>
</div>

<div id="toast" class="toast"></div>
<script>
    function showToast(message, type) {
        type = type || "error";
        const toast = document.getElementById("toast");
        toast.textContent = message;
        toast.className = "toast";
        void toast.offsetWidth;
        toast.className = "toast " + type + " show";
        setTimeout(() => { toast.classList.remove("show"); }, 3000);
    }
</script>

<script>

    document.querySelectorAll(".btn-assign").forEach(btn => {
        btn.addEventListener("click", function () {
            document.getElementById("userId").value = this.dataset.id;
            document.getElementById("userName").innerText = this.dataset.name;
            document.getElementById("roleId").value = this.dataset.role;
            document.getElementById("rolePopup").style.display = "flex";
        });
    });

    function closePopup() {
        document.getElementById("rolePopup").style.display = "none";
    }

    document.getElementById("saveRoleBtn").addEventListener("click", function () {
        const userId = document.getElementById("userId").value;
        const roleId = document.getElementById("roleId").value;
        fetch("${pageContext.request.contextPath}/admin/permission-management", {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: "userId=" + userId + "&roleId=" + roleId
        }).then(response => {
            if (response.status === 403) {
                return response.json().then(data => {
                    showToast(data.error, "error");
                });
            } else {
                showToast("Cập nhật quyền thành công!", "success");
                setTimeout(() => location.reload(), 1000);
            }
        });
    });

        document.querySelectorAll(".btn-override").forEach(btn => {
            btn.addEventListener("click", function () {
                const userId = this.dataset.id;
                document.getElementById("overrideUserId").value = userId;
                document.getElementById("overrideUserName").innerText = this.dataset.name;
                document.getElementById("overridePopup").style.display = "flex";
                loadPermissions(userId);
            });
        });

        function loadPermissions(userId) {
            document.getElementById("overrideTableBody").innerHTML =
                "<tr><td colspan='5'>Đang tải...</td></tr>";

            fetch("${pageContext.request.contextPath}/admin/permission-override?userId=" + userId)
                .then(r => r.json())
                .then(data => {
                    const tbody = document.getElementById("overrideTableBody");
                    tbody.innerHTML = "";

                    data.forEach(p => {

                        let badge = "";
                        if (p.overrideStatus === "GRANTED") {
                            badge = "GRANTED";
                        } else if (p.overrideStatus === "REVOKED") {
                            badge = "REVOKED";
                        } else {
                            badge = "Mặc định";
                        }

                        const tr = document.createElement("tr");

                        const td1 = document.createElement("td");
                        td1.textContent = p.permissionName;

                        const td2 = document.createElement("td");
                        td2.textContent = p.description || "";

                        const td3 = document.createElement("td");
                        td3.innerHTML = p.fromRole
                            ? '<i class="fa-solid fa-circle-check" style="color:green"></i>'
                            : '<i class="fa-solid fa-circle-xmark" style="color:red"></i>';

                        const td4 = document.createElement("td");
                        td4.textContent = badge;

                        const td5 = document.createElement("td");

                        const grantBtn = document.createElement("button");
                        grantBtn.textContent = "Grant";
                        grantBtn.onclick = () => {
                            console.log("Grant clicked");
                            doOverride(userId, p.id, "grant");
                        }

                        const revokeBtn = document.createElement("button");
                        revokeBtn.textContent = "Revoke";
                        revokeBtn.onclick = () => doOverride(userId, p.id, "revoke");

                        const resetBtn = document.createElement("button");
                        resetBtn.textContent = "Reset";
                        resetBtn.onclick = () => doOverride(userId, p.id, "reset");

                        td5.appendChild(grantBtn);
                        td5.appendChild(revokeBtn);
                        td5.appendChild(resetBtn);

                        tr.appendChild(td1);
                        tr.appendChild(td2);
                        tr.appendChild(td3);
                        tr.appendChild(td4);
                        tr.appendChild(td5);

                        tbody.appendChild(tr);
                    });
                });
        }


        function doOverride(userId, permissionId, action) {
            const body = "action=" + action
                + "&userId=" + userId
                + "&permissionId=" + permissionId;

            fetch("${pageContext.request.contextPath}/admin/permission-override", {
                method: "POST",
                headers: {"Content-Type": "application/x-www-form-urlencoded"},
                body: body
            })
                .then(r => r.json())
                .then(data => {
                    if (data.success) loadPermissions(userId);
                });
        }

        document.getElementById("resetAllBtn").addEventListener("click", function () {
            const userId = document.getElementById("overrideUserId").value;

            fetch("${pageContext.request.contextPath}/admin/permission-override", {
                method: "POST",
                headers: {"Content-Type": "application/x-www-form-urlencoded"},
                body: "action=resetAll&userId=" + userId
            })
                .then(r => r.json())
                .then(data => {
                    if (data.success) loadPermissions(userId);
                });
        });

        function closeOverridePopup() {
            document.getElementById("overridePopup").style.display = "none";
        }
</script>

</body>
</html>