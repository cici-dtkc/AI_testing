<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Flash Sale</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleFlashSale.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <script>
        const contextPath = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/js/flashSaleMan.js" defer></script>
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <div class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Quản lý Flash Sale</h2>
        <div class="flashSale-page">
            <div class="flashSale-header">
                <div class="search-box">
                    <input type="text" id="searchInput" placeholder="Tìm kiếm tên Flash Sale">
                    <i class="fas fa-magnifying-glass"></i>
                </div>
                <c:choose>
                    <c:when test="${canAdd}">
                        <button class="btn-add" id="openAddPopup">
                            <i class="fas fa-plus"></i> Tạo Flash Sale
                        </button>
                    </c:when>
                    <c:otherwise>
                        <button class="btn-add" disabled title="Bạn không có quyền tạo"
                                style="opacity:0.5; cursor:not-allowed;">
                            <i class="fas fa-plus"></i> Tạo Flash Sale
                        </button>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="flashSale-list">
                <table class="flashSale-table">
                    <thead>
                    <tr>
                        <th>Mã Flash Sale</th>
                        <th>Tên Flash Sale</th>
                        <th>Thời gian</th>
                        <th>Trạng thái</th>
                        <th>Thao tác</th>
                    </tr>
                    </thead>
                    <tbody id="flashSaleTableBody">
                    <c:choose>
                        <c:when test="${empty flashSales}">
                            <tr>
                                <td colspan="5" style="text-align: center; color: #888;">
                                    Chưa có Flash Sale nào được tạo
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="fs" items="${flashSales}">
                                <tr data-id="${fs.id}">
                                    <td>${fs.id}</td>
                                    <td>${fs.name}</td>
                                    <td>
                                            ${fs.startTimeFormatted} → ${fs.endTimeFormatted}
                                    </td>
                                    <td>
                                <span class="status ${fs.status}">
                                    <c:choose>
                                        <c:when test="${fs.status == 'active'}">Đang diễn ra</c:when>
                                        <c:when test="${fs.status == 'scheduled'}">Sắp diễn ra</c:when>
                                        <c:when test="${fs.status == 'ended'}">Đã diễn ra</c:when>
                                        <c:otherwise>${fs.status}</c:otherwise>
                                    </c:choose>
                                </span>
                                    </td>
                                    <td class="action">
                                        <button class="btn-view" data-id="${fs.id}">
                                            <i class="fa-solid fa-eye"></i>
                                        </button>

                                        <c:choose>
                                            <c:when test="${canEdit}">
                                                <button class="btn-edit openEditFlashSale" data-id="${fs.id}"
                                                        data-status="${fs.status}">
                                                    <i class="fa-solid fa-pen-to-square"></i>
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <button class="btn-edit" disabled title="Bạn không có quyền sửa"
                                                        style="opacity:0.5; cursor:not-allowed;">
                                                    <i class="fa-solid fa-pen-to-square"></i>
                                                </button>
                                            </c:otherwise>
                                        </c:choose>

                                        <c:choose>
                                            <c:when test="${canDelete}">
                                                <button class="btn-delete" data-id="${fs.id}">
                                                    <i class="fa-solid fa-trash"></i>
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <button class="btn-delete" disabled title="Bạn không có quyền xóa"
                                                        style="opacity:0.5; cursor:not-allowed;">
                                                    <i class="fa-solid fa-trash"></i>
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                    <tbody id="paginationBody">
                    <tr class="pagination-row">
                        <td colspan="5">
                            <div class="pagination" id="paginationContainer"></div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
<div class="modal-overlay" id="addFlashSaleModal">
    <div class="modal-flashsale">
        <h3>Tạo Flash Sale</h3>
        <form id="addFlashSaleForm">
            <div class="form-row">
                <div>
                    <label>Tên Flash Sale:</label>
                    <input type="text" name="flashSaleName" placeholder="Nhập tên Flash Sale" required>
                </div>
                <div>
                    <label>Phần trăm giảm:</label>
                    <input class="qty-input" type="number" name="discountPercent" value="30" min="1" max="90" required>
                </div>
            </div>
            <div class="form-row">
                <div>
                    <label>Giờ bắt đầu:</label>
                    <input type="datetime-local" name="startTime" required>
                </div>
                <div>
                    <label>Giờ kết thúc:</label>
                    <input type="datetime-local" name="endTime" required>
                </div>
            </div>

            <div class="form-group">
                <label>Thêm theo thể loại</label>
                <div class="category-select-row">
                    <select id="categorySelect">
                        <option value="">-- Áp thể loại --</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}">${cat.nameCategories}</option>
                        </c:forEach>
                    </select>
                    <button type="button" id="addByCategoryBtn">Thêm thể loại</button>
                </div>
            </div>

            <h4>Áp dụng cho truyện:</h4>
            <div class="comic-search-container">
                <input type="text" id="comicSearchInput"
                       placeholder="Tìm truyện theo tên để thêm vào Flash Sale"
                       class="search-input" autocomplete="off">
                <div id="searchResults" class="search-results"></div>
            </div>
            <div class="selected-comics-list" id="selectedProductList">
                <p class="empty-message">Chưa có truyện nào được chọn. Hãy tìm và thêm truyện ở trên.</p>
            </div>
            <div class="flashsale-buttons">
                <button type="button" class="cancel-btn" id="closeAddFlashSale">Hủy</button>
                <button type="button" class="save-btn" id="createFlashSaleBtn">Tạo Flash Sale</button>
            </div>
        </form>
    </div>
</div>

<div class="modal-overlay" id="editFlashSaleModal">
    <div class="modal-flashsale">
        <h3>Chỉnh sửa Flash Sale</h3>
        <form id="editFlashSaleForm">
            <div id="endedNotice" style="display:none; background:#fff3cd; border:1px solid #ffc107;
     color:#856404; padding:10px 14px; border-radius:6px; margin-bottom:14px; font-size:14px;">
                ⚠️ Flash Sale đã kết thúc, không thể chỉnh sửa. Hãy tạo Flash Sale mới.
            </div>
            <div id="activeNotice" style="display:none; background:#d1ecf1; border:1px solid #bee5eb;
     color:#0c5460; padding:10px 14px; border-radius:6px; margin-bottom:14px; font-size:14px;">
                ℹ️ Flash Sale đang diễn ra. Chỉ có thể thêm/bớt truyện, không thể đổi thời gian.
            </div>
            <div class="form-row">
                <div>
                    <label>Tên Flash Sale:</label>
                    <input type="text" name="flashSaleName">
                </div>
                <div>
                    <label>Phần trăm giảm:</label>
                    <input type="number" name="discountPercent" min="1" max="90">
                </div>
            </div>
            <div class="form-row">
                <div>
                    <label>Giờ bắt đầu:</label>
                    <input type="datetime-local" name="startTime">
                </div>
                <div>
                    <label>Giờ kết thúc:</label>
                    <input type="datetime-local" name="endTime">
                </div>
            </div>
            <h4>Truyện đã áp dụng:</h4>
            <div class="comic-search-container">
                <div class="search-with-button">
                    <input type="text" id="editComicSearchInput"
                           placeholder="Tìm truyện theo tên để thêm vào Flash Sale"
                           class="search-input" autocomplete="off">
                    <button type="button" id="applySelectedComicBtn" class="apply-comic-btn">
                        <i class="fas fa-plus"></i> Áp dụng
                    </button>
                </div>
                <div id="editSearchResults" class="search-results">
                </div>
            </div>

            <div class="product-select-list" id="editProductList">
            </div>

            <div class="flashsale-buttons">
                <button type="button" class="cancel-btn" id="closeEditFlashSale">Hủy</button>
                <button type="button" class="save-btn" id="updateFlashSaleBtn">Cập nhật</button>
            </div>
        </form>
    </div>
</div>
<div class="modal-overlay" id="viewFlashSaleModal">
    <div class="modal-flashsale">
        <h3>Chi tiết Flash Sale</h3>
        <div class="view-info-box">
            <p><strong>Mã Flash Sale:</strong> FS001</p>
            <p><strong>Tên Flash Sale:</strong> Flash Sale 10:00 - Mừng Noel</p>
            <p><strong>Thời gian:</strong> 10:00 24/12/2024 → 12:00 24/12/2024</p>
            <p><strong>Trạng thái:</strong> <span class="status active">Đang diễn ra</span></p>
            <h4>Sản phẩm áp dụng:</h4>
            <div class="view-product-list">
                <div class="item">
                    <span>Thám tử lừng danh Conan – Tập 12 – Gosho Aoyama</span>
                    <span>Giảm: 40%</span>
                </div>
                <div class="item">
                    <span>One Piece – Tập 100 – Eiichiro Oda</span>
                    <span>Giảm: 50%</span>
                </div>
            </div>
            <div class="flashsale-buttons">
                <button type="button" class="cancel-btn" id="closeViewFlashSale">Đóng</button>
            </div>
        </div>
    </div>
</div>
<div class="modal-overlay" id="deleteFlashSaleModal">
    <div class="modal-delete">
        <h3>Xác nhận xóa</h3>
        <p>Bạn có chắc muốn xóa Flash Sale này không?</p>
        <div class="delete-buttons">
            <button class="cancel-btn" id="closeDeleteFlashSale">Hủy</button>
            <button class="delete-confirm-btn" id="confirmDeleteFlashSale">Xóa</button>
        </div>
    </div>
</div>

<script>
    (function () {
        var PAGE_GROUP_SIZE = 3;
        var TOTAL_PAGES = parseInt('${totalPages}') || 1;
        var CURRENT_PAGE = parseInt('${currentPage}') || 1;
        var BASE_URL = '${pageContext.request.contextPath}/admin/manage-flashsale';

        function buildUrl(page) {
            return BASE_URL + '?page=' + page;
        }

        function renderPagination() {
            var container = document.getElementById('paginationContainer');
            var paginationBody = document.getElementById('paginationBody');
            if (!container) return;
            // Nếu chỉ có 1 trang duy nhất, ẩn phần phân trang đi
            if (TOTAL_PAGES <= 1) {
                if (paginationBody) paginationBody.style.display = 'none';
                return;
            }
            var currentGroup = Math.floor((CURRENT_PAGE - 1) / PAGE_GROUP_SIZE);
            var startPage = currentGroup * PAGE_GROUP_SIZE + 1;
            var endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, TOTAL_PAGES);
            var hasPrev = startPage > 1;
            var hasNext = endPage < TOTAL_PAGES;
            var html = '';
            if (hasPrev) {
                html += '<button class="page-btn nav-btn" onclick="window.location.href=\'' + buildUrl(startPage - 1) + '\'">&laquo;</button>';
            } else {
                html += '<button class="page-btn nav-btn" disabled>&laquo;</button>';
            }
            for (var i = startPage; i <= endPage; i++) {
                var activeClass = (i === CURRENT_PAGE) ? ' active' : '';
                html += '<button class="page-btn' + activeClass + '" onclick="window.location.href=\'' + buildUrl(i) + '\'">' + i + '</button>';
            }
            if (hasNext) {
                html += '<button class="page-btn nav-btn" onclick="window.location.href=\'' + buildUrl(endPage + 1) + '\'">&raquo;</button>';
            } else {
                html += '<button class="page-btn nav-btn" disabled>&raquo;</button>';
            }
            container.innerHTML = html;
        }

        document.addEventListener('DOMContentLoaded', renderPagination);
    })();
</script>

</body>
</html>