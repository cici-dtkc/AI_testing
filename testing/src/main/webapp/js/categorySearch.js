
let currentCategoryPage = 1;
/**
 * Tìm kiếm thể loại
 */
function searchCategories(page = 1) {
    const keyword = document.getElementById('categorySearchInput').value.trim();
    const tbody = document.getElementById('categoryTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="4" style="text-align: center; padding: 40px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>
                <p style="margin-top: 10px;">Đang tìm kiếm...</p>
            </td>
        </tr>
    `;
    const contextPath = window.contextPath || '';

    // Kiểm tra contextPath
    if (!window.contextPath) {
        console.error(' ontextPath is not defined!');
        showError('Lỗi cấu hình: contextPath không tồn tại');
        return;
    }

    const url = `${contextPath}/admin/categories/search?keyword=${encodeURIComponent(keyword)}&page=${page}`;

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Data received:', data);
            if (data.success) {
                currentCategoryPage = data.currentPage;
                updateCategoryTable(data.categories);
                updateCategoryPagination(data.currentPage, data.totalPages, data.totalCategories);
            } else {
                showError(data.message || 'Có lỗi xảy ra');
            }
        })
        .catch(error => {
            console.error('Fetch Error:', error);
            showError('Không thể kết nối đến server: ' + error.message);
        });
}

/**
 * Cập nhật bảng thể loại
 */
function updateCategoryTable(categories) {
    const tbody = document.getElementById('categoryTableBody');
    if (!categories || categories.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" style="text-align: center; padding: 40px; color: #999;">
                    <i class="fas fa-inbox" style="font-size: 48px; display: block; margin-bottom: 10px;"></i>
                    <p style="margin: 0; font-size: 16px;">Không tìm thấy thể loại nào</p>
                </td>
            </tr>
        `;
        return;
    }
    let html = '';

    categories.forEach(category => {
        html += `
            <tr>
                <td>${category.id}</td>
                <td>${category.nameCategories || '-'}</td>
                <td class="action-cell">
                    <button class="edit-category-btn" data-id="${category.id}" title="Chỉnh sửa">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button class="delete-category-btn" data-id="${category.id}" title="Xóa">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
    bindCategoryEventListeners();
}

/**
 * Cập nhật phân trang
 */
function updateCategoryPagination(currentPage, totalPages, totalCategories) {
    const paginationContainer = document.getElementById('categoryPaginationContainer');
    if (!paginationContainer) return;
    if (totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }
    paginationContainer.style.display = 'block';
    let html = '<div class="pagination">';
    if (currentPage > 1) {
        html += `<button class="page-btn nav-btn" onclick="searchCategories(1)">⏮</button>`;
        html += `<button class="page-btn nav-btn" onclick="searchCategories(${currentPage - 1})">◀</button>`;
    } else {
        html += `<button class="page-btn nav-btn" disabled>⏮</button>`;
        html += `<button class="page-btn nav-btn" disabled>◀</button>`;
    }
    const maxVisible = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages, startPage + maxVisible - 1);
    if (endPage - startPage < maxVisible - 1) {
        startPage = Math.max(1, endPage - maxVisible + 1);
    }
    if (startPage > 1) {
        html += `<button class="page-btn" onclick="searchCategories(1)">1</button>`;
        if (startPage > 2) html += '<span>...</span>';
    }
    for (let i = startPage; i <= endPage; i++) {
        const activeClass = i === currentPage ? 'active' : '';
        html += `<button class="page-btn ${activeClass}" onclick="searchCategories(${i})">${i}</button>`;
    }
    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += '<span>...</span>';
        html += `<button class="page-btn" onclick="searchCategories(${totalPages})">${totalPages}</button>`;
    }
    if (currentPage < totalPages) {
        html += `<button class="page-btn nav-btn" onclick="searchCategories(${currentPage + 1})">▶</button>`;
        html += `<button class="page-btn nav-btn" onclick="searchCategories(${totalPages})">⏭</button>`;
    } else {
        html += `<button class="page-btn nav-btn" disabled>▶</button>`;
        html += `<button class="page-btn nav-btn" disabled>⏭</button>`;
    }
    html += '</div>';
    paginationContainer.innerHTML = html;
}

/**
 * Hiển thị lỗi
 */
function showError(message) {
    const tbody = document.getElementById('categoryTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="4" style="text-align: center; padding: 40px; color: #f44336;">
                <i class="fas fa-exclamation-triangle" style="font-size: 32px;"></i>
                <p style="margin-top: 10px;">${message}</p>
                <button onclick="searchCategories(1)" style="margin-top: 10px; padding: 8px 16px; cursor: pointer;">
                    Thử lại
                </button>
            </td>
        </tr>
    `;
}

/**
 * Bind event listeners
 */
function bindCategoryEventListeners() {
    document.querySelectorAll('.edit-category-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const categoryId = this.dataset.id;
            console.log('Edit category:', categoryId);
        });
    });
    document.querySelectorAll('.delete-category-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const categoryId = this.dataset.id;
            console.log('Delete category:', categoryId);
        });
    });
}

/**
 * Tải danh sách ban đầu
 */
function loadInitialCategories() {
    searchCategories(1);
}
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('categorySearchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                searchCategories(1);
            }
        });
        console.log('Search input listener added');
    } else {
        console.error(' Search input not found!');
    }
    const searchBtn = document.getElementById('categorySearchBtn');
    if (searchBtn) {
        searchBtn.addEventListener('click', function(e) {
            e.preventDefault();
            searchCategories(1);
        });
        console.log('Search button listener added');
    } else {
        console.error('Search button not found!');
    }
    loadInitialCategories();
});