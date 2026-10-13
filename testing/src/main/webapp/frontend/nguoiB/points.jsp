<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.PointTransaction" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ưu đãi C-Point</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>

<jsp:include page="/frontend/public/header.jsp"/>

<main>
    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>

    <div class="xu-container">
        <div class="xu-header">
            <div class="xu-text">
                <p class="label">Số C-Point hiện có</p>
                <% int point =(Integer) request.getAttribute("point"); %>
                <h1><%= point%></h1>
            </div>
            <img src="${pageContext.request.contextPath}/img/dollar.png" class="xu-icon" alt="Point Icon">
        </div>

        <div class="history">
            <h3>Lịch sử giao dịch</h3>


            <%
                List<PointTransaction> pointHistory = (List<PointTransaction>) request.getAttribute("pointHistory");
                for (PointTransaction p : pointHistory){
                    if(p.getTransactionType().equals("EARN")){
            %>


                         <div class="item">
                             <div class="left">
                             <div class="light-icon">⚡</div>
                                 <p><%=p.getDescription()  %></p>
                            </div>
                             <span class="value plus">+<%=p.getPoints() %> C-Point</span>
                         </div>
            <%
                    }else{
            %>
            <div class="item">
                <div class="left">
                    <div class="light-icon">🎁</div>
                    <p><%=p.getDescription()%></p>
                </div>
                <span class="value minus">-<%=p.getPoints()%> C-Point</span>
            </div>
            <%
                    }
                }
            %>
        </div>
    </div>
</main>

<jsp:include page="/frontend/public/Footer.jsp"/>

</body>
</html>