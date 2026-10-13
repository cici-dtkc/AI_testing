<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <style>
        .alert {
            padding: 15px;
            margin: 15px 0;
            border-radius: 4px;
            font-size: 1rem;
            opacity: 0.95;
        }
        .alert-error {
            background-color: #ffebee;
            color: #b71c1c;
            border: 1px solid #ef9a9a;
            border-left: 5px solid #d32f2f;
        }
        .alert-success {
            background-color: #e8f5e9;
            color: #1b5e20;
            border: 1px solid #a5d6a7;
            border-left: 5px solid #2e7d32;
        }

    </style>
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>
<main>
    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>
    <div class="change-password-container">
        <h2>Đổi Mật Khẩu</h2>
        <c:if test="${not empty error}">
            <div class="alert alert-error">
                <strong>Lỗi:</strong> ${error}
            </div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success">
                <strong>Thành công:</strong> ${success}
            </div>
        </c:if>
        <form  id="changePasswordForm" action="${pageContext.request.contextPath}/change-password" method="post">
            <div class="form-group">
                <label for="current-password">Mật khẩu hiện tại*:</label>
                <input type="password" id="current-password" name="current-password" required>
            </div>
            <div class="form-group">
                <label for="new-password">Mật khẩu mới*:</label>
                <input type="password" id="new-password" name="new-password" required>
                <p id="passwordHint" class="password-requirement">Yêu cầu: Tối thiểu 8 ký tự, bao gồm chữ hoa và thường, số và ký tự đặc biệt.</p>
            </div>
            <div class="form-group">
                <label for="confirm-password">Xác nhận mật khẩu mới*:</label>
                <input type="password" id="confirm-password" name="confirm-password" required>
            </div>
            <div class="form-group">
                <p class="security-note">Lưu ý: Để bảo mật, không chia sẻ mật khẩu với bất kỳ ai. Nếu quên mật khẩu, hãy
                    liên hệ hỗ trợ qua email comicstore365@gmail.com.</p>
            </div>
            <button type="submit" class="save-btn">Lưu thay đổi</button>
        </form>
    </div>
</main>
<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    const newPasswordInput = document.getElementById("new-password");
    const passwordHint = document.getElementById("passwordHint");

    newPasswordInput.addEventListener("input", function () {
        const val = this.value;

        if (val === "") {
            passwordHint.style.color = "";
            passwordHint.innerText = "Yêu cầu: Tối thiểu 8 ký tự, bao gồm chữ hoa và thường, số và ký tự đặc biệt.";
            return;
        }

        let missing = [];
        const hasSpecial = /[^A-Za-z0-9]/.test(val);

        if (val.length < 8) missing.push("ít nhất 8 ký tự");
        if (!/[A-Z]/.test(val)) missing.push("chữ hoa");
        if (!/[a-z]/.test(val)) missing.push("chữ thường");
        if (!/[0-9]/.test(val)) missing.push("chữ số");
        if (!hasSpecial) missing.push("ký tự đặc biệt");

        if (missing.length > 0) {
            passwordHint.style.color = "red";
            passwordHint.innerText = "Mật khẩu còn thiếu " + missing.join(", ");
        }else {
            passwordHint.style.color = "green";
            passwordHint.innerText = "Mật khẩu hợp lệ";
        }
        if (confirmPasswordInput.value !== "") {
            confirmPasswordInput.dispatchEvent(new Event("input"));
        }
    });

    const confirmPasswordInput = document.getElementById("confirm-password");
    const confirmHint = document.createElement("p");
    confirmHint.className = "password-requirement";
    confirmPasswordInput.parentNode.insertBefore(confirmHint, confirmPasswordInput.nextSibling);

    confirmPasswordInput.addEventListener("input", function () {
        const val = this.value;

        if (val === "") {
            confirmHint.style.color = "";
            confirmHint.innerText = "";
            return;
        }

        if (val !== newPasswordInput.value) {
            confirmHint.style.color = "red";
            confirmHint.innerText = "Mật khẩu không giống";
        } else {
            confirmHint.style.color = "green";
            confirmHint.innerText = "Mật khẩu hợp lệ";
        }
    });

    newPasswordInput.addEventListener("input", function () {
        if (confirmPasswordInput.value !== "") {
            confirmPasswordInput.dispatchEvent(new Event("input"));
        }
    });

    document.getElementById("changePasswordForm").addEventListener("submit", function (e) {
        const newVal     = newPasswordInput.value;
        const confirmVal = confirmPasswordInput.value;

        const isValid = newVal.length >= 8
            && /[A-Z]/.test(newVal)
            && /[a-z]/.test(newVal)
            && /[0-9]/.test(newVal)
            && /[^A-Za-z0-9]/.test(newVal);

        if (!isValid) {
            e.preventDefault();
            newPasswordInput.focus();
            return;
        }

        if (confirmVal !== newVal) {
            e.preventDefault();
            confirmHint.style.color = "red";
            confirmHint.innerText = "Mật khẩu không giống";
            confirmPasswordInput.focus();
        }
    });
</script>


</body>
</html>