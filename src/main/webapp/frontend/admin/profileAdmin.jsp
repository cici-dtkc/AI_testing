
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<script>const BASE_URL = '${pageContext.request.contextPath}';</script>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Profile</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleProfile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>


<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <div class="profile-page">
            <div class="profile-right">
                <h4>Thông tin đăng nhập</h4>
                <p><strong>Email đăng nhập:</strong> admin@comicstore.com</p>
                <hr>
                <h4>Đổi mật khẩu</h4>
                <form id="changePassForm">
                    <small class="password-hint">
                        Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.
                    </small>
                    <label>Mật khẩu cũ
                        <input type="password" id="oldPassword" name="oldPassword" required>
                    </label>
                    <label>Mật khẩu mới
                        <input type="password" id="newPassword" name="newPassword" required minlength="8">
                    </label>
                    <label>Xác nhận mật khẩu
                        <input type="password" id="confirmPassword" name="confirmPassword" required>
                    </label>
                    <div class="actions">
                        <button type="submit" class="btn btn-primary">Đổi mật khẩu</button>
                    </div>
                    <p id="passMsg" class="muted"></p>
                </form>
            </div>
        </div>
    </div>
</div>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const bankView = document.getElementById('bankView');
        const bankEdit = document.getElementById('bankEdit');
        const btnEdit = document.getElementById('btn-edit-bank');
        const btnCancel = document.getElementById('btnCancelBank');
        const bankForm = document.getElementById('bankForm');
        const saveBtn = bankForm.querySelector('button[type="submit"]');
        const accountNumberInput = document.getElementById('accountNumber');
        const toggleEye = document.getElementById('toggleEye');
        const acctMasked = document.getElementById('acctMasked');
        const qrUpload = document.getElementById('qrUpload');
        const qrPreviewImg = document.getElementById('qrPreviewImg');
        const qrUploadBox = document.querySelector('.qr-upload-box');
        let actualAcct = accountNumberInput.value || '12341234';
        let masked = true;

        function maskAccount(acct) {
            const s = (acct || '').toString().replace(/\s+/g, '').replace(/[^0-9]/g, '');
            if (!s) return '----';
            const last4 = s.slice(-4);
            const maskedPart = s.slice(0, -4).replace(/\d/g, '*');
            return (maskedPart + last4).replace(/(.{4})/g, '$1 ').trim();
        }

        function updateMasked() {
            acctMasked.textContent = maskAccount(actualAcct);
        }

        toggleEye.addEventListener('click', () => {
            masked = !masked;
            if (masked) {
                accountNumberInput.value = maskAccount(actualAcct);
                toggleEye.classList.replace('fa-eye-slash', 'fa-eye');
            } else {
                accountNumberInput.value = actualAcct;
                toggleEye.classList.replace('fa-eye', 'fa-eye-slash');
            }
        });
        qrUpload.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = e => {
                    qrPreviewImg.src = e.target.result;
                    qrPreviewImg.style.display = 'block';
                };
                reader.readAsDataURL(file);
            } else {
                qrPreviewImg.style.display = 'none';
            }
        });
        btnEdit.addEventListener('click', () => {
            bankView.classList.add('hidden');
            bankEdit.classList.remove('hidden');
            accountNumberInput.value = masked ? maskAccount(actualAcct) : actualAcct;
            toggleEye.classList.toggle('fa-eye-slash', !masked);
            toggleEye.classList.toggle('fa-eye', masked);

        });
        btnCancel.addEventListener('click', () => {
            bankEdit.classList.add('hidden');
            bankView.classList.remove('hidden');
            qrUpload.value = '';
            qrPreviewImg.style.display = 'none';
        });
        saveBtn.disabled = true;
        updateMasked();
    });
    document.addEventListener('click', function (e) {
        if (e.target.closest('.qr-upload-box')) {
            e.preventDefault();
            const input = document.getElementById('qrUpload');
            if (input) input.click();
        }
    });
</script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        const current = window.location.pathname.split("/").pop();
        const links = document.querySelectorAll(".sidebar li a");
        links.forEach(link => {
            const linkPage = link.getAttribute("href");
            if (linkPage === current) {
                link.classList.add("active");
            }
        });
    });
</script>
<script>
    document.getElementById('changePassForm').addEventListener('submit', function (e) {
        e.preventDefault();
        const msg = document.getElementById('passMsg');
        msg.textContent = '';
        msg.className = 'muted';
        const body = new URLSearchParams({
            action: 'changePassword',
            oldPassword: document.getElementById('oldPassword').value,
            newPassword: document.getElementById('newPassword').value,
            confirmPassword: document.getElementById('confirmPassword').value
        });
        fetch(BASE_URL + '/admin/profile', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        })
            .then(r => r.json())
            .then(data => {
                msg.textContent = data.message;
                if (data.success) {
                    msg.style.color = 'green';
                    document.getElementById('changePassForm').reset();
                } else {
                    msg.style.color = 'red';
                }
            })
            .catch(() => {
                msg.textContent = 'Lỗi kết nối, vui lòng thử lại';
                msg.style.color = 'red';
            });
    });
</script>

</body>
</html>