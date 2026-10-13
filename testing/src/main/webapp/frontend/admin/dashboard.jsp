<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Trang chủ - Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleDashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <div class="dashboard">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h2>Tổng quan cửa hàng</h2>
                <div class="filter-section">
                    <form action="${pageContext.request.contextPath}/admin/dashboard" method="get" id="filterForm">
                        <input type="hidden" name="action" value="filter">
                        <input type="hidden" name="period" id="periodInput" value="${currentPeriod}">
                        <div class="filter-tabs">
                            <button type="button" class="filter-tab ${currentPeriod == 'today' ? 'active' : ''}"
                                    onclick="selectPeriod('today')">
                                Hôm nay
                            </button>
                            <button type="button" class="filter-tab ${currentPeriod == 'week' ? 'active' : ''}"
                                    onclick="selectPeriod('week')">
                                Tuần này
                            </button>
                            <button type="button" class="filter-tab ${currentPeriod == 'month' ? 'active' : ''}"
                                    onclick="selectPeriod('month')">
                                Tháng này
                            </button>
                            <button type="button" class="filter-tab ${currentPeriod == 'year' ? 'active' : ''}"
                                    onclick="selectPeriod('year')">
                                Năm này
                            </button>
                            <button type="button" class="filter-tab ${currentPeriod == 'custom' ? 'active' : ''}"
                                    onclick="toggleCustomDate()">
                                Tùy chỉnh
                            </button>
                        </div>
                        <div id="customDateRange" class="custom-date-range"
                             style="display: ${currentPeriod == 'custom' ? 'flex' : 'none'};">
                            <input type="date" name="startDate" value="${startDate}"
                                   placeholder="Từ ngày">
                            <span>đến</span>
                            <input type="date" name="endDate" value="${endDate}"
                                   placeholder="Đến ngày">
                            <button type="submit" class="btn-apply">
                                <i class="fa-solid fa-check"></i> Áp dụng
                            </button>
                        </div>
                    </form>
                </div>
            </div>
            <div class="stats-grid">
                <div class="stat-card">
                    <i class="fa-solid fa-book"></i>
                    <div>
                        <h3><fmt:formatNumber value="${stats.comicsOnSale}" pattern="#,##0"/></h3>
                        <p>Truyện đang bán</p>
                    </div>
                </div>
                <div class="stat-card">
                    <i class="fa-solid fa-receipt"></i>
                    <div>
                        <h3><fmt:formatNumber value="${stats.totalOrders}" pattern="#,##0"/></h3>
                        <p>Đơn hàng</p>
                    </div>
                </div>
                <div class="stat-card">
                    <i class="fa-solid fa-sack-dollar"></i>
                    <div>
                        <h3><fmt:formatNumber value="${stats.revenue}" pattern="#,##0"/>đ</h3>
                        <p>Doanh thu</p>
                    </div>
                </div>
                <div class="stat-card">
                    <i class="fa-solid fa-user"></i>
                    <div>
                        <h3><fmt:formatNumber value="${stats.newCustomers}" pattern="#,##0"/></h3>
                        <p>Khách hàng mới</p>
                    </div>
                </div>
            </div>
            <div class="chart-section">
                <h3 id="chartTitle">Biểu đồ doanh thu</h3>
                <canvas id="revenueChart"></canvas>
            </div>
            <div class="bestseller-section">
                <h3>Top 10 sản phẩm bán chạy nhất</h3>
                <table class="bestseller-table">
                    <thead>
                    <tr>
                        <th>Top</th>
                        <th>Sản phẩm</th>
                        <th>Số lượng bán</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="product" items="${topProducts}" varStatus="status">
                        <tr>
                            <td>${status.index + 1}</td>
                            <td>${product.name_comics}</td>
                            <td><fmt:formatNumber value="${product.total_sold}" pattern="#,##0"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty topProducts}">
                        <tr>
                            <td colspan="3" style="text-align: center; color: #999;">
                                Chưa có dữ liệu
                            </td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
            <div class="bestseller-section">
                <h3>Top 10 sản phẩm có đánh giá cao nhất</h3>
                <table class="bestseller-table">
                    <thead>
                    <tr>
                        <th>Top</th>
                        <th>Sản phẩm</th>
                        <th>Điểm trung bình</th>
                        <th>Số đánh giá</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="comic" items="${topRatedComics}" varStatus="status">
                        <tr>
                            <td>${status.index + 1}</td>
                            <td>${comic.name_comics}</td>
                            <td>
                                <div style="display: flex; align-items: center; gap: 5px;">
                                    <span style="color: #ffa500; font-weight: bold;">
                                        <fmt:formatNumber value="${comic.average_rating}" pattern="#.##" maxFractionDigits="2"/>
                                    </span>
                                    <i class="fa-solid fa-star" style="color: #ffa500; font-size: 14px;"></i>
                                </div>
                            </td>
                            <td>
                                <fmt:formatNumber value="${comic.total_reviews}" pattern="#,##0"/> đánh giá
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty topRatedComics}">
                        <tr>
                            <td colspan="4" style="text-align: center; color: #999;">
                                Chưa có dữ liệu đánh giá
                            </td>
                        </tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
    const chartData = ${chartData};
    const chartType = '${chartType}';
    const currentPeriod = '${currentPeriod}';
    function selectPeriod(period) {
        document.getElementById('periodInput').value = period;
        if (period !== 'custom') {
            document.getElementById('filterForm').submit();
        }
    }
    function toggleCustomDate() {
        const customDateRange = document.getElementById('customDateRange');
        const periodInput = document.getElementById('periodInput');
        if (customDateRange.style.display === 'none' || customDateRange.style.display === '') {
            customDateRange.style.display = 'flex';
            periodInput.value = 'custom';
            document.querySelectorAll('.filter-tab').forEach(btn => btn.classList.remove('active'));
            event.target.classList.add('active');
        } else {
            customDateRange.style.display = 'none';
        }
    }
    const ctx = document.getElementById('revenueChart').getContext('2d');
    let labels = [];
    let data = [];
    let chartTitle = '';
    if (chartType === 'monthly') {
        chartTitle = 'Doanh thu 12 tháng trong năm';
        labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
        data = new Array(12).fill(0);
        chartData.forEach(item => {
            const monthIndex = item.month - 1;
            data[monthIndex] = item.revenue / 1000000; // Chuyển sang triệu đồng
        });
    } else {
        if (currentPeriod === 'today') {
            chartTitle = 'Doanh thu hôm nay';
        } else if (currentPeriod === 'week') {
            chartTitle = 'Doanh thu theo ngày trong tuần';
        } else if (currentPeriod === 'month') {
            chartTitle = 'Doanh thu theo ngày trong tháng';
        } else {
            chartTitle = 'Doanh thu theo ngày';
        }
        chartData.forEach(item => {
            const date = new Date(item.date);
            const formattedDate = (date.getDate()) + '/' + (date.getMonth() + 1);
            labels.push(formattedDate);
            data.push(item.revenue / 1000000);
        });
    }
    document.getElementById('chartTitle').textContent = chartTitle;
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Doanh thu (Triệu VNĐ)',
                data: data,
                fill: true,
                borderColor: '#007bff',
                backgroundColor: 'rgba(0, 123, 255, 0.1)',
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return 'Doanh thu: ' + context.parsed.y.toFixed(2) + 'M VNĐ';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: value => value + 'M'
                    }
                }
            }
        }
    });
    document.addEventListener("DOMContentLoaded", function () {
        const current = window.location.pathname;
        const links = document.querySelectorAll(".sidebar li a");
        links.forEach(link => {
            const linkHref = link.getAttribute("href");
            if (current.endsWith(linkHref) || current.includes(linkHref)) {
                link.classList.add("active");
            } else {
                link.classList.remove("active");
            }
        });
    });
</script>
</body>
</html>
