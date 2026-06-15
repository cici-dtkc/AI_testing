    <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
    <%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
    <%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.User" %>

    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Thông tin tài khoản</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    </head>
    <style>
        .hint {
            color: #ef4444;
            display: none;
            font-size: 13px;
            margin-top: 4px;
        }
    </style>
    <body>

    <jsp:include page="/frontend/public/header.jsp"/>

    <main>
        <c:if test="${not empty message}">
            <script>
                window.addEventListener('DOMContentLoaded', function () {
                    const overlay = document.createElement('div');
                    overlay.className = 'msg-overlay';
                    overlay.innerHTML =
                        '<div class="msg-popup">' +
                        '<div class="msg-popup-icon"></div>' +
                        '<h3>Thành công</h3>' +
                        '<p>${message}</p>' +
                        '<button class="msg-btn-ok">OK</button>' +
                        '</div>';
                    document.body.appendChild(overlay);
                    requestAnimationFrame(() => overlay.classList.add('show'));

                    function closeMsg() {
                        overlay.classList.remove('show');
                        setTimeout(() => overlay.remove(), 250);
                    }

                    overlay.querySelector('.msg-btn-ok').addEventListener('click', closeMsg);
                });
            </script>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <script>
                window.addEventListener('DOMContentLoaded', function () {
                    const overlay = document.createElement('div');
                    overlay.className = 'msg-overlay';

                    overlay.innerHTML =
                        '<div class="msg-popup">' +
                        '<div class="msg-popup-icon"></div>' +
                        '<h3>Lỗi</h3>' +
                        '<p>${errorMessage}</p>' +
                        '<button class="msg-btn-ok">OK</button>' +
                        '</div>';

                    document.body.appendChild(overlay);
                    requestAnimationFrame(() => overlay.classList.add('show'));

                    function closeMsg() {
                        overlay.classList.remove('show');
                        setTimeout(() => overlay.remove(), 250);
                    }


                    overlay.addEventListener('click', function (e) {
                        if (e.target === overlay) closeMsg();
                    });


                });
            </script>
        </c:if>

        <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>

        <div class="profile-form-container">
            <h2>Hồ sơ cá nhân</h2>
            <form action="${pageContext.request.contextPath}/updateUser" method="post">
                <div class="form-group">
                    <label for="ho">Họ: *</label>
                    <input type="text" id="ho" name="ho" value="${ho}" placeholder="Nhập họ" required>
                    <span id="hoHint" class="hint"></span>
                </div>
                <div class="form-group">
                    <label for="ten">Tên: *</label>
                    <input type="text" id="ten" name="ten" value="${ten}" placeholder="Nhập tên" required>
                    <span id="tenHint" class="hint"></span>
                </div>
                <div class="form-group">
                    <label for="phone">Số điện thoại:</label>
                    <input type="tel" id="phone" name="phone" value="${phone}" placeholder="Nhập số điện thoại"
                           required pattern="[0-9]{10}" maxlength="10" title="nhập lại sdt"/>
                    <span id="phoneHint" class="hint"></span>
                </div>
                <div class="form-group">
                    <label for="email">Email: </label>
                    <input type="email" id="email" name="email" value="${email}" placeholder="Nhập email" required>
                    <span id="emailHint" class="hint"></span>
                </div>
                <div class="form-group">
                    <div class="genders">
                        <label>Giới tính: *</label>
                        <div class="gender-options">
                            <label><input type="radio" name="gender" value="male" ${gender eq 'male' ? 'checked' : ''}> Nam</label>
                            <label><input type="radio" name="gender" value="female"  ${gender eq 'female' ? 'checked' : ''}> Nữ</label>
                        </div>
                    </div>
                </div>
                <div class="form-group">
                    <div class="birthday">
                        <label for="day">Ngày tháng năm sinh: *</label>
                        <input type="number" id="day" name="day" value="${day}" min="1" max="31" required> /
                        <input type="number" id="month" name="month" value="${month}" min="1" max="12" required> /
                        <input type="number" id="year" name="year" value="${year}" min="1900" max="2025" required>
                        <span id="birthHint" class="hint"></span>
                    </div>
                </div>
                <div class="form-group">
                    <div class="address">
                        <div class="diaChi">
                            <div class="address-group">
                                <label for="province">Tỉnh/Thành phố: *</label>
                                <select id="province" name="province" required>
                                    <option value="">Chọn tỉnh/thành phố</option>
                                </select>
                            </div>
                            <div class="address-group">
                                <label for="district">Phường/Xã: *</label>
                                <select id="district" name="district" required disabled>
                                    <option value="">Chọn phường/Xã</option>
                                </select>
                            </div>
                        </div>

                        <label for="house-number">Số nhà: *</label>
                        <input type="text" id="house-number" name="house-number" value="${houseNumber}" placeholder="Nhập số nhà, xã" required>
                        <span id="addressHint" class="hint"></span>
                    </div>
                </div>

                <button type="submit" class="save-btn">Lưu thay đổi</button>
            </form>
        </div>
    </main>

    <jsp:include page="/frontend/public/Footer.jsp"/>

    <script>
        document.addEventListener("DOMContentLoaded", function () {

            const API_BASE = "${pageContext.request.contextPath}/api/provinces";

            const provinceSelect = document.getElementById("province");
            const districtSelect = document.getElementById("district");
            const savedProvince = "${province}";
            const savedDistrict = "${district}";
            const isProvinceCode = "${isProvinceCode}";
            const isDistrictCode = "${isDistrictCode}";
            fetch(API_BASE + "/p/")
                .then(res => {
                    if (!res.ok) throw new Error("Không load được tỉnh");
                    return res.json();
                })
                .then(provinces => {
                    provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>';

                    let selectedProvinceCode = null;

                    provinces.forEach(p => {
                        const opt = document.createElement("option");
                        opt.value = p.name;
                        opt.textContent = p.name;
                        opt.dataset.code = p.code;
                        opt.dataset.name = p.name;
                        if (p.name === savedProvince || p.code == savedProvince) {
                            opt.selected = true;
                            selectedProvinceCode = p.code;
                        }

                        provinceSelect.appendChild(opt);
                    });

                    provinceSelect.disabled = false;
                    console.log("Provinces loaded:", provinces.length);

                    // Nếu có tỉnh đã lưu, load huyện tương ứng
                    if (selectedProvinceCode) {
                        loadDistricts(selectedProvinceCode, savedDistrict);
                    }
                })
                .catch(err => {
                    console.error("Lỗi load tỉnh:", err);
                    alert("Không thể tải danh sách tỉnh/thành phố");
                });
            function loadDistricts(provinceCode, districtToSelect = null) {
                districtSelect.innerHTML = '<option>Đang tải...</option>';
                districtSelect.disabled = true;

                if (!provinceCode) {
                    districtSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                    return;
                }

                fetch(API_BASE + "/p/" + provinceCode + "?depth=2")
                    .then(res => {
                        if (!res.ok) throw new Error("Không load được huyện");
                        return res.json();
                    })
                    .then(data => {
                        districtSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';

                        let districts = [];

                        if (data.districts && Array.isArray(data.districts)) {
                            districts = data.districts;
                        } else if (data.wards && Array.isArray(data.wards)) {
                            districts = data.wards;
                        } else if (Array.isArray(data)) {
                            districts = data;
                        }

                        districts.forEach(d => {
                            const opt = document.createElement("option");
                            opt.value = d.name;
                            opt.textContent = d.name;
                            opt.dataset.code = d.code;
                            opt.dataset.name = d.name;
                            if (districtToSelect && (d.name === districtToSelect || d.code == districtToSelect)) {
                                opt.selected = true;
                                console.log("Selected district:", d.name);
                            }

                            districtSelect.appendChild(opt);
                        });

                        districtSelect.disabled = false;
                        console.log("Districts loaded:", districts.length);
                    })
                    .catch(err => {
                        console.error("Lỗi load huyện:", err);
                        alert("Không thể tải danh sách huyện");
                    });
            }
            provinceSelect.addEventListener("change", function () {
                const selectedOption = this.options[this.selectedIndex];
                const provinceCode = selectedOption.dataset.code;
                if (!provinceCode) {
                    districtSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                    districtSelect.disabled = true;
                    return;
                }
                loadDistricts(provinceCode);
            });

        });
    </script>


    <script>
        function setInputState(inputEl, hintEl, valid, message) {
            inputEl.classList.remove("valid", "invalid");

            if (valid === null) {
                hintEl.innerText = "";
                hintEl.style.display = "none";
                return;
            }

            if (valid) {
                inputEl.classList.add("valid");
                hintEl.style.display = "none";
            } else {
                inputEl.classList.add("invalid");
                hintEl.innerText = message;
                hintEl.style.display = "block";
            }
        }


        const hoInput = document.getElementById("ho");

        hoInput.addEventListener("input", function () {
            const hint = document.getElementById("hoHint");
            const val = this.value.trim();
            const nameRegex = /^[\p{L}\s]+$/u;

            if (val === "") {
                setInputState(this, hint, false,
                    "Vui lòng nhập họ");
                return;
            }

            if (val.length < 2) {
                setInputState(this, hint, false,
                    "Họ phải có ít nhất 2 ký tự");
                return;
            }

            if (!nameRegex.test(val)) {
                setInputState(this, hint, false,
                    "Họ chỉ được chứa chữ");
                return;
            }

            setInputState(this, hint, true, "");
        });


        const tenInput = document.getElementById("ten");

        tenInput.addEventListener("input", function () {
            const hint = document.getElementById("tenHint");
            const val = this.value.trim();
            const nameRegex = /^[\p{L}\s]+$/u;

            if (val === "") {
                setInputState(this, hint, false,
                    "Vui lòng nhập tên");
                return;
            }

            if (val.length < 2) {
                setInputState(this, hint, false,
                    "Tên phải có ít nhất 2 ký tự");
                return;
            }

            if (!nameRegex.test(val)) {
                setInputState(this, hint, false,
                    "Tên chỉ được chứa chữ");
                return;
            }

            setInputState(this, hint, true, "");
        });


        const phoneInput = document.getElementById("phone");

        phoneInput.addEventListener("input", function () {
            const hint = document.getElementById("phoneHint");
            this.value = this.value.replace(/\D/g, "");
            const val = this.value;
            const phoneRegex = /^(0[35789])[0-9]{8}$/;

            if (val === "") {
                setInputState(this, hint, false,
                    "Vui lòng nhập số điện thoại");
                return;
            }

            if (!phoneRegex.test(val)) {
                setInputState(this, hint, false,
                    "SĐT phải bắt đầu bằng 03/05/07/08/09 và đủ 10 số");
                return;
            }

            setInputState(this, hint, true, "");
        });


        const emailInput = document.getElementById("email");

        emailInput.addEventListener("input", function () {
            const hint = document.getElementById("emailHint");
            const val = this.value.trim();
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

            if (val === "") {
                setInputState(this, hint, false,
                    "Vui lòng nhập email");
                return;
            }

            if (!emailRegex.test(val)) {
                setInputState(this, hint, false,
                    "Email không hợp lệ");
                return;
            }

            setInputState(this, hint, true, "");
        });


        function validateBirthDate() {
            const day = document.getElementById("day");
            const month = document.getElementById("month");
            const year = document.getElementById("year");
            const hint = document.getElementById("birthHint");

            if (!day.value || !month.value || !year.value) {
                hint.style.display = "none";
                return;
            }

            const d = parseInt(day.value);
            const m = parseInt(month.value);
            const y = parseInt(year.value);

            const birthDate = new Date(y, m - 1, d);

            if (
                birthDate.getFullYear() !== y ||
                birthDate.getMonth() !== m - 1 ||
                birthDate.getDate() !== d
            ) {
                if (!day.value || !month.value || !year.value) {
                    setInputState(year, hint, false,
                        "Ngày sinh không hợp lệ");
                }
                return;
            }

            const today = new Date();
            let age = today.getFullYear() - y;

            if (
                today.getMonth() < (m - 1) ||
                (today.getMonth() === (m - 1) &&
                    today.getDate() < d)
            ) {
                age--;
            }

            if (age < 5 || age > 120) {
                hint.innerText = "Tuổi phải từ 5 đến 120";
                hint.style.display = "block";
                return;
            }

            hint.style.display = "none";
        }

        document.getElementById("day")
            .addEventListener("input", validateBirthDate);

        document.getElementById("month")
            .addEventListener("input", validateBirthDate);

        document.getElementById("year")
            .addEventListener("input", validateBirthDate);


        function validateAddress() {
            const house = document.getElementById("house-number");
            const province = document.getElementById("province");
            const district = document.getElementById("district");
            const hint = document.getElementById("addressHint");

            if (
                house.value.trim() === "" ||
                province.value === "" ||
                district.value === ""
            ) {
                hint.innerText = "Vui lòng nhập đầy đủ địa chỉ";
            }
        }

        document.getElementById("house-number")
            .addEventListener("input", validateAddress);

        document.getElementById("province")
            .addEventListener("change", validateAddress);

        document.getElementById("district")
            .addEventListener("change", validateAddress);
        window.addEventListener('DOMContentLoaded', function () {
            document.getElementById('ho').dispatchEvent(new Event('input'));
            document.getElementById('ten').dispatchEvent(new Event('input'));
            document.getElementById('phone').dispatchEvent(new Event('input'));
            document.getElementById('email').dispatchEvent(new Event('input'));
            validateBirthDate();
        });
    </script>
    </body>
    </html>
