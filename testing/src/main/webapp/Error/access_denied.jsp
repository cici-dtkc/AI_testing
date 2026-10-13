<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.User" %>
<!DOCTYPE html>
<html>
<head>
    <title>403 - Access Denied</title>
    <style>
        body {
            font-family: Arial;
            text-align: center;
            margin-top: 100px;
            background: #f8f9fa;
        }
        .box {
            display: inline-block;
            padding: 30px;
            border-radius: 10px;
            background: white;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
        }
        h1 { color: red; }
        a {
            text-decoration: none;
            color: white;
            background: #007bff;
            padding: 10px 20px;
            border-radius: 5px;
        }
    </style>
</head>
<body>
<div class="box">
    <h1>403 - Access Denied</h1>
    <p>Bạn không có quyền truy cập trang này.</p>
    <%
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        Boolean isAdmin = (session != null) ? (Boolean) session.getAttribute("isAdmin") : null;

        String backUrl;
        String backText;

        if (currentUser == null) {
            backUrl = request.getContextPath() + "/login";
            backText = "Đăng nhập";
        } else if (Boolean.TRUE.equals(isAdmin)) {
            backUrl = request.getContextPath() + "/admin/dashboard";
            backText = "Quay về Dashboard";
        } else {
            backUrl = request.getContextPath() + "/home";
            backText = "Quay về trang chủ";
        }
    %>
    <a href="<%= backUrl %>"><%= backText %></a>
</div>
</body>
</html>