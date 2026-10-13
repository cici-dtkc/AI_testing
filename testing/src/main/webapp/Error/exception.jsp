<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Lỗi xảy ra</title>
    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            text-align: center;
            padding: 50px;
            color: #333;
        }

        .error-container {
            background-color: #fff;
            border-radius: 10px;
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
            display: inline-block;
            padding: 40px 60px;
            max-width: 500px;
        }

        h2 {
            font-size: 36px;
            color: #dc3545;
            margin-bottom: 20px;
        }

        p {
            font-size: 18px;
            margin-bottom: 30px;
        }

        .btn-home {
            display: inline-block;
            padding: 10px 20px;
            background-color: #007bff;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            transition: background-color 0.3s ease;
        }

        .btn-home:hover {
            background-color: #0056b3;
        }
    </style>
</head>
<body>
<div class="error-container">
    <h2>Exception!</h2>
    <a href="${pageContext.request.contextPath}/frontend/public/homePage.jsp" class="btn-home">Về trang chủ</a>
</div>
</body>
</html>
