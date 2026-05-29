<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Theo dõi trạng thái đăng nhập</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ưu đãi C-Point</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/status-login.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>
<main>
    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>

    <div class="login-all">
        <h2>Thông tin đăng nhập tài khoản</h2>
        <div class="table-container">
            <table>
                <thead>
                <tr>
                    <th>User ID</th>
                    <th>IP Address</th>
                    <th>User Agent</th>
                    <th>Login Time</th>
                    <th>Trạng thái</th>
                </tr>
                </thead>
                <tbody>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                <tr>

                    <td>101</td>
                    <td>192.168.1.10</td>
                    <td>Chrome/120.0</td>
                    <td>2026-05-29 19:45:00</td>
                    <td class="active">Đang hoạt động</td>
                </tr>

                <tr>
                    <td>102</td>
                    <td>192.168.1.11</td>
                    <td>Firefox/115.0</td>
                    <td>2026-05-28 14:20:00</td>
                    <td class="inactive">Đã thoát</td>
                </tr>

                </tbody>
            </table>
        </div>

    </div>
</main>
<jsp:include page="/frontend/public/Footer.jsp"/>
</body>
</html>
