<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>Xác nhận Email</title>
  <style>
    body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; background: #f4f4f4; }
    .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); text-align: center; }
    input[type="text"] { padding: 10px; width: 80%; margin: 15px 0; border: 1px solid #ccc; border-radius: 5px; font-size: 16px; text-align: center; letter-spacing: 2px;}
    button { padding: 10px 20px; background: #0276DA; color: white; border: none; border-radius: 5px; cursor: pointer; font-size: 16px; }
    .error { color: red; margin-bottom: 10px; }
  </style>
</head>
<body>
<div class="container">
  <h2>Xác thực tài khoản</h2>
  <p>Vui lòng nhập mã OTP đã được gửi đến email: <b>${sessionScope.otpEmail_register}</b></p>

  <c:if test="${not empty error}">
    <p class="error">${error}</p>
  </c:if>

  <form action="${pageContext.request.contextPath}/VerifyRegisterServlet" method="post">
    <input type="text" name="otp" placeholder="Nhập mã OTP 6 số" maxlength="6" required>
    <br>
    <button type="submit">Xác nhận</button>
  </form>
</div>
</body>
</html>
