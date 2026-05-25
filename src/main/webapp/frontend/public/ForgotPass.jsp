<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quên mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/ForgotPassword.css">
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
<div class="container">
    <div class="left">
        <div class="box">
            <img src="${pageContext.request.contextPath}/img/logo.png" class="logo">

            <form action="${pageContext.request.contextPath}/AuthenEmailServlet" method="post" onsubmit="return validateForgotPassForm()">
                <h2>Quên mật khẩu</h2>
                <p>Hãy nhập email của bạn để nhận mã xác thực</p>

                <div id="clientError" style="color: red; font-size: 14px; font-weight: bold; text-align: center; display: none; margin-bottom: 8px;"></div>

                <c:if test="${not empty error}">
                    <div id="serverError" style="color : red; font-size: 14px; display: flex;font-weight: bold; justify-content: center; margin-bottom: 8px;">${error}</div>
                </c:if>

                <label>Email:<span class="required-star">*</span></label>
                <input type="email" placeholder="Nhập email" name="email" id="emailInput" required>

                <!-- Google reCAPTCHA Widget -->
                <div class="g-recaptcha" data-sitekey="${applicationScope.RECAPTCHA_SITE_KEY}" style="margin-bottom: 15px; display: flex; justify-content: center;"></div>

                <div class="buttons">
                    <button class="back"
                            type="button"
                            onclick="window.location.href='${pageContext.request.contextPath}/login'"
                            style="color: black;">
                        ← Quay lại đăng nhập
                    </button>

                    <button class="send" type="submit" id="openPopup">Lấy mã xác thực</button>
                </div>
            </form>
        </div>
    </div>
    <div class="image-section">
        <img src="${pageContext.request.contextPath}/img/anhLogin.png" alt="Books">
    </div>
</div>
<div class="popup" id="otpPopup" style="display:none;">
    <div class="popup-content">
        <span class="close" id="closePopup">&times;</span>
        <h2>Nhập mã xác thực</h2>

        <c:if test="${not empty sessionScope.otpError}">
            <div style="color: red; margin-top: 8px;">${sessionScope.otpError}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/VerifyOtpServlet" method="post">
            <label>Nhập mã xác thực:</label>
            <input type="text" placeholder="Nhập mã" name="code" required>
            <button type="submit">Xác nhận</button>
        </form>
    </div>
</div>

<script>
    const popup = document.getElementById("otpPopup");
    const closeBtn = document.getElementById("closePopup");
    closeBtn.onclick = () => {
        popup.style.display = "none";
    }
    window.onclick = (e) => {
        if (e.target === popup) {
            popup.style.display = "none";
        }
    }
    <c:if test="${sessionScope.otpSent eq true}">
    window.onload = () => {
        popup.style.display = "flex";
    }
    </c:if>
    const emailInput = document.getElementById("emailInput");
    function validateEmail() {
        const value = emailInput.value.trim();
        const  emailRegex =  /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (value === ""){
            emailInput.classList.remove("valid");
            emailInput.classList.add("invalid");
        }else if(emailRegex.test(value)){
            emailInput.classList.remove("invalid");
            emailInput.classList.add("valid");
        }else {
            emailInput.classList.remove("valid");
            emailInput.classList.add("invalid");
        }
    }
    document.addEventListener("DOMContentLoaded", validateEmail);
    emailInput.addEventListener("input", validateEmail);
    emailInput.addEventListener("blur", validateEmail);

    emailInput.addEventListener("input", () => {
        const clientError = document.getElementById("clientError");
        const serverError = document.getElementById("serverError");
        if (clientError) clientError.style.display = "none";
        if (serverError) serverError.style.display = "none";
    });

    function validateForgotPassForm() {
        const clientError = document.getElementById("clientError");
        const serverError = document.getElementById("serverError");
        if (serverError) serverError.style.display = "none";
        clientError.style.display = "none";
        clientError.innerText = "";

        const value = emailInput.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (value === "") {
            clientError.innerText = "Vui lòng nhập email!";
            clientError.style.display = "block";
            return false;
        }
        if (!emailRegex.test(value)) {
            clientError.innerText = "Email không đúng định dạng!";
            clientError.style.display = "block";
            return false;
        }

        // Client-side CAPTCHA verification
        const recaptchaResponse = grecaptcha.getResponse();
        if (recaptchaResponse === "") {
            clientError.innerText = "Vui lòng xác minh CAPTCHA chính xác!";
            clientError.style.display = "block";
            return false;
        }

        return true;
    }
</script>
</body>
</html>