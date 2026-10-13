<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    // Nếu không có orderSuccess trong session thì redirect về home
    if (session.getAttribute("orderSuccess") == null) {
        response.sendRedirect(request.getContextPath() + "/home");
        return;
    }
    String orderSuccess = (String) session.getAttribute("orderSuccess");
    session.removeAttribute("orderSuccess");
    session.removeAttribute("orderId");
    session.removeAttribute("orderTotal");
    session.removeAttribute("orderPaymentMethod");
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đặt hàng thành công</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/orderSuccess.css">
    <title>Title</title>
</head>
<body>
<div class="popup">
    <div class="icon"><i class="fas fa-check-circle"></i></div>
    <h2>Đặt hàng thành công!</h2>
    <p><%= orderSuccess %></p>
    <a href="${pageContext.request.contextPath}/order-history?filter=pending" class="btn">Xem đơn hàng của tôi</a>
    <p class="countdown">Tự động chuyển sau <span id="sec">3</span> giây...</p>
</div>

<script>
    let s = 3;
    const secEl = document.getElementById('sec');
    const timer = setInterval(() => {
        s--;
        secEl.textContent = s;
        if (s <= 0) {
            clearInterval(timer);
            window.location.href = '${pageContext.request.contextPath}/order-history?filter=pending';
        }
    }, 1000);
</script>
</body>
</html>
