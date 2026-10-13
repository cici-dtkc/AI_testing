<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Cổng thanh toán MoMo</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/qr-payment.css">
</head>
<body>
<div class="page-wrap">

    <div class="info-card">
        <div class="momo-logo-header">
            <span>Cổng thanh toán MoMo</span>
        </div>

        <div class="info-label">Nhà cung cấp</div>
        <div class="info-value">MoMo Payment</div>

        <div class="info-label">Mã đơn hàng</div>
        <div class="info-value" id="infoOrderId">--</div>

        <div class="info-label">Mô tả</div>
        <div class="info-value" id="infoDesc">Thanh toán đơn hàng</div>

        <div class="info-label">Số tiền</div>
        <div class="info-value amount" id="infoAmount">--</div>

        <div class="timer-box">
            <div class="timer-label">Đơn hàng sẽ hết hạn sau:</div>
            <div class="timer-digits">
                <div class="digit-block" id="tH">01</div>
                <div class="digit-sep">:</div>
                <div class="digit-block" id="tM">00</div>
                <div class="digit-sep">:</div>
                <div class="digit-block" id="tS">00</div>
            </div>
        </div>

        <a class="btn-back-link"
           onclick="location.href='${pageContext.request.contextPath}/checkout'">
            Quay về
        </a>
    </div>

    <div class="qr-card">
        <p class="qr-title">Sử dụng App MoMo hoặc ứng dụng camera hỗ trợ QR code để quét mã</p>

        <div class="qr-wrap">
            <img id="qrImg" src="${qrUrl}" alt="QR thanh toán"
                 onerror="this.src='https://via.placeholder.com/220x220?text=QR+Error'">
        </div>

        <p class="qr-hint">
            Sau khi quét và chuyển khoản thành công,<br>
            bấm nút bên dưới để xác nhận đơn hàng.
        </p>

        <form action="${pageContext.request.contextPath}/momo-return" method="get">
            <input type="hidden" name="resultCode" value="0">
            <input type="hidden" name="transId"  id="hiddenTransId" value="">
            <input type="hidden" name="orderId"  id="hiddenOrderId" value="">
            <button type="submit" class="btn-confirm">
                Tôi đã chuyển khoản xong
            </button>
        </form>
    </div>
</div>

<script>
    const params  = new URLSearchParams(window.location.search);
    const amount  = params.get('amount')  || '0';
    const orderId = params.get('orderId') || '';
    const info    = params.get('info')    || 'Thanh toan don hang Comic Store';

    function formatVND(num) {
        return Number(num).toLocaleString('vi-VN') + 'đ';
    }

    document.getElementById('infoAmount').textContent  = formatVND(amount);
    document.getElementById('infoOrderId').textContent = orderId;
    document.getElementById('infoDesc').textContent    = decodeURIComponent(info);
    document.getElementById('hiddenTransId').value     = orderId;
    document.getElementById('hiddenOrderId').value     = orderId;

    let seconds = 3600;

    function updateTimer() {
        if (seconds <= 0) {
            clearInterval(interval);
            document.getElementById('tH').textContent = '00';
            document.getElementById('tM').textContent = '00';
            document.getElementById('tS').textContent = '00';
            return;
        }
        document.getElementById('tH').textContent = String(Math.floor(seconds / 3600)).padStart(2, '0');
        document.getElementById('tM').textContent = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
        document.getElementById('tS').textContent = String(seconds % 60).padStart(2, '0');
    }

    updateTimer();
    var interval = setInterval(function() {
        seconds--;
        updateTimer();
    }, 1000);
</script>
</body>
</html>
