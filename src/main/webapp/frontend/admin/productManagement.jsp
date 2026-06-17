<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:useBean id="categoryDAO" class="vn.edu.hcmuaf.fit.ttltw_nhom6.dao.CategoriesDao"/>
<jsp:useBean id="seriesDAO" class="vn.edu.hcmuaf.fit.ttltw_nhom6.dao.SeriesDAO"/>
<c:set var="categories" value="${categoryDAO.allCategories}"/>
<c:set var="seriesList" value="${seriesDAO.allSeries}"/>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Quản lý sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/stylePro.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/styleSidebar.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/adminCss/adminHeader.css">
</head>
<body>
<div class="container">
    <jsp:include page="/frontend/admin/ASide.jsp"/>
    <main class="main-content">
        <%@ include file="HeaderAdmin.jsp" %>
        <h2 class="page-title">Quản lý sản phẩm</h2>
        <div class="table-container">
            <div class="search-add">
                <div class="search-box">
                    <input type="text" id="mainSearchInput" name="keyword" placeholder="Tìm kiếm truyện..."
                           value="${param.keyword}">
                    <button type="submit" onclick="searchProducts()">
                        <i class="fas fa-magnifying-glass"></i>
                    </button>
                </div>
                <select id="displayFilter" class="filter-select">
                    <option value="all">Tất cả sản phẩm</option>
                    <option value="visible">Sản phẩm đang hiển thị</option>
                    <option value="hidden">Sản phẩm đang bị ẩn</option>
                </select>
                <div class="action-buttons">
                    <button class="add-btn">+ Thêm truyện</button>
                </div>
            </div>
        </div>
        <div class="table-wrapper">
            <table>
                <thead>
                <tr>
                    <th>Mã truyện</th>
                    <th>Tên truyện</th>
                    <th>Bộ truyện</th>
                    <th>Thể loại</th>
                    <th>Tác giả</th>
                    <th>Giá</th>
                    <th>Còn trong kho</th>
                    <th></th>
                </tr>
                </thead>
                <tbody id="productTableBody">
                </tbody>
                <tbody id="paginationBody">
                <tr class="pagination-row">
                    <td colspan="10">
                        <div class="pagination" id="paginationContainer"></div>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="modal-overlay" id="addModal">
            <div class="modal-rectangle">
                <h3>Thêm truyện mới</h3>
                <form id="addForm" class="form-horizontal" enctype="multipart/form-data">
                    <div class="form-left">
                        <div class="form-group">
                            <label>Tên truyện: <span style="color: red;">*</span></label>
                            <input type="text" name="nameComics" required>
                        </div>
                        <div class="form-group">
                            <label>Bộ truyện:</label>
                            <select name="seriesId">
                                <option value="" selected>-- Chọn bộ truyện --</option>
                                <c:forEach var="series" items="${seriesList}">
                                    <option value="${series.id}">${series.seriesName}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Thể loại: <span style="color: red;">*</span></label>
                            <select name="categoryId" required>
                                <option value="" selected disabled>-- Chọn thể loại --</option>
                                <c:forEach var="category" items="${categories}">
                                    <option value="${category.id}">${category.nameCategories}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-group two-columns">
                            <div>
                                <label>Số lượng: <span style="color: red;">*</span></label>
                                <input type="number" name="stockQuantity" min="0" value="0" required>
                            </div>
                            <div>
                                <label>Giá bán: <span style="color: red;">*</span></label>
                                <input type="text" name="price" placeholder="VD: 25,000" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Tác giả: <span style="color: red;">*</span></label>
                            <input type="text" name="author" required>
                        </div>
                        <div class="form-group">
                            <label>Nhà xuất bản:</label>
                            <input type="text" name="publisher">
                        </div>
                        <div class="form-group two-columns">
                            <div style="flex: 0.9">
                                <label>Ngày đăng:</label>
                                <input type="date">
                            </div>
                            <div style="flex: 1">
                                <label>Tập:</label>
                                <input type="number" name="volume" min="1" placeholder="VD: 1">
                            </div>
                        </div>
                        <div class="form-group two-columns">
                            <div>
                                <label>Giá Nhập: <span style="color: red;">*</span></label>
                                <input type="text" name="price-import" placeholder="VD: 25,000" required>
                            </div>
                        </div>
                    </div>
                    <div class="form-right">
                        <div class="images-grid">
                            <div class="image-upload">
                                <input type="file" class="imgInput" name="coverImage" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Ảnh bìa</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="imgInput" name="detailImage1" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang chi tiết 1</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="imgInput" name="detailImage2" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang chi tiết 2</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="imgInput" name="detailImage3" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang chi tiết 3</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Mô tả ngắn:</label>
                            <textarea name="description" rows="6" placeholder="Nhập mô tả ngắn..."></textarea>
                        </div>
                    </div>
                </form>
                <div class="form-buttons">
                    <button type="button" class="save-btn">Lưu</button>
                    <button type="button" class="cancel-btn">Hủy</button>
                </div>
            </div>
        </div>
        <div class="modal-overlay" id="editModal">
            <div class="modal-rectangle">
                <h3>Chỉnh sửa truyện</h3>
                <form id="editForm" class="form-horizontal">
                    <div class="form-left">
                    </div>
                    <div class="form-right">
                        <div class="images-grid">
                            <div class="image-upload">
                                <input type="file" class="editImgInput" name="coverImage" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Ảnh bìa</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="editImgInput" name="detailImage1" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang 1</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="editImgInput" name="detailImage2" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang 2</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                            <div class="image-upload">
                                <input type="file" class="editImgInput" name="detailImage3" accept="image/*" hidden>
                                <div class="img-box">
                                    <i class="icon">+</i>
                                    <span class="label">Trang 3</span>
                                    <img class="imgPreview">
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label>Mô tả ngắn:</label>
                            <textarea name="description" rows="6" placeholder="Nhập mô tả..."></textarea>
                        </div>
                    </div>
                </form>
                <div class="form-buttons">
                    <button type="button" class="cancel-btn">Hủy</button>
                    <button type="button" class="save-btn">Cập nhật</button>
                </div>
            </div>
        </div>
        <div class="modal-overlay" id="confirmDeleteModal">
            <div class="confirm-box">
                <p>Bạn có chắc muốn xóa truyện không?</p>
                <div class="confirm-buttons">
                    <button class="cancel-btn">Hủy</button>
                    <button class="delete-confirm-btn">Xác nhận</button>
                </div>
            </div>
        </div>
        <div class="modal-overlay review-popup" id="review-TT001">
            <div class="modal-rectangle">
                <div class="review-list">
                    <div class="review-item">
                        <p><strong>Nguyễn Văn A</strong> -
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                        </p>
                        <p class="review-date">08/11/2024</p>
                        <p>Shop đóng gói cẩn thận, bọc nilon và seal đều rất chu đáo, còn bọc thêm chống sốc và bìa cứng
                            để truyện không bị hư hại gì. Nhất định sẽ mua lại ạ :3</p>
                        <div>
                            <img src="${pageContext.request.contextPath}/img/review1.png" alt="review">
                            <img src="${pageContext.request.contextPath}/img/review2.png" alt="review">
                            <img src="${pageContext.request.contextPath}/img/review3.png" alt="review">
                        </div>
                    </div>
                    <div class="review-item">
                        <p><strong>Trần Thị B</strong> -
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i></p>
                        <p class="review-date">08/11/2024</p>
                        <p>Shop giao hàng nhanh chóng gói hàng cũng kỹ lưỡng nha chu đáo nhắn tin với khách.</p>
                        <div>
                            <img src="${pageContext.request.contextPath}/img/review4.png" alt="review">
                        </div>
                    </div>
                    <div class="review-item">
                        <p><strong>Trần Thị B</strong> -
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i>
                            <i class="fa-solid fa-star"></i></p>
                        <p class="review-date">08/11/2024</p>
                        <p>Shop giao hàng nhanh chóng gói hàng cũng kỹ lưỡng nha chu đáo nhắn tin với khách.</p>
                        <div>
                            <img src="${pageContext.request.contextPath}/img/review4.png" alt="review">
                        </div>
                    </div>
                </div>
                <div class="form-buttons">
                    <button type="button" class="cancel-btn close-review-btn">Đóng</button>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/addComic.js"></script>
<script src="${pageContext.request.contextPath}/js/editComic.js"></script>
<script src="${pageContext.request.contextPath}/js/deleteComic.js"></script>
<script>
    let currentPage = 1;
    let currentFilter = 'all';
    function searchProducts(page = 1) {
        const keyword = document.getElementById('mainSearchInput').value.trim();
        const filterSelect = document.getElementById('displayFilter');
        const filterValue = filterSelect.value; // 'all', 'visible', 'hidden'
        currentFilter = filterValue;
        const tbody = document.getElementById('productTableBody');
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px;">' +
            '<i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>' +
            '<p style="margin-top: 10px;">Đang tìm kiếm...</p></td></tr>';
        let hiddenFilter = null;
        if (filterValue === 'visible') {
            hiddenFilter = 0;
        } else if (filterValue === 'hidden') {
            hiddenFilter = 1;
        }
        let url = contextPath + '/admin/products/search?keyword=' +
            encodeURIComponent(keyword) + '&page=' + page;
        if (hiddenFilter !== null) {
            url += '&hiddenFilter=' + hiddenFilter;
        }
        fetch(url)
            .then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.json();
            })
            .then(data => {
                console.log('Data received:', data);
                currentPage = data.currentPage;
                updateTable(data.comics);
                updatePagination(data.currentPage, data.totalPages);
                bindEventListeners();
            })
            .catch(error => {
                console.error('Error:', error);
                tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #f44336;">' +
                    '<i class="fas fa-exclamation-triangle" style="font-size: 32px;"></i>' +
                    '<p style="margin-top: 10px;">Có lỗi xảy ra: ' + error.message + '</p></td></tr>';
            });
    }
    function toggleHidden(id, hidden) {
        console.log('toggleHidden called:', id, hidden); // Debug log

        fetch(contextPath + '/admin/products/toggle-hidden', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'id=' + id + '&hidden=' + hidden
        })
            .then(response => response.json())
            .then(data => {
                console.log('Response:', data); // Debug log

                if (data.success) {
                    console.log(' Toggle success:', data.message);
                    alert(data.message);
                    searchProducts(currentPage);
                } else {
                    alert('Lỗi: ' + data.message);
                }
            })
            .catch(error => {
                console.error('Error toggling hidden:', error);
                alert('Có lỗi xảy ra khi cập nhật trạng thái');
            });
    }
    function updateTable(comics) {
        const tbody = document.getElementById('productTableBody');
        if (!comics || comics.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #999;">' +
                '<i class="fas fa-inbox" style="font-size: 48px; display: block; margin-bottom: 10px;"></i>' +
                '<p style="margin: 0; font-size: 16px;">Không tìm thấy truyện nào</p></td></tr>';
            return;
        }
        let html = '';
        comics.forEach(function (comic) {
            html += '<tr>' +
                '<td>' + comic.id + '</td>' +
                '<td>' + comic.nameComics + '</td>' +
                '<td>' + (comic.seriesName || '-') + '</td>' +
                '<td>' + (comic.categoryName || 'Chưa phân loại') + '</td>' +
                '<td>' + (comic.author || '-') + '</td>' +
                '<td>' + formatPrice(comic.price) + ' đ</td>' +
                '<td>' + comic.stockQuantity + ' quyển</td>' +
                '<td class="review-cell">' +
                '</td>' +
                '<td class="action-cell">' +
                '<button class="edit-btn" data-comic-id="' + comic.id + '" title="Chỉnh sửa">' +
                '<i class="fa-solid fa-pen-to-square"></i>' +
                '</button>' +
                '<button class="trash-btn" data-comic-id="' + comic.id + '" title="Xóa truyện">' +
                '<i class="fa-solid fa-trash"></i>' +
                '</button>' +
                '<div class="menu-container">' +
                '<button class="more-btn">⋮</button>' +
                '<div class="dropdown-menu">' +
                '<label>' +
                '<input type="radio" name="display-' + comic.id + '" ' +
                (comic.isHidden === 0 ? 'checked' : '') +
                ' onchange="toggleHidden(' + comic.id + ', 0)">' +
                ' Hiển thị' +
                '</label>' +
                '<label>' +
                '<input type="radio" name="display-' + comic.id + '" ' +
                (comic.isHidden === 1 ? 'checked' : '') +
                ' onchange="toggleHidden(' + comic.id + ', 1)">' +
                ' Ẩn sản phẩm' +
                '</label>' +
                '</div>' +
                '</div>' +
                '</td>' +
                '</tr>';
        });
        tbody.innerHTML = html;
    }
    const PAGE_GROUP_SIZE = 3;
    function getPageGroup(page) {
        return Math.floor((page - 1) / PAGE_GROUP_SIZE);
    }
    function updatePagination(currentPage, totalPages) {
        const paginationContainer = document.getElementById('paginationContainer');
        const paginationBody = document.getElementById('paginationBody');
        if (totalPages <= 1) {
            paginationBody.style.display = 'none';
            return;
        }
        paginationBody.style.display = '';
        const currentGroup = getPageGroup(currentPage);
        const startPage = currentGroup * PAGE_GROUP_SIZE + 1;
        const endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);
        const hasPrev = startPage > 1;
        const hasNext = endPage < totalPages;
        let html = '';
        if (hasPrev) {
            const prevGroupLastPage = startPage - 1;
            html += '<button class="page-btn nav-btn" onclick="searchProducts(' + prevGroupLastPage + ')" title="Nhóm trang trước">&laquo;</button>';
        } else {
            html += '<button class="page-btn nav-btn" disabled title="Nhóm trang trước">&laquo;</button>';
        }
        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === currentPage ? 'active' : '';
            html += '<button class="page-btn ' + activeClass + '" onclick="searchProducts(' + i + ')">' + i + '</button>';
        }
        if (hasNext) {
            const nextGroupFirstPage = endPage + 1;
            html += '<button class="page-btn nav-btn" onclick="searchProducts(' + nextGroupFirstPage + ')" title="Nhóm trang tiếp">&raquo;</button>';
        } else {
            html += '<button class="page-btn nav-btn" disabled title="Nhóm trang tiếp">&raquo;</button>';
        }
        paginationContainer.innerHTML = html;
    }
    function formatPrice(price) {
        return new Intl.NumberFormat('vi-VN').format(price);
    }
    function bindEventListeners() {
        document.querySelectorAll('.view-review-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const comicId = this.dataset.comic;
                const popup = document.getElementById(`review-${comicId}`);
                if (popup) {
                    popup.style.display = 'flex';
                } else {
                    alert('Popup review cho truyện ID ' + comicId + ' chưa được tạo!');
                }
            });
        });
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const comicId = this.dataset.comicId;
                console.log('Edit comic ID:', comicId);
                document.getElementById('editModal').style.display = 'flex';
            });
        });
        document.querySelectorAll('.trash-btn').forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                const comicId = this.dataset.comicId;
                const comicRow = this.closest('tr');
                const comicName = comicRow.querySelector('td:nth-child(2)').textContent;
                if (typeof window.showDeleteConfirmation === 'function') {
                    window.showDeleteConfirmation(comicId, comicName);
                } else {
                    console.error('showDeleteConfirmation not found!');
                    const confirmDelete = confirm('Bạn có chắc muốn xóa truyện "' + comicName + '" không?');
                    if (confirmDelete) {
                        window.deleteComicDirect(comicId);
                    }
                }
            });
        });
        document.querySelectorAll('.more-btn').forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                document.querySelectorAll('.dropdown-menu').forEach(m => m.style.display = 'none');
                const menu = this.nextElementSibling;
                const rect = this.getBoundingClientRect();
                menu.style.display = 'block';
                menu.style.top = rect.bottom + 'px';
                menu.style.left = (rect.right - menu.offsetWidth) + 'px';
            });
        });
    }
    document.addEventListener('DOMContentLoaded', function () {
        loadInitialComicsList();
        const filterSelect = document.getElementById('displayFilter');
        filterSelect.addEventListener('change', function () {
            searchProducts(1);
        });
        document.getElementById('mainSearchInput').addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                searchProducts(1);
            }
        });
        document.addEventListener('click', () => {
            document.querySelectorAll('.dropdown-menu').forEach(m => m.style.display = 'none');
        });
    });
    async function deleteComic(comicId) {
        try {
            const response = await fetch(contextPath + '/admin/products/delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({id: comicId})
            });
            const result = await response.json();
            if (result.success) {
                alert('Xóa truyện thành công!');
                searchProducts(currentPage);
            } else {
                alert('Lỗi: ' + (result.message || 'Không thể xóa truyện'));
            }
        } catch (error) {
            console.error('Error:', error);
            alert('Có lỗi xảy ra khi xóa truyện');
        }
    }

</script>
<script>
    document.querySelectorAll('.more-btn').forEach(btn => {
    });
    document.addEventListener('click', () => {
        document.querySelectorAll('.dropdown-menu').forEach(m => m.style.display = 'none');
    });
</script>
<script>
    const addBtn = document.querySelector('.add-btn');
    const modal = document.getElementById('addModal');
    const cancelBtn = document.querySelector('.cancel-btn');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            modal.style.display = 'flex';
        });
    }
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    }
</script>
<script>
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('editModal').style.display = 'flex';
        });
    });
    document.querySelectorAll('.cancel-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal-overlay');
            if (modal) {
                modal.style.display = 'none';
            }
        });
    });

</script>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        const current = window.location.pathname.split("/").pop();
        const links = document.querySelectorAll(".sidebar li a");

        links.forEach(link => {
            const linkPage = link.getAttribute("href");
            if (linkPage === current) {
                link.classList.add("active");
            }
        });
    });
</script>

<script>
    document.querySelectorAll('.view-review-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const comicId = btn.dataset.comic;
            const popup = document.getElementById(`review-${comicId}`);
            if (popup) popup.style.display = 'flex';
        });
    });
    document.querySelectorAll('.close-review-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            btn.closest('.review-popup').style.display = 'none';
        });
    });
    document.querySelectorAll('.review-popup').forEach(popup => {
        popup.addEventListener('click', (e) => {
            if (e.target === popup) popup.style.display = 'none';
        });
    });
</script>

<script>
    document.querySelectorAll(".image-upload").forEach(box => {
        const input = box.querySelector(".imgInput");
        const preview = box.querySelector(".imgPreview");
        const label = box.querySelector(".label");
        const icon = box.querySelector(".icon");
        const imgBox = box.querySelector(".img-box");
        if (imgBox && input) {
            imgBox.addEventListener("click", () => input.click());
            input.addEventListener("change", () => {
                const file = input.files[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = e => {
                    preview.src = e.target.result;
                    preview.style.display = "block";
                    if (icon) icon.style.display = "none";
                    if (label) label.style.display = "none";
                };
                reader.readAsDataURL(file);
            });
        }
    });
</script>
<script>
    document.querySelectorAll("#editModal .image-upload .img-box").forEach((box) => {
        box.addEventListener("click", () => {
            const input = box.parentElement.querySelector(".editImgInput");
            if (input) input.click();
        });
    });
    document.querySelectorAll(".editImgInput").forEach((input) => {
        input.addEventListener("change", () => {
            const file = input.files[0];
            if (file) {
                const img = input.parentElement.querySelector(".imgPreview");
                if (img) img.src = URL.createObjectURL(file);
            }
        });
    });
</script>
<script>
    const contextPath = '${pageContext.request.contextPath}';
</script>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        console.log('Loading initial comics list...');
    });
    async function loadInitialComicsList() {
        const tbody = document.getElementById('productTableBody');
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px;">' +
            '<i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>' +
            '<p style="margin-top: 10px;">Đang tải danh sách truyện...</p></td></tr>';
        try {
            const filterSelect = document.getElementById('displayFilter');
            const filterValue = filterSelect.value;
            let hiddenFilter = null;
            if (filterValue === 'visible') {
                hiddenFilter = 0;
            } else if (filterValue === 'hidden') {
                hiddenFilter = 1;
            }
            let url = contextPath + '/admin/products/list?page=1';
            if (hiddenFilter !== null) {
                url += '&hiddenFilter=' + hiddenFilter;
            }
            const response = await fetch(url);
            if (!response.ok) throw new Error('HTTP error! status: ' + response.status);

            const data = await response.json();
            if (data.success && data.comics) {
                console.log('Comics count:', data.comics.length);
                updateTable(data.comics);
                updatePagination(1, data.totalPages || 1, data.totalComics || data.comics.length);
                bindEventListeners();
            } else {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #f44336;">' +
                    '<p>Không thể tải dữ liệu</p></td></tr>';
            }
        } catch (error) {
            console.error(' Error:', error);
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #f44336;">' +
                '<i class="fas fa-exclamation-triangle" style="font-size: 32px;"></i>' +
                '<p style="margin-top: 10px;">Lỗi: ' + error.message + '</p></td></tr>';
        }
    }
</script>


</body>
</html>