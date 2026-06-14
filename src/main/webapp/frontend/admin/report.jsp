<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Voucher" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Thống kê</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleReport.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Thống kê</h2>
        <div class="report-wrapper">
            <div class="filter-time">
                <button class="time-btn active" data-filter="today">Hôm nay</button>
                <button class="time-btn" data-filter="week">Tuần này</button>
                <button class="time-btn" data-filter="month">Tháng này</button>
                <button class="time-btn" data-filter="year">Năm nay</button>
                <button class="time-btn" data-filter="custom">Tùy chỉnh</button>
                <input type="date" id="startDate" style="display:none;">
                <input type="date" id="endDate" style="display:none;">

                <button class="export-btn" onclick="exportExcel()">
                    <i class="fas fa-file-excel"></i> Xuất Excel
                </button>

            </div>

            <div class="kpi-cards">
                <div class="kpi-card" data-chart="revenue">
                    <i class="fas fa-dollar-sign kpi-icon"></i>
                    <div class="kpi-info">
                        <span class="kpi-title">Doanh thu</span>
                        <span class="kpi-value" id="kpi-revenue">0đ</span>
                    </div>
                </div>
                <div class="kpi-card" data-chart="orders">
                    <i class="fas fa-shopping-cart kpi-icon"></i>
                    <div class="kpi-info">
                        <span class="kpi-title">Số đơn hàng</span>
                        <span class="kpi-value" id="kpi-orders">0 đơn</span>
                    </div>
                </div>
                <div class="kpi-card" data-chart="avgValue">
                    <i class="fas fa-chart-line kpi-icon"></i>
                    <div class="kpi-info">
                        <span class="kpi-title">Giá trị đơn trung bình</span>
                        <span class="kpi-value" id="kpi-avg">0đ</span>
                    </div>
                </div>
                <div class="kpi-card">
                    <i class="fas fa-star kpi-icon"></i>
                    <div class="kpi-info">
                        <span class="kpi-title">Sản phẩm bán chạy nhất</span>
                        <span class="kpi-value" id="kpi-best">Chưa có dữ liệu</span>
                    </div>
                </div>
            </div>
            <div class="chart-container">
                <h3 id="chart-title">Biểu đồ Doanh thu</h3>
                <canvas id="mainChart"></canvas>
            </div>
            <div class="top-products-wrapper">
                <div class="top-products">
                    <h3>Top 3 sản phẩm bán chạy</h3>
                    <table>
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>Đã bán</th>
                        </tr>
                        </thead>
                        <tbody id="top-products-body">
                        <tr>
                            <td colspan="2">Đang tải...</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div class="top-products-chart">
                    <canvas id="topProductsChart"></canvas>
                </div>
            </div>


            <div class="flashsale-stats-wrapper">
                <div class="flashsale-ranking">
                    <h3>Top 5 flash sale doanh thu cao nhất</h3>
                    <div id="flashsale-ranking-list">
                        <p style="color:#888; text-align:center; padding:20px;">Đang tải...</p>
                    </div>
                </div>
                <div class="flashsale-table-box">
                    <h3>Danh sách flash sale</h3>
                    <table class="flashsale-table">
                        <thead>
                        <tr>
                            <th>Tên Flash Sale</th>
                            <th>Giảm</th>
                            <th>Trạng thái</th>
                            <th>Đơn hàng</th>
                            <th>Doanh thu</th>
                        </tr>
                        </thead>
                        <tbody id="flashsale-table-body">
                        <tr>
                            <td colspan="5" style="text-align:center; color:#888;">Đang tải...</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>


            <div class="voucher-stats-wrapper">

                <div class="voucher-ranking">
                    <h3>Top 5 voucher được dùng nhiều nhất</h3>
                    <div id="voucher-ranking-list">

                            <p></p>

                    </div>
                </div>


                <div class="voucher-table-box">
                    <h3>Danh sách voucher</h3>
                    <table class="voucher-table">
                        <thead>
                        <tr>
                            <th>Mã voucher</th>
                            <th>Giảm</th>
                            <th>Trạng thái</th>
                            <th>Lượt dùng / Tổng</th>
                            <th>Doanh thu tiết kiệm</th>
                        </tr>
                        </thead>
                    </table>

                    <div class="voucher-tbody-scroll">
                        <table class="voucher-table">
                            <tbody id="voucher-table-body">

                                <tr>
                                    <td colspan="5"></td>
                                </tr>

                            </tbody>
                        </table>
                    </div>
                </div>

            </div>


            <div class="comics-status-wrapper">
                <div class="comics-status-box sold-box">
                    <h3>Truyện đã bán
                        <span class="comics-count-badge" id="sold-count"></span>
                    </h3>
                    <input type="text" class="comics-search" id="sold-search"
                           placeholder="Tìm kiếm truyện">
                    <div class="comics-scroll">
                        <table class="comics-table">
                            <thead>
                                <tr>
                                    <th>Tên truyện</th>
                                    <th>Đã bán</th>
                                    <th>Doanh thu</th>
                                </tr>
                            </thead>

                            <tbody id="sold-comics-body">
                                <tr><td colspan="3"></td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="comics-status-box unsold-box">
                    <h3>Truyện chưa bán
                        <span class="comics-count-badge" id="unsold-count">0</span>
                    </h3>
                    <input type="text" class="comics-search" id="unsold-search"
                           placeholder="Tìm kíếm truyện">
                    <div class="comics-scroll">
                        <table class="comics-table">

                            <thead>
                                <tr>
                                    <th>Tên truyện</th>
                                    <th>Số lượng trong kho</th>
                                </tr>
                            </thead>

                            <tbody id="unsold-comics-body">
                                <tr>
                                    <td>Dragon Ball</td>
                                    <td>1000</td>
                                </tr>
                            </tbody>

                        </table>
                    </div>
                </div>
            </div>

        </div>
    </div>
</div>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        const contextPath = '<%=request.getContextPath()%>';
        let mainChart = null;
        let topProductsChart = null;
        let currentChartType = 'revenue';
        let reportData = null;

        function formatCurrency(value) {
            if (value === null || value === undefined || isNaN(value)) {
                return '0đ';
            }
            const numValue = Number(value);
            if (numValue === 0) return '0đ';
            return new Intl.NumberFormat('vi-VN').format(Math.round(numValue)) + 'đ';
        }

        function formatNumber(value) {
            if (value === null || value === undefined || isNaN(value)) {
                return '0';
            }
            const numValue = Number(value);
            if (numValue === 0) return '0';
            return new Intl.NumberFormat('vi-VN').format(numValue);
        }

        function loadReportData(filter, startDate, endDate) {
            const params = new URLSearchParams({filter: filter});
            if (filter === 'custom' && startDate && endDate) {
                params.append('startDate', startDate);
                params.append('endDate', endDate);
            }

            fetch(contextPath + '/admin/report-data?' + params.toString())
                .then(response => response.json())
                .then(data => {
                    reportData = data;
                    updateKPICards(data.kpi);
                    updateTopProducts(data.topProducts);
                    updateChart(currentChartType);
                    updateFlashSales(data.flashSales);
                    updateVoucherRanking(data.topVouchers);
                    updateVoucherTable(data.allVouchers);
                    updateComicsSold(data.comicsSold, data.totalSold);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Lỗi tải dữ liệu!');
                });
        }

        function updateKPICards(kpi) {
            const revenueEl = document.getElementById('kpi-revenue');
            const ordersEl = document.getElementById('kpi-orders');
            const avgEl = document.getElementById('kpi-avg');
            const bestEl = document.getElementById('kpi-best');
            if (revenueEl) {
                revenueEl.textContent = formatCurrency(kpi.revenue || 0);
            }
            if (ordersEl) {
                ordersEl.textContent = formatNumber(kpi.totalOrders || 0) + ' đơn';
            }
            if (avgEl) {
                avgEl.textContent = formatCurrency(kpi.avgOrderValue || 0);
            }
            if (bestEl) {
                bestEl.textContent = kpi.bestProduct || 'Chưa có dữ liệu';
            }
        }

        function updateTopProducts(products) {
            const tbody = document.getElementById('top-products-body');
            if (!products || products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="2">Chưa có dữ liệu</td></tr>';
                updateDoughnutChart([]);
                return;
            }
            tbody.innerHTML = products.map(p =>
                '<tr><td>' + p.name_comics + '</td><td>' + formatNumber(p.total_sold) + '</td></tr>'
            ).join('');
            updateDoughnutChart(products);
        }

        function updateDoughnutChart(products) {
            const canvas = document.getElementById('topProductsChart');
            const ctx = canvas.getContext('2d');
            if (topProductsChart) {
                topProductsChart.destroy();
            }
            if (!products || products.length === 0) return;
            topProductsChart = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: products.map(p => p.name_comics),
                    datasets: [{
                        data: products.map(p => p.total_sold),
                        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56'],
                        borderWidth: 2,
                        borderColor: '#fff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '60%',
                    plugins: {
                        legend: {
                            display: true,
                            position: 'bottom',
                            labels: {font: {size: 12}, padding: 15, usePointStyle: true}
                        },
                        tooltip: {
                            callbacks: {
                                label: function (context) {
                                    return context.label + ': ' + formatNumber(context.parsed) + ' cuốn';
                                }
                            }
                        }
                    },
                    animation: {duration: 1000, easing: 'easeOutQuart'}
                }
            });
        }

        function updateFlashSales(flashSales) {
            const rankingEl = document.getElementById('flashsale-ranking-list');
            const tableBody = document.getElementById('flashsale-table-body');

            if (!flashSales || flashSales.length === 0) {
                rankingEl.innerHTML = '<p style="color:#888; text-align:center; padding:20px;">Chưa có dữ liệu flash sale</p>';
                tableBody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#888;">Chưa có dữ liệu</td></tr>';
                return;
            }

            const maxRevenue = flashSales[0].total_revenue || 1;
            const rankColors = ['#FAC775', '#D3D1C7', '#F5C4B3'];
            const statusMap = {
                'active': ['badge-active', 'Đang diễn ra'],
                'ended': ['badge-ended', 'Đã kết thúc'],
                'scheduled': ['badge-scheduled', 'Sắp diễn ra']
            };

            rankingEl.innerHTML = flashSales.map((fs, i) => {
                const pct = Math.round((fs.total_revenue / maxRevenue) * 100);
                const rankBg = rankColors[i] || '#e0e0e0';
                const rankColor = i === 0 ? '#633806' : i === 1 ? '#2C2C2A' : i === 2 ? '#4A1B0C' : '#555';
                return '<div class="fs-rank-item">' +
                    '<div class="fs-rank-badge" style="background:' + rankBg + '; color:' + rankColor + ';">' + (i + 1) + '</div>' +
                    '<div class="fs-rank-info">' +
                    '<div class="fs-rank-name">' + fs.name + '</div>' +
                    '<div class="fs-rank-bar-wrap"><div class="fs-rank-bar" style="width:' + pct + '%"></div></div>' +
                    '</div>' +
                    '<div class="fs-rank-revenue">' + formatCurrency(fs.total_revenue) + '</div>' +
                    '</div>';
            }).join('');

            tableBody.innerHTML = flashSales.map(fs => {
                const statusEntry = statusMap[fs.status] || ['badge-ended', fs.status];
                const badgeClass = statusEntry[0];
                const statusText = statusEntry[1];
                return '<tr>' +
                    '<td>' + fs.name + '</td>' +
                    '<td>' + fs.discount_percent + '%</td>' +
                    '<td><span class="fs-badge ' + badgeClass + '">' + statusText + '</span></td>' +
                    '<td>' + formatNumber(fs.total_orders) + '</td>' +
                    '<td>' + formatCurrency(fs.total_revenue) + '</td>' +
                    '</tr>';
            }).join('');
        }

        function updateChart(type) {
            if (!reportData || !reportData.chartData) return;
            currentChartType = type;
            const canvas = document.getElementById('mainChart');
            const ctx = canvas.getContext('2d');
            if (mainChart) mainChart.destroy();
            let chartData, label, title;
            switch (type) {
                case 'revenue':
                    chartData = reportData.chartData.revenue;
                    label = 'Doanh thu';
                    title = 'Biểu đồ Doanh thu';
                    break;
                case 'orders':
                    chartData = reportData.chartData.orders;
                    label = 'Số đơn hàng';
                    title = 'Biểu đồ Số đơn hàng';
                    break;
                case 'avgValue':
                    chartData = reportData.chartData.avgValue;
                    label = 'Giá trị trung bình';
                    title = 'Biểu đồ Giá trị đơn trung bình';
                    break;
            }

            document.getElementById('chart-title').textContent = title;
            const gradient = ctx.createLinearGradient(0, 0, 0, 400);
            gradient.addColorStop(0, 'rgba(0,123,255,0.8)');
            gradient.addColorStop(1, 'rgba(0,123,255,0.2)');
            mainChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        label: label,
                        data: chartData.data,
                        backgroundColor: gradient,
                        borderRadius: 5,
                        borderSkipped: false
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: {display: false},
                        tooltip: {
                            enabled: true,
                            callbacks: {
                                label: function (context) {
                                    let value = context.parsed.y;
                                    if (type === 'orders') {
                                        return label + ': ' + formatNumber(value) + ' đơn';
                                    } else {
                                        return label + ': ' + formatCurrency(value);
                                    }
                                }
                            }
                        }
                    },
                    animation: {duration: 1000, easing: 'easeOutQuart'},
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function (value) {
                                    if (type === 'orders') {
                                        return formatNumber(value);
                                    } else {
                                        return formatCurrency(value);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }

        function updateVoucherRanking(vouchers) {
            const rankingEl = document.getElementById('voucher-ranking-list');
            if (!vouchers || vouchers.length === 0) {
                rankingEl.innerHTML = '<p>Không có voucher</p>';
                return;
            }
            rankingEl.innerHTML = vouchers.map(function(v, i) {
                const percent = v.quantity > 0 ? Math.round(v.usedCount * 100 / v.quantity) : 0;
                return '<div class="vs-rank-item">' +
                    '<div class="vs-rank-badge">' + (i + 1) + '</div>' +
                    '<div class="vs-rank-info">' +
                    '<div class="vs-rank-name">' + v.code + '</div>' +
                    '<div class="vs-rank-bar-wrap"><div class="vs-rank-bar" style="width:' + percent + '%"></div></div>' +
                    '<span class="vs-usage-label">' + v.usedCount + '/' + v.quantity + ' lượt · ' + percent + '%</span>' +
                    '</div>' +
                    '<div class="vs-rank-usage">' + v.usedCount + ' lượt</div>' +
                    '</div>';
            }).join('');
        }

        function updateVoucherTable(vouchers) {
            const tbody = document.getElementById('voucher-table-body');
            if (!vouchers || vouchers.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5">Không có voucher</td></tr>';
                return;
            }
            tbody.innerHTML = vouchers.map(function(v) {
                let statusHtml;
                if (v.expired) {
                    statusHtml = '<span class="vs-badge badge-ended">Hết hạn</span>';
                } else if (v.usedCount >= v.quantity) {
                    statusHtml = '<span class="vs-badge badge-ended">Hết lượt</span>';
                } else {
                    statusHtml = '<span class="vs-badge badge-active">Đang hoạt động</span>';
                }
                return '<tr>' +
                    '<td>' + v.code + '</td>' +
                    '<td>' + v.discountValue + '%</td>' +
                    '<td>' + statusHtml + '</td>' +
                    '<td>' + v.usedCount + '/' + v.quantity + '</td>' +
                    '<td>' + formatCurrency(v.savedRevenue || 0) + '</td>' +
                    '</tr>';
            }).join('');
        }

        function updateComicsSold(comicsSold, totalSold) {
            const tbody = document.getElementById('sold-comics-body');
            const countBadge = document.getElementById('sold-count');

            if (countBadge) countBadge.textContent = totalSold || 0;

            if (!comicsSold || comicsSold.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3">Không có truyện</td></tr>';
                return;
            }

            tbody.innerHTML = comicsSold.map(c =>
                '<tr>' +
                '<td>' + c.name_comics + '</td>' +
                '<td>' + formatNumber(c.total_sold) + '</td>' +
                '<td>' + formatCurrency(c.revenue) + '</td>' +
                '</tr>'
            ).join('');

        }

        document.querySelectorAll('.kpi-card[data-chart]').forEach(card => {
            card.addEventListener('click', function () {
                const chartType = this.getAttribute('data-chart');
                updateChart(chartType);
                document.querySelectorAll('.kpi-card[data-chart]').forEach(c => {
                    c.style.border = 'none';
                });
                this.style.border = '2px solid #007bff';
            });
        });
        const timeButtons = document.querySelectorAll('.time-btn');
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        timeButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                timeButtons.forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                const filter = this.getAttribute('data-filter');
                if (filter === 'custom') {
                    startDateInput.style.display = 'inline-block';
                    endDateInput.style.display = 'inline-block';
                } else {
                    startDateInput.style.display = 'none';
                    endDateInput.style.display = 'none';
                    loadReportData(filter);
                }
            });
        });
        startDateInput.addEventListener('change', function () {
            if (endDateInput.value) {
                loadReportData('custom', this.value, endDateInput.value);
            }
        });
        endDateInput.addEventListener('change', function () {
            if (startDateInput.value) {
                loadReportData('custom', startDateInput.value, this.value);
            }
        });
        window.onload = function() {
            console.log('window loaded');
            loadReportData('today');
            const current = window.location.pathname.split("/").pop();
            const links = document.querySelectorAll(".sidebar li a");
            links.forEach(link => {
                const linkPage = link.getAttribute("href");
                if (linkPage === current) {
                    link.classList.add("active");
                }
            });
        };

        function exportExcel() {
            const activeBtn = document.querySelector('.time-btn.active');
            const filter = activeBtn ? activeBtn.getAttribute('data-filter') : 'today';

            let url = contextPath + '/admin/export-excel?filter=' + filter;

            if (filter === 'custom') {
                const start = document.getElementById('startDate').value;
                const end = document.getElementById('endDate').value;
                if (!start || !end) {
                    alert('Vui lòng chọn khoảng thời gian!');
                    return;
                }
                url += '&startDate=' + start + '&endDate=' + end;
            }
            window.location.href = url;
        }
    </script>
</body>
</html>