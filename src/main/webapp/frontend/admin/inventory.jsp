<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
            <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                <html>

                <head>
                    <meta charset="UTF-8">
                    <title>Quản lý kho hàng</title>
                    <link rel="stylesheet"
                        href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
                    <link rel="stylesheet"
                        href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
                    <link rel="stylesheet"
                        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleInventory.css">
                </head>

                <body>
                    <div class="container">
                        <jsp:include page="/frontend/admin/ASide.jsp" />
                        <div class="main-content">
                            <%@ include file="HeaderAdmin.jsp" %>
                                <div class="admin-content">
                                    <div class="page-header">
                                        <h2>Quản lý kho hàng & Thống kê tồn kho</h2>
                                        <div class="filter-group">
                                            <button class="btn btn-primary"
                                                onclick="openAdjustmentModal(null, 'import')">
                                                <i class="fa-solid fa-plus"></i> Điều chỉnh kho thủ công
                                            </button>
                                        </div>
                                    </div>
                                    <div class="tabs-container">
                                        <button class="tab-btn active" onclick="switchTab('lowStockTab')">
                                            <i class="fa-solid fa-triangle-exclamation"></i> Cần nhập gấp
                                            (${fn:length(lowStockComics)})
                                        </button>
                                        <button class="tab-btn" onclick="switchTab('allProductsTab')">
                                            <i class="fa-solid fa-book"></i> Quản lý tồn kho
                                        </button>
                                        <button class="tab-btn" onclick="switchTab('historyTab')">
                                            <i class="fa-solid fa-clock-rotate-left"></i> Nhật ký giao dịch
                                        </button>
                                    </div>
                                    <div id="lowStockTab" class="tab-content active">
                                        <div class="control-panel">
                                            <form method="get"
                                                action="${pageContext.request.contextPath}/admin/inventory"
                                                class="filter-group">
                                                <label for="thresholdSelect"
                                                    style="font-weight: 600; font-size: 14px;">Mức cảnh báo tồn:</label>
                                                <select id="thresholdSelect" name="threshold" class="form-select">
                                                    <option value="5" ${threshold==5 ? 'selected' : '' }>&lt;= 5 cuốn</option>
                                                    <option value="10" ${threshold==10 ? 'selected' : '' }>&lt;= 10 cuốn</option>
                                                    <option value="20" ${threshold==20 ? 'selected' : '' }>&lt;= 20 cuốn</option>
                                                    <option value="50" ${threshold==50 ? 'selected' : '' }>&lt;= 50 cuốn</option>
                                                    <option value="custom" ${threshold!=5 && threshold!=10 && threshold!=20 && threshold!=50 ? 'selected' : '' }>Tùy chỉnh...</option>
                                                </select>
                                                <div id="customThresholdContainer" style="display: ${threshold!=5 && threshold!=10 && threshold!=20 && threshold!=50 ? 'inline-flex' : 'none'}; align-items: center; gap: 8px;">
                                                    <input type="number" id="customThresholdInput" class="form-input" min="1" value="${threshold}" style="width: 80px; min-width: 80px; padding: 6px 10px;" placeholder="Số lượng">
                                                    <button type="button" class="btn btn-primary" onclick="submitCustomThreshold()" style="padding: 6px 12px; font-size: 13px;">Áp dụng</button>
                                                </div>
                                            </form>
                                            <div style="font-size: 14px; color: var(--text-muted); font-weight: 500;">
                                                Có <b>${fn:length(lowStockComics)}</b> tác phẩm dưới mức an toàn.
                                            </div>
                                        </div>

                                        <div class="table-container">
                                            <table>
                                                <thead>
                                                    <tr>
                                                        <th>Truyện quyển</th>
                                                        <th>Tồn kho (Tổng)</th>
                                                        <th>Bị hỏng</th>
                                                        <th>Khả dụng (Bán)</th>
                                                        <th>Đã bán (Completed)</th>
                                                        <th>Đánh giá & Gợi ý</th>
                                                        <th>Hành động</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <c:choose>
                                                        <c:when test="${empty lowStockComics}">
                                                            <tr>
                                                                <td colspan="7"
                                                                    style="text-align: center; color: var(--text-muted); padding: 24px;">
                                                                    Không có tác phẩm nào dưới mức cảnh báo. Kho hàng an
                                                                    toàn!
                                                                </td>
                                                            </tr>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:forEach var="comic" items="${lowStockComics}">
                                                                <c:set var="targetStock" value="${(threshold * 3) > comic.total_sold ? (threshold * 3) : comic.total_sold}" />
                                                                <c:set var="suggestedQty" value="${targetStock - comic.available_quantity}" />
                                                                <tr>
                                                                    <td>
                                                                        <div class="comic-cell">
                                                                            <img src="${comic.thumbnail_url}"
                                                                                alt="Cover" class="comic-thumb"
                                                                                onerror="this.src='${pageContext.request.contextPath}/img/logo.png'">
                                                                            <div class="comic-info">
                                                                                <h4>${fn:escapeXml(comic.name_comics)}
                                                                                </h4>
                                                                                <span>Tập: ${comic.volume != null ?
                                                                                    comic.volume : '-'}</span>
                                                                            </div>
                                                                        </div>
                                                                    </td>
                                                                    <td><b>${comic.stock_quantity}</b></td>
                                                                    <td class="text-muted">${comic.damaged_quantity}
                                                                    </td>
                                                                    <td>
                                                                        <span class="stock-indicator stock-low">
                                                                            <i
                                                                                class="fa-solid fa-circle-exclamation"></i>
                                                                            ${comic.available_quantity}
                                                                        </span>
                                                                    </td>
                                                                    <td>${comic.total_sold}</td>
                                                                    <td>
                                                                        <c:choose>
                                                                            <c:when test="${suggestedQty > 0}">
                                                                                <span class="badge badge-warning" style="background-color: #FEF3C7; color: #D97706; border: 1px solid #FFE0B2; font-weight: 600; padding: 4px 8px; border-radius: 6px; font-size: 12px; display: inline-flex; align-items: center; gap: 4px;">
                                                                                    <i class="fa-solid fa-truck-ramp-box"></i> Nhập thêm <b>${suggestedQty}</b> cuốn
                                                                                </span>
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                <span class="badge badge-success" style="background-color: #D1FAE5; color: #065F46; border: 1px solid #A7F3D0; font-weight: 600; padding: 4px 8px; border-radius: 6px; font-size: 12px; display: inline-flex; align-items: center; gap: 4px;">
                                                                                    <i class="fa-solid fa-circle-check"></i> Không cần nhập
                                                                                </span>
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </td>
                                                                    <td>
                                                                        <button class="btn btn-success btn-sm"
                                                                            onclick="openAdjustmentModal(${comic.id}, 'import', ${suggestedQty > 0 ? suggestedQty : 0})">
                                                                            <i class="fa-solid fa-plus"></i> Nhập hàng
                                                                            nhanh
                                                                        </button>
                                                                    </td>
                                                                </tr>
                                                            </c:forEach>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                    <div id="allProductsTab" class="tab-content">
                                        <div class="control-panel">
                                            <div class="filter-group">
                                                <input type="text" id="catalogSearch"
                                                    placeholder="Tìm kiếm tên truyện..." class="form-input"
                                                    onkeyup="filterCatalog()" style="min-width: 300px;">
                                            </div>
                                        </div>

                                        <div class="table-container">
                                            <table id="catalogTable">
                                                <thead>
                                                    <tr>
                                                        <th>Truyện quyển</th>
                                                        <th>Tổng tồn kho</th>
                                                        <th>Bị hỏng</th>
                                                        <th>Có thể bán</th>
                                                        <th>Điều chỉnh nhanh</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <c:forEach var="comic" items="${allComics}">
                                                        <tr class="catalog-row">
                                                            <td class="catalog-name"
                                                                data-name="${fn:toLowerCase(comic.name_comics)}">
                                                                <div class="comic-info">
                                                                    <h4 style="margin: 0;">
                                                                        ${fn:escapeXml(comic.name_comics)}</h4>
                                                                    <span class="text-muted">Tập: ${comic.volume != null
                                                                        ? comic.volume : '-'}</span>
                                                                </div>
                                                            </td>
                                                            <td><b>${comic.stock_quantity}</b></td>
                                                            <td>${comic.damaged_quantity}</td>
                                                            <td>
                                                                <span
                                                                    class="stock-indicator ${comic.available_quantity <= 10 ? 'stock-low' : 'stock-ok'}">
                                                                    ${comic.available_quantity}
                                                                </span>
                                                            </td>
                                                            <td>
                                                                <div style="display: flex; gap: 8px;">
                                                                    <button class="btn btn-success btn-outline"
                                                                        onclick="openAdjustmentModal(${comic.id}, 'import')"
                                                                        title="Nhập thêm kho">
                                                                        <i class="fa-solid fa-plus"></i> Nhập
                                                                    </button>
                                                                    <button class="btn btn-danger btn-outline"
                                                                        onclick="openAdjustmentModal(${comic.id}, 'export')"
                                                                        title="Xuất kho thủ công">
                                                                        <i class="fa-solid fa-minus"></i> Xuất
                                                                    </button>
                                                                    <button class="btn btn-warning btn-outline"
                                                                        onclick="openAdjustmentModal(${comic.id}, 'damage')"
                                                                        title="Báo sản phẩm hỏng">
                                                                        <i class="fa-solid fa-triangle-exclamation"></i>
                                                                        Hỏng
                                                                    </button>
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    </c:forEach>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>


                                    <div id="historyTab" class="tab-content">
                                        <div class="control-panel">
                                            <div class="filter-group">
                                                <input type="text" id="historySearch" placeholder="Tìm tên truyện..."
                                                    class="form-input" onkeyup="filterHistory()"
                                                    style="min-width: 250px;">
                                                <select id="historyTypeFilter" class="form-select"
                                                    onchange="filterHistory()">
                                                    <option value="all">Tất cả loại giao dịch</option>
                                                    <option value="IMPORT">Nhập kho (IMPORT)</option>
                                                    <option value="EXPORT">Xuất kho (EXPORT)</option>
                                                    <option value="DAMAGE">Báo hỏng (DAMAGE)</option>
                                                </select>
                                            </div>
                                        </div>

                                        <div class="table-container">
                                            <table id="historyTable">
                                                <thead>
                                                    <tr>
                                                        <th>Thời gian</th>
                                                        <th>Truyện quyển</th>
                                                        <th>Loại</th>
                                                        <th>Số lượng</th>
                                                        <th>Mục đích / Ghi chú</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <c:choose>
                                                        <c:when test="${empty transactionHistory}">
                                                            <tr>
                                                                <td colspan="5"
                                                                    style="text-align: center; color: var(--text-muted); padding: 24px;">
                                                                    Chưa có bất kỳ giao dịch kho nào được ghi nhận.
                                                                </td>
                                                            </tr>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:forEach var="trans" items="${transactionHistory}">
                                                                <tr class="history-row" data-type="${trans.type}"
                                                                    data-name="${fn:toLowerCase(trans.name_comics)}">
                                                                    <td>
                                                                        <fmt:parseDate value="${trans.created_at}"
                                                                            pattern="yyyy-MM-dd'T'HH:mm:ss"
                                                                            var="parsedDate" />
                                                                        <fmt:formatDate value="${parsedDate}"
                                                                            pattern="dd/MM/yyyy HH:mm:ss" />
                                                                    </td>
                                                                    <td>
                                                                        <b>${fn:escapeXml(trans.name_comics)}</b>
                                                                        <span
                                                                            style="font-size: 12px; color: var(--text-muted); display: block;">Tập:
                                                                            ${trans.volume != null ? trans.volume :
                                                                            '-'}</span>
                                                                    </td>
                                                                    <td>
                                                                        <span
                                                                            class="badge ${trans.type == 'IMPORT' ? 'badge-import' : (trans.type == 'EXPORT' ? 'badge-export' : 'badge-damage')}">
                                                                            ${trans.type}
                                                                        </span>
                                                                    </td>
                                                                    <td><b>${trans.quantity}</b></td>
                                                                    <td style="color: #374151;">
                                                                        ${fn:escapeXml(trans.note)}</td>
                                                                </tr>
                                                            </c:forEach>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                        </div>
                    </div>

                    <div class="modal-overlay" id="adjustmentModal">
                        <div class="modal-box">
                            <div class="modal-header">
                                <h3 id="modalTitle">Điều chỉnh kho hàng</h3>
                                <button class="modal-close" onclick="closeModal()">&times;</button>
                            </div>
                            <form id="adjustmentForm" onsubmit="submitAdjustment(event)">
                                <div class="modal-body">
                                    <div class="form-group">
                                        <label for="modalComicSelect">Tác phẩm:</label>
                                        <select id="modalComicSelect" name="comicId" class="form-control-full" required>
                                            <option value="">-- Chọn tác phẩm cần điều chỉnh --</option>
                                            <c:forEach var="comic" items="${allComics}">
                                                <option value="${comic.id}">[Tập ${comic.volume != null ? comic.volume :
                                                    '-'}] ${fn:escapeXml(comic.name_comics)} (Tồn khả dụng:
                                                    ${comic.available_quantity})</option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div class="form-group">
                                        <label for="modalActionSelect">Giao dịch:</label>
                                        <select id="modalActionSelect" name="action" class="form-control-full" required>
                                            <option value="import">Nhập thêm kho (IMPORT)</option>
                                            <option value="export">Xuất kho thủ công (EXPORT)</option>
                                            <option value="damage">Báo hỏng sản phẩm (DAMAGE)</option>
                                        </select>
                                    </div>

                                    <div class="form-group">
                                        <label for="modalQuantity">Số lượng:</label>
                                        <input type="number" id="modalQuantity" name="quantity"
                                            class="form-control-full" min="1" required
                                            placeholder="Nhập số lượng lớn hơn 0">
                                    </div>

                                    <div class="form-group">
                                        <label for="modalNote">Ghi chú nguyên nhân:</label>
                                        <textarea id="modalNote" name="note" class="form-control-full"
                                            placeholder="Ví dụ: Nhập thêm từ nhà cung cấp Kim Đồng, Bán lẻ ngoài cửa hàng..."></textarea>
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-outline" onclick="closeModal()">Đóng</button>
                                    <button type="submit" class="btn btn-primary">Xác nhận điều chỉnh</button>
                                </div>
                            </form>
                        </div>
                    </div>

                    <div class="toast-container" id="toastContainer"></div>

                    <script>

                        function switchTab(tabId) {
                            document.querySelectorAll('.tab-content').forEach(content => {
                                content.classList.remove('active');
                            });
                            document.querySelectorAll('.tab-btn').forEach(btn => {
                                btn.classList.remove('active');
                            });

                            document.getElementById(tabId).classList.add('active');

                            const clickedBtn = Array.from(document.querySelectorAll('.tab-btn')).find(btn => {
                                return btn.getAttribute('onclick').includes(tabId);
                            });
                            if (clickedBtn) clickedBtn.classList.add('active');
                        }

                        function openAdjustmentModal(comicId, actionType, defaultQty) {
                            const modal = document.getElementById('adjustmentModal');
                            const selectComic = document.getElementById('modalComicSelect');
                            const selectAction = document.getElementById('modalActionSelect');
                            const inputQty = document.getElementById('modalQuantity');
                            const inputNote = document.getElementById('modalNote');
                            selectComic.value = "";
                            selectAction.value = "import";
                            inputQty.value = "";
                            inputNote.value = "";

                            if (comicId) {
                                selectComic.value = comicId;
                                selectComic.style.pointerEvents = "none";
                                selectComic.style.backgroundColor = "#F3F4F6";
                            } else {
                                selectComic.style.pointerEvents = "auto";
                                selectComic.style.backgroundColor = "#FFF";
                            }

                            if (actionType) {
                                selectAction.value = actionType;
                            }

                            if (defaultQty && defaultQty > 0) {
                                inputQty.value = defaultQty;
                                inputNote.value = "Nhập hàng tự động theo gợi ý tồn kho an toàn";
                            }

                            modal.style.display = "flex";
                        }

                        function closeModal() {
                            document.getElementById('adjustmentModal').style.display = "none";
                        }

                        function submitAdjustment(event) {
                            event.preventDefault();
                            const form = document.getElementById('adjustmentForm');
                            const formData = new FormData(form);
                            const params = new URLSearchParams(formData);

                            fetch('${pageContext.request.contextPath}/admin/inventory', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                body: params
                            })
                                .then(response => response.json())
                                .then(data => {
                                    if (data.success) {
                                        showToast(data.message, 'success');
                                        closeModal();
                                        setTimeout(() => {
                                            location.reload();
                                        }, 1200);
                                    } else {
                                        showToast(data.message, 'error');
                                    }
                                })
                                .catch(error => {
                                    console.error('Error adjusting stock:', error);
                                    showToast('Lỗi máy chủ không thể gửi yêu cầu!', 'error');
                                });
                        }

                        // Toast triggers
                        function showToast(message, type = 'success') {
                            const container = document.getElementById('toastContainer');
                            const toast = document.createElement('div');
                            toast.className = `toast ${type}`;

                            const icon = type === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation';
                            toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;

                            container.appendChild(toast);

                            setTimeout(() => {
                                toast.style.animation = 'toastIn 0.3s ease reverse forwards';
                                setTimeout(() => {
                                    toast.remove();
                                }, 300);
                            }, 3000);
                        }

                        // Search catalog
                        function filterCatalog() {
                            const input = document.getElementById('catalogSearch');
                            const filter = input.value.toLowerCase().trim();
                            const rows = document.querySelectorAll('.catalog-row');

                            rows.forEach(row => {
                                const nameCell = row.querySelector('.catalog-name');
                                const name = nameCell.getAttribute('data-name');
                                if (name.includes(filter)) {
                                    row.style.display = '';
                                } else {
                                    row.style.display = 'none';
                                }
                            });
                        }

                        function filterHistory() {
                            const textInput = document.getElementById('historySearch');
                            const typeInput = document.getElementById('historyTypeFilter');

                            const textFilter = textInput.value.toLowerCase().trim();
                            const typeFilter = typeInput.value;

                            const rows = document.querySelectorAll('.history-row');

                            rows.forEach(row => {
                                const name = row.getAttribute('data-name');
                                const type = row.getAttribute('data-type');

                                const textMatch = name.includes(textFilter);
                                const typeMatch = typeFilter === 'all' || type === typeFilter;

                                if (textMatch && typeMatch) {
                                    row.style.display = '';
                                } else {
                                    row.style.display = 'none';
                                }
                            });
                        }
                        document.addEventListener("DOMContentLoaded", function() {
                            const select = document.getElementById('thresholdSelect');
                            const customContainer = document.getElementById('customThresholdContainer');
                            const customInput = document.getElementById('customThresholdInput');

                            if (select) {
                                select.addEventListener('change', function() {
                                    if (select.value === 'custom') {
                                        customContainer.style.display = 'inline-flex';
                                        customInput.focus();
                                    } else {
                                        customContainer.style.display = 'none';
                                        select.form.submit();
                                    }
                                });
                            }

                            if (customInput) {
                                customInput.addEventListener('keypress', function(e) {
                                    if (e.key === 'Enter') {
                                        e.preventDefault();
                                        submitCustomThreshold();
                                    }
                                });
                            }
                        });

                        function submitCustomThreshold() {
                            const input = document.getElementById('customThresholdInput');
                            const val = parseInt(input.value);
                            if (isNaN(val) || val < 1) {
                                alert('Vui lòng nhập số cuốn hợp lệ (lớn hơn 0)');
                                return;
                            }
                            const form = document.getElementById('thresholdSelect').form;
                            let hiddenInput = form.querySelector('input[name="threshold"]');
                            if (!hiddenInput) {
                                hiddenInput = document.createElement('input');
                                hiddenInput.type = 'hidden';
                                hiddenInput.name = 'threshold';
                                form.appendChild(hiddenInput);
                            }
                            hiddenInput.value = val;
                            document.getElementById('thresholdSelect').name = "";
                            form.submit();
                        }

                        window.onclick = function (event) {
                            const modal = document.getElementById('adjustmentModal');
                            if (event.target === modal) {
                                closeModal();
                            }
                        }
                    </script>
                </body>

                </html>