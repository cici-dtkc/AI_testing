<%--<jsp:useBean id="name" scope="request" type="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Category"/>--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Khuyến mãi</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/stylePromotion.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <div class="promotion-page">
            <div class="promo-top">
                <div class="search-box">
                    <input type="text" id="searchInput" placeholder="Tìm kiếm mã...">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
                <button class="btn-add" id="openAddPopup">
                    <i class="fas fa-plus"></i> Thêm mã
                </button>
                <select id="statusFilter">
                    <option value="">Tất cả trạng thái</option>
                    <option value="running">Đang chạy</option>
                    <option value="expired">Hết hạn</option>
                    <option value="out">Hết lượt</option>
                </select>
            </div>
            <table class="promo-table">
                <thead>
                <tr>
                    <th>Mã</th>
                    <th>Giá trị</th>
                    <th>Áp dụng</th>
                    <th>Thể loại</th>
                    <th>Dùng/Tổng</th>
                    <th>Giá tối thiểu</th>
                    <th>Lần dùng</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Trạng thái</th>
                    <th></th>
                </tr>
                </thead>
                <tbody id="promoTableBody">

                <c:forEach var="voucher" items="${allVouchers}">
                    <tr>
                        <td>${voucher.code}</td>
                        <td>${voucher.discountValue}</td>
                        <td>${voucher.discountTarget}</td>
                        <td>${voucher.applyScope}</td>
                        <td>${voucher.usedCount}/${voucher.quantity}</td>
                        <td>${voucher.minOrderAmount}</td>

                        <c:choose>
                            <c:when test="${voucher.singleUse}">
                                <td>Một lần</td>
                            </c:when>
                            <c:otherwise>
                                <td>Nhiều lần</td>
                            </c:otherwise>
                        </c:choose>

                        <td>${voucher.startDate}</td>
                        <td>${voucher.endDate}</td>

                        <c:choose>
                            <c:when test="${voucher.expired}">
                                <td><span class="status-out">Hết Hạn</span></td>
                            </c:when>
                            <c:when test="${voucher.usedCount == voucher.quantity}">
                                <td><span class="status-out">Hết lượt</span></td>
                            </c:when>

                            <c:otherwise>
                                <td><span class="status-active">Đang chạy</span></td>
                            </c:otherwise>
                        </c:choose>

                        <td>
                            <button class="edit-btn" onclick="openEdit('${voucher.code}')"><i class="fa-solid fa-pen-to-square"></i></button>
                            <button class="delete-btn" onclick="openDeletePopup('${voucher.code}')">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                </c:forEach>

                <!-- Phân trang -->
                <tr class="pagination-row">
                    <td colspan="10">
                        <div class="pagination" id="tablePagination">
                            <button class="page-btn pro-page" data-page="1">1</button>
                            <button class="page-btn pro-page" data-page="2">2</button>
                            <button class="page-btn pro-page" data-page="3">3</button>
                        </div>
                    </td>
                </tr>
                </tbody>
            </table>

        </div>
    </div>
</div>
<div class="popup-overlay" id="addPopup">
    <div class="popup-box">
        <form action="${pageContext.request.contextPath}/admin/vouchers" method="post">
            <h3>Thêm mã khuyến mãi</h3>

            <div class="popup-grid">
                <!-- Dòng 1: Mã + Loại -->
                <div>
                    <label>Tên mã</label>
                    <input type="text" id="addCode" placeholder="Nhập mã" maxlength="20" name="code">
                </div>
                <div>
                    <label>Áp dụng</label>
                    <select id="addTarget" name="discount_target"  required>
                        <option value="Vận chuyển">Vận chuyển</option>
                        <option value="Giảm giá">Giảm giá</option>
                    </select>
                </div>
                <div>
                    <label>Loại</label>
                    <select id="addType"  name="discount_type" required>
                        <option value="">Chọn loại</option>
                        <option value="percent">Phần trăm (%)</option>
                        <option value="fixed">Số tiền (₫)</option>
                    </select>
                </div>

                <!-- Dòng 2: Giá trị + Đơn tối thiểu -->
                <div>
                    <label>Giá trị giảm</label>
                    <input type="number" id="addValue" min="0" name="discount_value" placeholder="10 hoặc 20000">
                </div>
                <div>
                    <label>Đơn tối thiểu</label>
                    <input type="number" id="addMinOrder" min="0" name="min_order_amount" placeholder="0" value="0">
                </div>

                <!-- Dòng 3: Số lượng + Áp dụng cho -->
                <div>
                    <label>Số lượng</label>
                    <input type="number" id="addMaxUsage" name="quantity" min="1" value="100">
                </div>
                <div>
                    <label>Áp dụng</label>
                    <select id="addApply" name="apply_scope">
                        <option value="Tất cả">Toàn bộ sản phẩm</option>
                        <option value="category">Một thể loại</option>
                    </select>
                </div>

                <!-- Checkbox 1 lần/khách -->
                <div class="checkbox-row">
                    <input type="checkbox" id="addSingleUse" name="is_single_use">
                    <label for="addSingleUse">Mỗi khách chỉ dùng 1 lần</label>
                </div>

                <!-- Chọn thể loại (ẩn mặc định) -->
                <div id="addCategoryBox" style="grid-column: 1 / -1;  display: none;">
                    <label for="addCategory" >Thể loại</label>
                    <select id="addCategory" name="cate" disabled>  </select>
                </div>

                <!-- Ngày bắt đầu + kết thúc -->
                <div class="date-row">
                    <div>
                        <label>Từ ngày</label>
                        <input type="datetime-local" id="addStart" name="start_date">
                    </div>
                    <div>
                        <label>Đến ngày</label>
                        <input type="datetime-local" id="addEnd" name="end_date">
                    </div>
                </div>
            </div>

            <div class="btn-row">
                <button class="btn-cancel" id="closeAddPopup">Hủy</button>
                <button type="submit" class="btn-save" id="saveAddBtn">Tạo mã</button>
            </div>
        </form>
    </div>
</div>
<div class="popup-overlay" id="editPopup">
    <form action="${pageContext.request.contextPath}/admin/editVoucher" method="POST" >
        <div class="popup-box">
            <h3>Sửa mã khuyến mãi</h3>
            <div class="popup-grid">
                <div>
                    <label>Tên mã</label>
                    <input type= 'text'  id="editCode"  name="code" class="readonly">
                </div>
                <div>
                    <label>Loại giảm giá</label>
                    <input type= 'text'id="editTypeDisplay"  name="" class="readonly">
                </div>
                <div>
                    <label>Giá trị giảm</label>
                    <input type= 'text'  id="editValueDisplay"  name="" class="readonly">
                </div>
                <div>
                    <label>Áp dụng cho</label>
                    <input type= 'text' id="editApplyDisplay"  name="" class="readonly">
                </div>
                <div>
                    <label>Đơn tối thiểu</label>
                    <input type="number" id="editMinOrder" min="0"  name="minOrder" placeholder="0">
                </div>
                <div>
                    <label>Số lượng tối đa</label>
                    <input type="number" id="editMaxUsage"  name="quantity" min="1">
                </div>
                <div class="checkbox-row">
                    <input type="checkbox" id="editSingleUse"  name="">
                    <label for="editSingleUse">Mỗi khách chỉ dùng được 1 lần</label>
                </div>
                <div class="date-row">
                    <div>
                        <label>Từ ngày</label>
                        <input type="datetime-local" id="editStart"  name="startDate">
                    </div>
                    <div>
                        <label>Đến ngày</label>
                        <input type="datetime-local" id="editEnd" name="endDate">
                    </div>
                </div>
            </div>

            <div class="btn-row">
                <button class="btn-cancel" id="closeEditPopup" type="button">Hủy</button>
                <button class="btn-save" id="saveEditBtn"type="submit">Cập nhật</button>
            </div>
        </div>
    </form>

</div>
<div class="popup-overlay" id="deleteConfirmPopup">
    <form  action="${pageContext.request.contextPath}/admin/deleteVoucher" method="POST">

        <input type="hidden" name="deleted-code" id="deleteCode">

        <div class="popup-box small">
            <i class="fa-solid fa-triangle-exclamation"></i>
            <h3>Xác nhận xóa</h3>
            <p>Bạn có chắc muốn xóa mã <b id="deleteVoucherCode"></b> này không?</p>
            <div class="btn-row">
                <button class="btn-cancel" id="cancelDeleteBtn" type ="button">Không</button>
                <button class="btn-save" id="confirmDeleteBtn" type="submit">Có</button>
            </div>
        </div>
    </form>
</div>

<%--<c:if test = "${not empty message}" >--%>
<%--    <script type="text/javascript">--%>
<%--        window.onload = function () {--%>
<%--            setTimeout(() => {--%>
<%--                alert("${message}");--%>
<%--            }, 300);--%>
<%--        }--%>
<%--    </script>--%>
<%--</c:if>--%>
<div class="popup-overlay" id="notifyPopup" style="display:none; z-index:9999;">
    <div class="popup-box small" style="text-align:center; max-width:380px;">
        <i id="notifyIcon" class="fa-solid fa-circle-check" style="font-size:2.5rem; margin-bottom:12px;"></i>
        <h3 id="notifyTitle" style="margin:0 0 8px;">Thông báo</h3>
        <p id="notifyMessage" style="margin:0 0 20px; color:#555;"></p>
        <div class="btn-row" style="justify-content:center;">
            <button class="btn-save" id="notifyOkBtn">OK</button>
        </div>
    </div>
</div>

<script>
    function showNotify(message, type = 'success') {
        const popup   = document.getElementById('notifyPopup');
        const icon    = document.getElementById('notifyIcon');
        const title   = document.getElementById('notifyTitle');
        const msg     = document.getElementById('notifyMessage');
        icon.className = '';
        if (type === 'success') {
            icon.className = 'fa-solid fa-circle-check success';
            title.textContent = 'Thành công';
        } else if (type === 'error') {
            icon.className = 'fa-solid fa-circle-xmark error';
            title.textContent = 'Lỗi';
        } else {
            icon.className = 'fa-solid fa-triangle-exclamation warning';
            title.textContent = 'Cảnh báo';
        }
        msg.textContent = message;
        popup.style.display = 'flex';

        document.getElementById('notifyOkBtn').onclick = () => {
            popup.style.display = 'none';
        };
    }
    <c:if test="${not empty message}">
    window.addEventListener('DOMContentLoaded', function () {
        setTimeout(() => {
            const msg = "${message}";
            const type = msg.toLowerCase().includes("thành công") || msg.toLowerCase().includes("thêm") || msg.toLowerCase().includes("xóa") || msg.toLowerCase().includes("cập nhật")
                ? 'success' : 'error';
            showNotify(msg, type);
        }, 300);
    });
    </c:if>
    document.addEventListener("DOMContentLoaded", function () {
        const addPopup    = document.getElementById('addPopup');
        const tbody       = document.getElementById('promoTableBody');
        const editPopup   = document.getElementById('editPopup');
        const deletedPopup = document.getElementById('deleteConfirmPopup');
        document.getElementById('openAddPopup').onclick = () => {
            addPopup.style.display = 'flex';
            document.getElementById('addCode').value = '';
            document.getElementById('addType').value = '';
            document.getElementById('addValue').value = '';
            document.getElementById('addMinOrder').value = '0';
            document.getElementById('addMaxUsage').value = '100';
            document.getElementById('addSingleUse').checked = false;
            document.getElementById('addStart').value = '';
            document.getElementById('addEnd').value = '';
            document.getElementById('addCategoryBox').style.display = 'none';
            document.getElementById('addCategory').disabled = true;
            document.getElementById('addCategory').value = '';
        };
        document.getElementById('closeAddPopup').onclick = () => addPopup.style.display = 'none';
        document.getElementById('cancelDeleteBtn').onclick = () => deletedPopup.style.display = 'none';
        window.openEdit = function(code) {
            const rows = Array.from(tbody.querySelectorAll('tr')).filter(r => !r.classList.contains('pagination-row'));
            const row  = rows.find(r => r.cells[0].textContent.trim() === code);
            if (!row) return showNotify('Không tìm thấy mã!', 'warning');
            document.getElementById('editCode').value        = row.cells[0].textContent.trim();
            document.getElementById('editValueDisplay').value = row.cells[1].textContent.trim();
            document.getElementById('editTypeDisplay').value  = row.cells[2].textContent.trim();
            document.getElementById('editApplyDisplay').value = row.cells[3].textContent.trim();
            const minOrder = row.cells[4].textContent.replace(/[^\d]/g, '');
            document.getElementById('editMinOrder').value    = minOrder || 0;
            const usage    = row.cells[3].textContent.trim();
            const maxUsage = usage.split('/')[1]?.trim() || '100';
            document.getElementById('editMaxUsage').value    = maxUsage.replace(/\D/g, '');
            document.getElementById('editEnd').value         = row.cells[8].textContent.trim();
            document.getElementById('editSingleUse').checked = false;
            document.getElementById('editStart').value       = row.cells[7].textContent.trim();
            editPopup.style.display = 'flex';
        };
        document.getElementById('closeEditPopup').onclick = () => editPopup.style.display = 'none';
    });
    (function(){
        let currentPage = 1;
        const ROWS_PER_PAGE = 5;
        const tbody = document.getElementById('promoTableBody');
        const rows  = Array.from(tbody.querySelectorAll('tr')).filter(r => !r.classList.contains('pagination-row'));
        const pageButtons = document.querySelectorAll('.pro-page');

        function showPage(page){
            const start = (page - 1) * ROWS_PER_PAGE;
            const end   = start + ROWS_PER_PAGE;
            rows.forEach((r, idx) => {
                r.style.display = (idx >= start && idx < end) ? "" : "none";
            });
            pageButtons.forEach(btn => btn.classList.remove('active'));
            document.querySelector(`.pro-page[data-page="${page}"]`)?.classList.add('active');
            currentPage = page;
        }
        pageButtons.forEach(btn => {
            btn.addEventListener('click', () => showPage(Number(btn.dataset.page)));
        });
        showPage(1);
    })();
    document.addEventListener("DOMContentLoaded", function () {
        const current = window.location.pathname.split("/").pop();
        document.querySelectorAll(".sidebar li a").forEach(link => {
            if (link.getAttribute("href") === current) link.classList.add("active");
        });
    });
    document.getElementById("addApply").addEventListener("change", function () {
        const value          = this.value;
        const categorySelect = document.getElementById("addCategory");
        const categoryBox    = document.getElementById("addCategoryBox");
        if (value === "category") {
            categorySelect.disabled = false;
            categorySelect.style.pointerEvents = 'auto';
            categoryBox.style.display = 'block';
            fetch("${pageContext.request.contextPath}/admin/loadCategory")
                .then(res => res.json())
                .then(data => {
                    categorySelect.innerHTML = '';
                    data.forEach(name => {
                        const opt = document.createElement('option');
                        opt.value = name;
                        opt.textContent = name;
                        categorySelect.appendChild(opt);
                    });
                });
        } else {
            categorySelect.disabled = true;
            categorySelect.style.pointerEvents = 'none';
            categorySelect.value = '';
            categoryBox.style.display = 'none';
        }
    });
    document.addEventListener("DOMContentLoaded", function() {
        const form = document.querySelector("#addPopup .popup-box form");
        if (!form) return;
        form.addEventListener("submit", function(event) {
            const code= document.getElementById("addCode").value.trim();
            const discountTarget = document.getElementById("addTarget").value;
            const discountType = form.querySelector("select[name='discount_type']").value;
            const discountValue = document.getElementById("addValue").value;
            const minOrder= document.getElementById("addMinOrder").value;
            const quantity      = document.getElementById("addMaxUsage").value;
            const scope         = document.getElementById("addApply").value;
            const category      = document.getElementById("addCategory").value;
            const startDate     = document.getElementById("addStart").value;
            const endDate       = document.getElementById("addEnd").value;

            let errors = [];

            if (!code)                                               errors.push("Tên mã không được để trống.");
            if (!discountTarget)                                     errors.push("Phải chọn mục áp dụng.");
            if (!discountType)                                       errors.push("Phải chọn loại giảm giá.");
            if (!discountValue || discountValue <= 0)                errors.push("Giá trị giảm phải > 0.");
            if (minOrder < 0)                                        errors.push("Đơn tối thiểu không hợp lệ.");
            if (!quantity || quantity <= 0)                          errors.push("Số lượng phải > 0.");
            if (scope === "category" && !category)                   errors.push("Phải chọn thể loại khi áp dụng theo thể loại.");
            if (!startDate)                                          errors.push("Phải chọn ngày bắt đầu.");
            if (!endDate)                                            errors.push("Phải chọn ngày kết thúc.");
            if (startDate && endDate && new Date(startDate) >= new Date(endDate))
                errors.push("Ngày kết thúc phải sau ngày bắt đầu.");

            if (errors.length > 0) {
                event.preventDefault();
                showNotify(errors.join('\n'), 'error');
            }
        });
    });

    /* ===== Popup xóa ===== */
    function openDeletePopup(code) {
        document.getElementById("deleteVoucherCode").textContent = code;
        const input = document.getElementById("deleteCode");
        if (input) input.value = code;
        document.getElementById("deleteConfirmPopup").style.display = "flex";
    }
</script>



</body>
</html>