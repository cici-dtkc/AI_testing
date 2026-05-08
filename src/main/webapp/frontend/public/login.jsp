<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/login1.css">

</head>
<body>

<div class="container">
    <div class="login-section">
        <div class="login-box">
            <form action="${pageContext.request.contextPath}/login" method="post" onsubmit="return validateLogin()">
                <h2>Đăng nhập</h2>
                <p id="clientError"
                   style="color:red; font-size:14px; font-weight: bold; text-align:center; display:none;">
                </p>

                <c:if test="${not empty error}">
                    <h3 id="serverError"
                        style="color:red; font-size:14px; text-align:center;">
                            ${error}
                    </h3>
                </c:if>

                <label>Nhập Email hoặc Tên đăng nhập:</label>
                <input type="text" id="username" placeholder="Email hoặc tên đăng nhập" name="username" maxlength="100"
                       autocomplete="username">
                <span id="usernameHint" class="hint"></span>
                <label>Nhập mật khẩu:</label>
                <div class="password-box">
                    <input id="password" type="password" placeholder="Mật khẩu" name="password" maxlength="100"
                           autocomplete="current-password">
                    <span class="eye">
                    <img src="${pageContext.request.contextPath}/img/eyePassword.png" id="toggleEye" alt="eye">
                </span>
                </div>
                <span id="passwordHint" class="hint"></span>
                <div class="links">
                    <a href="${pageContext.request.contextPath}/RegisterServlet">Đăng ký tài khoản</a>
                    <a href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu</a>
                </div>

                <button type="submit" value="Đăng nhập">Đăng nhập</button>

            </form>
            <p class="or">Hoặc đăng nhập bằng</p>

            <div class="social">
                <img class="google" id="googleLogin"
                     src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ13Zhu7gCjSl_MTncNr7aEbl9HkMWaidA4wNFbPO0oxQm49i-RPgTEME3oN_nSroHl1KqwY2haK-IFEYfBc1BkwP9PBNEv9ApC9tvzcVM&s"
                     alt="Google"
                     style="cursor: pointer;">

                <img class="facebook" id="facebookLogin"
                     src="https://upload.wikimedia.org/wikipedia/commons/b/b9/2023_Facebook_icon.svg"
                     alt="Facebook"
                     style="cursor: pointer; width: 40px; height: 40px;">
            </div>

        </div>
    </div>
    <div class="image-section">
        <img src="${pageContext.request.contextPath}/img/anhLogin.png" alt="Books">
    </div>
</div>

<script>
    const eye = document.getElementById("toggleEye");
    const passwordInput = document.querySelector(".password-box input");

    if (eye && passwordInput) {
        eye.addEventListener("click", () => {
            if (passwordInput.type === "password") {
                passwordInput.type = "text";
                eye.src = "${pageContext.request.contextPath}/img/eyePasswordHide.png";
            } else {
                passwordInput.type = "password";
                eye.src = "${pageContext.request.contextPath}/img/eyePassword.png";
            }
        });
    }
    const googleBtn = document.getElementById("googleLogin");
    if (googleBtn) {
        googleBtn.addEventListener("click", () => {
            window.location.href = "${pageContext.request.contextPath}/login-google";
        });
    }
    const facebookBtn = document.getElementById("facebookLogin");
    if (facebookBtn) {
        // facebookBtn.addEventListener("click", () => {
        //     window.location.href = "https://www.facebook.com/v18.0/dialog/oauth?client_id=4112997315630876&redirect_uri=http://localhost:8080/TTLTW_Nhom6/login-facebook-callback&scope=email,public_profile";
        // });
        facebookBtn.addEventListener("click", () => {
            window.location.href = "${pageContext.request.contextPath}/facebook-login";
        });
    }
</script>
<script>
    function validateLogin() {
        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;
        const errorBox = document.getElementById("clientError");

        errorBox.style.display = "none";
        errorBox.innerText = "";

        if (username === "") {
            showError("Vui lòng nhập email hoặc tên đăng nhập");
            document.getElementById("username").focus();
            return false;
        }
        if (password === "") {
            showError("Vui lòng nhập mật khẩu");
            document.getElementById("password").focus();
            return false;
        }
        if (username.length < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự");
            return false;
        }
        if (username.includes("@")) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(username)) {
                showError("Địa chỉ email không hợp lệ");
                return false;
            }
        }
        if (password.length < 8) {
            showError("Mật khẩu phải có ít nhất 8 ký tự");
            return false;
        }
        return true;
    }

    function showError(msg) {
        const errorBox = document.getElementById("clientError");
        const serverError = document.getElementById("serverError");
        if (serverError) {
            serverError.style.display = "none";
        }

        errorBox.innerText = msg;
        errorBox.style.display = "block";
    }

</script>
<script>

    const usernameInput = document.getElementById("username");
    const passwordInput2 = document.getElementById("password");

    usernameInput.addEventListener("input", function () {
        const val = this.value.trim();
        const hint = document.getElementById("usernameHint");

        if (val === "") {
            setInputState(this, hint, null, "");
            return;
        }

        if (val.length < 3) {
            setInputState(this, hint, false, "Tên đăng nhập phải có ít nhất 3 ký tự");
            return;
        }

        // Nếu có @  validate email
        if (val.includes("@")&& val.indexOf("@") < val.lastIndexOf(".")) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(val)) {
                setInputState(this, hint, false, "Địa chỉ email không hợp lệ");
            } else {
                setInputState(this, hint, true, "");
            }
            return;
        }

        const usernameRegex = /^[a-zA-Z0-9_.]+$/;
        if (!usernameRegex.test(val)) {
            setInputState(this, hint, false, "Tên đăng nhập không được chứa ký tự đặc biệt");
        } else {
            setInputState(this, hint, true, "");
        }
    });

    passwordInput2.addEventListener("input", function () {
        const val = this.value;
        const hint = document.getElementById("passwordHint");

        if (val === "") {
            setInputState(this, hint, null, "");
            return;
        }

        if (val.length < 8) {
            setInputState(this, hint, false, `Mật khẩu phải có ít nhất 8 ký tự (cần thêm ${8 - val.length} ký tự)`);
            return;
        }

        const hasUpper = /[A-Z]/.test(val);
        const hasLower = /[a-z]/.test(val);
        const hasSpecial = /[\W_]/.test(val);

        if (!hasUpper || !hasLower || !hasSpecial) {
            let missing = [];
            if (!hasUpper) missing.push("chữ hoa");
            if (!hasLower) missing.push("chữ thường");
            if (!hasSpecial) missing.push("ký tự đặc biệt");
            setInputState(this, hint, false, `Mật khẩu phải có: ${missing.join(", ")}`);
            return;
        }

        setInputState(this, hint, true, "");
    });

    //Xóa lỗi server khi bắt đầu gõ
    usernameInput.addEventListener("input", clearServerError);
    passwordInput2.addEventListener("input", clearServerError);

    function clearServerError() {
        const serverError = document.getElementById("serverError");
        const clientError = document.getElementById("clientError");
        if (serverError) serverError.style.display = "none";
        if (clientError) clientError.style.display = "none";
    }
    function setInputState(inputEl, hintEl, valid, message) {
        inputEl.classList.remove("valid", "invalid");
        hintEl.classList.remove("error", "success");
        hintEl.innerText = message;

        if (valid === true) {
            inputEl.classList.add("valid");
            hintEl.classList.add("success");
        } else if (valid === false) {
            inputEl.classList.add("invalid");
            hintEl.classList.add("error");
        } else {
            hintEl.style.display = "none";
            return;
        }
        hintEl.style.display = "block";
    }
</script>


</body>
</html>