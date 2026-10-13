<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tạo mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/resetPassword.css">
</head>
<body>

<div class="container">
    <div class="left">
        <div class="box">
            <img src="${pageContext.request.contextPath}/img/logo.png" class="logo" alt="Logo">

            <form action="${pageContext.request.contextPath}/ResetPassServlet" method="post">
                <h2>Tạo mật khẩu</h2>

                <c:if test="${not empty error}">
                    <div class="error-message">${error}</div>
                </c:if>

                <label for="oldPassword">Nhập mật khẩu mới:</label>
                <div class="password-box">
                    <input type="password" name="oldPassword" id="oldPassword" placeholder="Mật khẩu mới" required>
                    <img src="${pageContext.request.contextPath}/img/eyePassword.png"
                         class="eye toggle1"
                         alt="Toggle password visibility">
                </div>

                <label for="newPassword">Xác nhận lại mật khẩu:</label>
                <div class="password-box">
                    <input type="password" name="newPassword" id="newPassword" placeholder="Xác nhận mật khẩu" required>
                    <img src="${pageContext.request.contextPath}/img/eyePassword.png"
                         class="eye toggle2"
                         alt="Toggle password visibility">
                </div>

                <button type="submit">Xác nhận</button>
            </form>
        </div>
    </div>
    <div class="right">
        <img src="${pageContext.request.contextPath}/img/anhLogin.png" alt="Books">
    </div>
</div>

<div class="popup-success" id="successPopup">
    <div class="success-box">
        <h2>Thay đổi mật khẩu thành công</h2>
        <div class="check-icon"></div>
    </div>
</div>

<script>
    function togglePassword(eyeClass) {
        const eye = document.querySelector("." + eyeClass);
        const input = eye.previousElementSibling;

        eye.addEventListener("click", function() {
            if (input.type === "password") {
                input.type = "text";
                eye.src = "${pageContext.request.contextPath}/img/eyePasswordHide.png";
            } else {
                input.type = "password";
                eye.src = "${pageContext.request.contextPath}/img/eyePassword.png";
            }
        });
    }

    togglePassword("toggle1");
    togglePassword("toggle2");

    <c:if test="${success eq true}">
    window.addEventListener('load', function() {
        const successPopup = document.getElementById("successPopup");
        successPopup.style.display = "flex";

        setTimeout(function() {
            window.location.href = "${pageContext.request.contextPath}/login";
        }, 3000);
    });
    </c:if>
</script>

</body>
</html>