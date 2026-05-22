<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ page isELIgnored="false" %>

            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <title>Đăng ký</title>
                <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/signIn.css">
                <script src="https://www.google.com/recaptcha/api.js" async defer></script>
            </head>

            <body>

                <div class="container">
                    <div class="left">
                        <div class="box">
                            <h2>Đăng ký</h2>

                            <p id="clientError" style="color:red; font-size:14px; font-weight:bold;
          text-align:center; display:none; margin-bottom:5px;">
                            </p>
                            <c:if test="${not empty error}">
                                <p class=""
                                    style="color : red; font-size: 14px; display: flex;font-weight: bold; justify-content: center;margin-bottom: 5px">
                                    ${error}</p>
                            </c:if>
                            <form action="${pageContext.request.contextPath}/RegisterServlet" method="post"
                                onsubmit="return validateRegister()">
                                <label>Nhập tên đăng nhập:</label>
                                <input id="username" type="text" placeholder="" name="username" maxlength="30">

                                <%-- <label>Nhập email:<span style="color:#999; font-size:12px;">(hoặc số điện
                                        thoại)</span></label>--%>
                                    <%-- <input id="email" type="email" placeholder="" name="email">--%>

                                        <%-- <label>Số điện thoại: <span style="color:#999; font-size:12px;">(hoặc
                                                email)</span></label>--%>
                                            <%-- <input id="phone" type="tel" placeholder="Nhập số điện thoại"
                                                name="phone">--%>

                                                    <div class="field">
                                                        <label>Email</label>
                                                        <input id="email" type="email" placeholder="Nhập email"
                                                            name="email" maxlength="100" required>
                                                    </div>


                                                <label>Nhập mật khẩu:</label>
                                                <div class="password-box">
                                                    <input id="password" type="password" placeholder="" name="password"
                                                        maxlength="72">
                                                    <img src="${pageContext.request.contextPath}/img/eyePassword.png"
                                                        class="eye toggle1">
                                                </div>

                                                <label>Xác nhận lại mật khẩu:</label>
                                                <div class="password-box">
                                                    <input id="confirmPassword" type="password" placeholder=""
                                                        name="confirmPassword" maxlength="72">
                                                    <img src="${pageContext.request.contextPath}/img/eyePassword.png"
                                                        class="eye toggle2">
                                                </div>

                                                 <!-- Google reCAPTCHA Widget -->
                                                 <div class="g-recaptcha" data-sitekey="${applicationScope.RECAPTCHA_SITE_KEY}" style="margin-bottom: 15px; display: flex; justify-content: center;"></div>

                                                 <button type="submit" value="Đăng ký" id="registerBtn">Đăng ký</button>
                                                 <p
                                                     style="text-align:center; margin-top:15px; font-size:14px; color:#333;">
                                                     Bạn đã có tài khoản?
                                                     <a href="${pageContext.request.contextPath}/login"
                                                         style="color:#0276DA; font-weight:bold; text-decoration:none;">
                                                         Đăng nhập
                                                     </a>
                                                 </p>
                            </form>

                        </div>

                    </div>
                    <div class="right">
                        <img src="${pageContext.request.contextPath}/img/anhLogin.png" alt="Books">
                    </div>

                </div>
                <div class="popup-success" id="successPopup">
                    <div class="success-box">
                        <h2>Đăng ký thành công</h2>
                        <div class="check-icon"></div>
                    </div>
                </div>

                <script>
                    function togglePassword(eyeClass) {
                        const eye = document.querySelector("." + eyeClass);
                        const input = eye.previousElementSibling;
                        eye.addEventListener("click", () => {
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
                    const usernameRegex = /^[a-zA-Z0-9_]{3,30}$/;
                    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
                    // Chấp nhận: 0912345678 , +84912345678 , +84 912 345 678
                    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[\W_]).{8,}$/;

                    function normalizePhone(val) {
                        return val.trim().replace(/\s+/g, '').replace(/^\+84/, '0');
                    }
                    function setValid(input) { input.classList.remove("invalid"); input.classList.add("valid"); }
                    function setInvalid(input) { input.classList.remove("valid"); input.classList.add("invalid"); }
                    function clearState(input) { input.classList.remove("valid", "invalid"); }
                    function validateUsername() {
                        const el = document.getElementById("username");
                        if (!usernameRegex.test(el.value.trim())) { setInvalid(el); return false; }
                        setValid(el); return true;
                    }
                    function validateEmail() {
                        const el = document.getElementById("email");
                        const val = el.value.trim();
                        if (val === "") { clearState(el); return null; }
                        if (!emailRegex.test(val)) { setInvalid(el); return false; }
                        setValid(el); return true;
                    }

                    function validatePassword() {
                        const el = document.getElementById("password");
                        if (!passwordRegex.test(el.value)) { setInvalid(el); return false; }
                        setValid(el); return true;
                    }

                    function validateConfirmPassword() {
                        const el = document.getElementById("confirmPassword");
                        const pw = document.getElementById("password").value;
                        if (el.value === "" || el.value !== pw) { setInvalid(el); return false; }
                        setValid(el); return true;
                    }
                    function validateEmailPhone() {
                        return validateEmail();
                    }

                    const phoneEl = document.getElementById("phone");
                    document.getElementById("username").addEventListener("input", validateUsername);
                    document.getElementById("email").addEventListener("input", () => {
                        validateEmail();
                        if (phoneEl && phoneEl.value.trim() !== "") validateEmailPhone();
                    });
                    document.getElementById("email").addEventListener("blur", validateEmailPhone);

                    if (phoneEl) {
                        phoneEl.addEventListener("input", () => {
                            if (typeof validatePhone === "function") validatePhone();
                            if (document.getElementById("email").value.trim() !== "") validateEmailPhone();
                        });
                        phoneEl.addEventListener("blur", validateEmailPhone);
                    }

                    document.getElementById("password").addEventListener("input", () => {
                        validatePassword();
                        if (document.getElementById("confirmPassword").value !== "") validateConfirmPassword();
                    });
                    document.getElementById("confirmPassword").addEventListener("input", validateConfirmPassword);

                    function validateRegister() {
                        const clientError = document.getElementById("clientError");
                        clientError.style.display = "none";
                        clientError.innerText = "";
                        const okUsername = validateUsername();
                        const okEmailPhone = validateEmailPhone();
                        const okPassword = validatePassword();
                        const okConfirm = validateConfirmPassword();
                        if (!okUsername) {
                            showRegisterError("Tên đăng nhập chỉ gồm chữ, số, dấu gạch dưới (3–30 ký tự)");
                            return false;
                        }
                        if (!okEmailPhone) {
                            const emailVal = document.getElementById("email").value.trim();
                            if (!emailVal) {
                                showRegisterError("Vui lòng nhập email");
                                return false;
                            } else if (!emailRegex.test(emailVal)) {
                                showRegisterError("Email không đúng định dạng");
                                return false;
                            }
                        }
                        if (!okPassword) {
                            showRegisterError("Mật khẩu phải hơn 8 ký tự, gồm chữ hoa, chữ thường và ký tự đặc biệt");
                            return false;
                        }
                        if (!okConfirm) {
                            showRegisterError("Mật khẩu xác nhận không khớp");
                            return false;
                        }

                        // Client-side CAPTCHA verification
                        const recaptchaResponse = grecaptcha.getResponse();
                        if (recaptchaResponse === "") {
                            showRegisterError("Vui lòng xác minh CAPTCHA chính xác!");
                            return false;
                        }

                        return true;
                    }

                    function showRegisterError(msg) {
                        const clientError = document.getElementById("clientError");
                        clientError.innerText = msg;
                        clientError.style.display = "block";
                    }
                </script>
                <c:if test="${not empty success}">
                    <script>
                        const successPopup = document.getElementById("successPopup");
                        successPopup.style.display = "flex";
                        setTimeout(() => {
                            window.location.href = "${pageContext.request.contextPath}/login";
                        }, 2000);
                    </script>
                </c:if>


            </body>




            </html>