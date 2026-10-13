<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


    <aside class="sidebar">
        <div class="sidebar-header">
            <img src="${pageContext.request.contextPath}/img/logo.png" alt="Logo" class="logo">
            <h2>Comic Store</h2>
        </div>

        <ul>
            <li>
                <a href="${pageContext.request.contextPath}/admin/dashboard">
                    <img src="${pageContext.request.contextPath}/img/home.png" class="icon">
                    <span>Dashboard</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/ProductManagement">
                    <img src="${pageContext.request.contextPath}/img/series.png" class="icon">
                    <span>Quản lý sản phẩm</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/SeriesManagement?filter=all">
                    <img src="${pageContext.request.contextPath}/img/product.png" class="icon">
                    <span>Quản lý series</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/CategoryManagement">
                    <img src="${pageContext.request.contextPath}/img/category.png" class="icon">
                    <span>Quản lý thể loại</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/orders">
                    <img src="${pageContext.request.contextPath}/img/order.png" class="icon">
                    <span>Quản lý đơn hàng</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/inventory">
                    <img src="${pageContext.request.contextPath}/img/product.png" class="icon">
                    <span>Quản lý kho hàng</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/user-management">
                    <img src="${pageContext.request.contextPath}/img/user.png" class="icon">
                    <span>Quản lý người dùng</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/manage-flashsale">
                    <img src="${pageContext.request.contextPath}/img/flashSale.png" class="icon">
                    <span>Quản lý Flash Sale</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/vouchersManagement">
                    <img src="${pageContext.request.contextPath}/img/promo.png" class="icon">
                    <span>Quản lý khuyến mãi</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/permission-management">
                    <img src="${pageContext.request.contextPath}/img/authorization.png" class="icon">
                    <span>Phân quyền</span>
                </a>
            </li>
            <li>
                <a href="${pageContext.request.contextPath}/admin/ReportManagement">
                    <img src="${pageContext.request.contextPath}/img/report.png" class="icon">
                    <span>Thống kê</span>
                </a>
            </li>

        </ul>
    </aside>


<script>
    document.addEventListener("DOMContentLoaded", function () {
        const currentPath = window.location.pathname;
        const sidebarLinks = document.querySelectorAll(".sidebar ul li a");

        sidebarLinks.forEach(link => {
            const linkPath = new URL(link.href).pathname;
            if (currentPath.startsWith(linkPath)) {
                link.classList.add("active");
            }
        });
    });
</script>

