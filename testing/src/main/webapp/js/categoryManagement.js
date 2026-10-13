// ==================== GLOBAL VARIABLES ====================
const ROWS_PER_PAGE = 10;
let currentPage = 1;
let totalPages = 1;
let currentKeyword = '';
let currentCategoryPage = 1;
let deletingCategoryId = null;
let deletingCategoryName = '';
let editingCategoryId = null;


function getContextPath() {
    return window.contextPath || '';
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

function setDefaultDate() {
    const today = new Date().toISOString().split('T')[0];
    const newDateInput = document.getElementById('newCategoryCreatedDate');
    if (newDateInput) {
        newDateInput.value = today;
        newDateInput.readOnly = true;
    }

    const editDateInput = document.getElementById('editCategoryDate');
    if (editDateInput) {
        editDateInput.value = today;
        editDateInput.readOnly = true;
    }
}

function showMessage(message, type = 'info') {
    const oldMsg = document.querySelector('.toast-message');
    if (oldMsg) {
        oldMsg.remove();
    }

    const toast = document.createElement('div');
    toast.className = `toast-message toast-${type}`;

    let icon = '';
    switch(type) {
        case 'success':
            icon = 'fa-check-circle';
            break;
        case 'error':
            icon = 'fa-exclamation-circle';
            break;
        case 'info':
            icon = 'fa-info-circle';
            break;
        default:
            icon = 'fa-info-circle';
    }

    toast.innerHTML = `
        <i class="fas ${icon}"></i>
        <span>${message}</span>
    `;

    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function showToast(message, type = 'success') {
    showMessage(message, type);
}
function searchCategories(page = 1) {
    const keyword = document.getElementById('categorySearchInput').value.trim();
    const tbody = document.getElementById('categoryTableBody');

    tbody.innerHTML = `
        <tr>
            <td colspan="3" style="text-align: center; padding: 40px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>
                <p style="margin-top: 10px;">Đang tìm kiếm...</p>
            </td>
        </tr>
    `;

    const contextPath = getContextPath();

    if (!contextPath && contextPath !== '') {
        showError('Lỗi cấu hình: contextPath không tồn tại');
        return;
    }

    const url = `${contextPath}/admin/categories/search?keyword=${encodeURIComponent(keyword)}&page=${page}`;

    fetch(url)
        .then(response => {
            console.log('📡 Response status:', response.status);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                currentCategoryPage = data.currentPage;
                currentPage = data.currentPage;
                updateCategoryTable(data.categories);
                updateCategoryPagination(data.currentPage, data.totalPages, data.totalCategories);
                if (keyword) {
                    showToast(data.message || `Tìm thấy ${data.totalCategories} kết quả`, 'info');
                }
            } else {
                showError(data.message || 'Có lỗi xảy ra');
            }
        })
        .catch(error => {
            console.error('Search error:', error);
            showError('Không thể kết nối đến server: ' + error.message);
        });
}

function handleSearch() {
    const input = document.getElementById('categorySearchInput');
    currentKeyword = input.value.trim();
    console.log('Searching with keyword:', currentKeyword);
    searchCategories(1);
}
function loadCategories(page = 1) {
    const contextPath = getContextPath();
    const tbody = document.getElementById('categoryTableBody');

    tbody.innerHTML = `
        <tr>
            <td colspan="3" style="text-align: center; padding: 40px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>
                <p style="margin-top: 10px;">Đang tải...</p>
            </td>
        </tr>
    `;

    fetch(`${contextPath}/admin/listCategories?page=${page}&pageSize=${ROWS_PER_PAGE}`)
        .then(res => res.json())
        .then(data => {
            console.log("Load categories response:", data);

            if (!data.success) {
                console.error(data.message);
                showMessage(data.message || 'Không thể tải dữ liệu', 'error');
                return;
            }

            currentPage = data.currentPage;
            totalPages = data.totalPages;

            updateCategoryTable(data.categories);
            renderPagination(data.currentPage, data.totalPages);
        })
        .catch(err => {
            console.error("Fetch error:", err);
            showMessage('Không thể tải danh sách thể loại!', 'error');
            showError('Không thể tải danh sách thể loại!');
        });
}
function updateCategoryTable(categories) {
    const tbody = document.getElementById('categoryTableBody');

    if (!categories || categories.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="3" style="text-align: center; padding: 40px; color: #999;">
                    <i class="fas fa-inbox" style="font-size: 48px; display: block; margin-bottom: 10px;"></i>
                    <p style="margin: 0; font-size: 16px;">Không tìm thấy thể loại nào</p>
                </td>
            </tr>
        `;
        return;
    }

    let html = '';
    categories.forEach(category => {
        const escapedName = escapeHtml(category.nameCategories || '-');
        const isHidden = category.is_hidden === 1 || category.is_hidden === true;
        html += `
            <tr>
                <td>${category.id}</td>
                <td>${escapedName}</td>
                <td class="action-cell">
                    <div class="action-wrapper">
                        <button class="edit-category-btn" 
                                data-id="${category.id}" 
                                onclick="openEditPopup(${category.id})"
                                title="Chỉnh sửa">
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        <button class="delete-category-btn" 
                                data-id="${category.id}" 
                                data-name="${escapedName}"
                                onclick="openDeletePopup(${category.id}, '${escapedName.replace(/'/g, "\\'")}')"
                                title="Xóa">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                        <div class="menu-container" id="menu-cat-${category.id}">
                            <button class="more-btn" onclick="toggleCategoryMenu(${category.id})">⋮</button>
                            <div class="dropdown-menu">
                                <label>
                                    <input type="radio" name="display_CAT${category.id}"
                                           value="show" ${!isHidden ? 'checked' : ''}
                                           onchange="toggleCategoryHidden(${category.id}, 'show')"> Hiển thị
                                </label>
                                <label>
                                    <input type="radio" name="display_CAT${category.id}"
                                           value="hide" ${isHidden ? 'checked' : ''}
                                           onchange="toggleCategoryHidden(${category.id}, 'hide')"> Ẩn đi
                                </label>
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
    bindCategoryEventListeners();
}
function updateCategoryPagination(currentPage, totalPages, totalCategories) {
    const container = document.getElementById('categoryPaginationContainer');

    if (!container) return;

    if (totalPages <= 1) {
        container.style.display = 'none';
        return;
    }

    container.style.display = 'block';

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
    container.innerHTML = html;
}

function renderPagination(current, total) {
    const container = document.getElementById('categoryPaginationContainer');

    if (total <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '<div class="pagination">';

    html += `<button class="page-btn" ${current === 1 ? 'disabled' : ''} 
             onclick="loadCategories(${current - 1})">
             <i class="fas fa-chevron-left"></i>
             </button>`;

    for (let i = 1; i <= total; i++) {
        if (i === 1 || i === total || (i >= current - 2 && i <= current + 2)) {
            html += `<button class="page-btn ${i === current ? 'active' : ''}" 
                     onclick="loadCategories(${i})">${i}</button>`;
        } else if (i === current - 3 || i === current + 3) {
            html += '<span class="page-dots">...</span>';
        }
    }

    html += `<button class="page-btn" ${current === total ? 'disabled' : ''} 
             onclick="loadCategories(${current + 1})">
             <i class="fas fa-chevron-right"></i>
             </button>`;

    html += '</div>';
    container.innerHTML = html;
}
function openAddPopup() {
    console.log('Opening add popup');
    document.getElementById('newCategoryName').value = '';
    document.getElementById('newCategoryDesc').value = '';
    setDefaultDate();
    document.getElementById('addPopup').style.display = 'flex';
}

function closeAddPopup() {
    document.getElementById('addPopup').style.display = 'none';
}

function handleAddCategory() {
    console.log('=== Adding Category ===');
    const name = document.getElementById('newCategoryName').value.trim();
    const description = document.getElementById('newCategoryDesc').value.trim();

    if (!name) {
        showMessage('Vui lòng nhập tên thể loại!', 'error');
        return;
    }

    if (name.length > 100) {
        showMessage('Tên thể loại không được vượt quá 100 ký tự!', 'error');
        return;
    }

    const contextPath = getContextPath();
    const url = `${contextPath}/admin/addCategory`;
    console.log('Adding to URL:', url);

    const saveBtn = document.querySelector('#addPopup .save-btn');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Đang lưu...';
    }

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=UTF-8'
        },
        body: JSON.stringify({
            name: name,
            description: description
        })
    })
        .then(response => {
            console.log('Add response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Add response data:', data);

            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Lưu';
            }

            if (data.success) {
                showMessage(data.message || 'Thêm thể loại thành công!', 'success');
                closeAddPopup();
                loadCategories(1);
            } else {
                showMessage(data.message || 'Có lỗi xảy ra!', 'error');
            }
        })
        .catch(error => {
            console.error('Error adding category:', error);

            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = 'Lưu';
            }

            showMessage('Không thể kết nối đến server! ' + error.message, 'error');
        });
}

function openEditPopup(id) {
    console.log('=== Opening Edit Popup ===');
    console.log('Category ID:', id);

    editingCategoryId = id;

    const popup = document.getElementById('editPopup');
    const nameInput = document.getElementById('editCategoryName');
    const descInput = document.getElementById('editCategoryDesc');
    const dateInput = document.getElementById('editCategoryDate');

    if (!popup || !nameInput || !descInput) {
        return;
    }

    nameInput.value = '';
    descInput.value = '';

    if (dateInput) {
        const today = new Date().toISOString().split('T')[0];
        dateInput.value = today;
    }

    popup.style.display = 'flex';

    const contextPath = getContextPath();
    const url = `${contextPath}/admin/getCategoryById?id=${id}`;
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.category) {
                const category = data.category;
                nameInput.value = category.nameCategories || '';
                descInput.value = category.description || '';
            } else {
                showMessage(data.message || 'Không tìm thấy thể loại!', 'error');
                closeEditPopup();
            }
        })
        .catch(error => {
            showMessage('Không thể tải thông tin thể loại! ' + error.message, 'error');
            closeEditPopup();
        });
}

function closeEditPopup() {
    const editPopup = document.getElementById('editPopup');
    if (editPopup) {
        editPopup.style.display = 'none';
    }
    editingCategoryId = null;
    const nameInput = document.getElementById('editCategoryName');
    const descInput = document.getElementById('editCategoryDesc');
    if (nameInput) nameInput.value = '';
    if (descInput) descInput.value = '';
}

function handleEditCategory() {
    console.log('Editing Category ID:', editingCategoryId);
    if (!editingCategoryId) {
        showMessage('Không xác định được thể loại cần sửa!', 'error');
        return;
    }
    const name = document.getElementById('editCategoryName').value.trim();
    const description = document.getElementById('editCategoryDesc').value.trim();
    if (!name) {
        showMessage('Vui lòng nhập tên thể loại!', 'error');
        return;
    }
    if (name.length > 100) {
        showMessage('Tên thể loại không được vượt quá 100 ký tự!', 'error');
        return;
    }
    const contextPath = getContextPath();
    const url = `${contextPath}/admin/updateCategory`;
    const saveBtn = document.getElementById('confirmEditBtn');
    const originalText = saveBtn ? saveBtn.textContent : 'Cập nhật';
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Đang lưu...';
    }
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=UTF-8'
        },
        body: JSON.stringify({
            id: editingCategoryId,
            name: name,
            description: description
        })
    })
        .then(response => {
            console.log('Update response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Update response data:', data);

            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = originalText;
            }

            if (data.success) {
                showMessage(data.message || 'Cập nhật thể loại thành công!', 'success');
                closeEditPopup();
                loadCategories(currentPage || 1);
            } else {
                showMessage(data.message || 'Cập nhật thất bại!', 'error');
            }
        })
        .catch(error => {
            console.error('Error updating category:', error);

            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = originalText;
            }

            showMessage('Không thể kết nối đến server! ' + error.message, 'error');
        });
}
function openDeletePopup(id, name) {
    console.log('Opening delete popup for:', id, name);
    deletingCategoryId = id;
    deletingCategoryName = name;
    const deleteNameElement = document.getElementById('deleteCategoryName');
    const deleteMessageElement = document.getElementById('deleteCategoryMessage');
    if (deleteNameElement) {
        deleteNameElement.textContent = `"${name}"`;
    }
    if (deleteMessageElement) {
        deleteMessageElement.textContent = `Bạn có chắc chắn muốn xóa thể loại `;
    }
    const deletePopup = document.getElementById('deletePopup');
    if (deletePopup) {
        deletePopup.style.display = 'flex';
    }
}
function closeDeletePopup() {
    const deletePopup = document.getElementById('deletePopup');
    if (deletePopup) {
        deletePopup.style.display = 'none';
    }
    deletingCategoryId = null;
    deletingCategoryName = '';
}
function handleDeleteCategory() {
    console.log('Category ID:', deletingCategoryId);
    if (!deletingCategoryId) {
        showMessage('Không xác định được thể loại cần xóa!', 'error');
        return;
    }
    const contextPath = getContextPath();
    const url = `${contextPath}/admin/deleteCategory`;
    console.log('Deleting at URL:', url);

    const deleteBtn = document.getElementById('confirmDeleteBtn');
    if (deleteBtn) {
        deleteBtn.disabled = true;
        deleteBtn.textContent = 'Đang xóa...';
    }
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=UTF-8'
        },
        body: JSON.stringify({
            id: deletingCategoryId
        })
    })
        .then(response => {
            console.log('Delete response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Delete response data:', data);

            if (deleteBtn) {
                deleteBtn.disabled = false;
                deleteBtn.textContent = 'Xóa';
            }

            if (data.success) {
                showMessage(data.message || 'Xóa thành công!', 'success');
                closeDeletePopup();
                loadCategories(currentPage);
            } else {
                showMessage(data.message || 'Có lỗi xảy ra!', 'error');
            }
        })
        .catch(error => {
            console.error('Error deleting category:', error);

            if (deleteBtn) {
                deleteBtn.disabled = false;
                deleteBtn.textContent = 'Xóa';
            }

            showMessage('Không thể kết nối đến server! ' + error.message, 'error');
        });
}
function showError(message) {
    const tbody = document.getElementById('categoryTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="3" style="text-align: center; padding: 40px; color: #f44336;">
                <i class="fas fa-exclamation-triangle" style="font-size: 32px;"></i>
                <p style="margin-top: 10px;">${message}</p>
                <button onclick="loadCategories(1)" 
                        style="margin-top: 10px; padding: 8px 16px; cursor: pointer; 
                               background: #ff4c4c; color: white; border: none; border-radius: 4px;">
                    Thử lại
                </button>
            </td>
        </tr>
    `;
}

function closeAllPopups() {
    document.querySelectorAll('.popup-overlay').forEach(popup => {
        popup.style.display = 'none';
    });
    deletingCategoryId = null;
    deletingCategoryName = '';
    editingCategoryId = null;
}

function bindCategoryEventListeners() {
    // Event listeners are handled by onclick in HTML
    console.log('Category event listeners bound via onclick attributes');
}

function initEventListeners() {
    const addBtn = document.querySelector('.add-category-btn');
    if (addBtn) {
        addBtn.addEventListener('click', openAddPopup);
    }

    const saveBtn = document.querySelector('#addPopup .save-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', handleAddCategory);
    }

    const searchBtn = document.getElementById('categorySearchBtn');
    if (searchBtn) {
        searchBtn.addEventListener('click', handleSearch);
    }

    const searchInput = document.getElementById('categorySearchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSearch();
            }
        });
    }

    const deleteConfirmBtn = document.getElementById('confirmDeleteBtn');
    if (deleteConfirmBtn) {
        deleteConfirmBtn.addEventListener('click', function(e) {
            e.preventDefault();
            handleDeleteCategory();
        });
    }

    const deleteCancelBtn = document.querySelector('#deletePopup .cancel-btn');
    if (deleteCancelBtn) {
        deleteCancelBtn.addEventListener('click', function(e) {
            e.preventDefault();
            closeDeletePopup();
        });
    }

    document.querySelectorAll('.popup-overlay').forEach(overlay => {
        overlay.addEventListener('click', function (e) {
            if (e.target === this) {
                closeAllPopups();
            }
        });
    });
}

function initEditEventListeners() {
    const editSaveBtn = document.getElementById('confirmEditBtn');
    if (editSaveBtn) {
        editSaveBtn.addEventListener('click', function(e) {
            e.preventDefault();
            handleEditCategory();
        });
    }

    const editCancelBtn = document.querySelector('#editPopup .cancel-btn');
    if (editCancelBtn) {
        editCancelBtn.addEventListener('click', function(e) {
            e.preventDefault();
            closeEditPopup();
        });
    }
}
function toggleCategoryMenu(id) {
    const container = document.getElementById(`menu-cat-${id}`);
    document.querySelectorAll('.menu-container').forEach(m => {
        if (m !== container) m.classList.remove('active');
    });
    container.classList.toggle('active');
}

function toggleCategoryHidden(id, action) {
    const contextPath = getContextPath();
    fetch(`${contextPath}/admin/CategoryManagement`, {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({id: id, action: action})
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showMessage(action === 'hide' ? 'Đã ẩn thể loại!' : 'Đã hiển thị thể loại!', 'success');
                const container = document.getElementById(`menu-cat-${id}`);
                if (container) container.classList.remove('active');
            } else {
                showMessage('Có lỗi khi cập nhật!', 'error');
            }
        })
        .catch(err => {
            console.error('Toggle hidden error:', err);
            showMessage('Không thể kết nối đến server!', 'error');
        });
}

document.addEventListener('click', function(e) {
    if (!e.target.closest('.menu-container')) {
        document.querySelectorAll('.menu-container').forEach(m => m.classList.remove('active'));
    }
});

document.addEventListener('DOMContentLoaded', function() {
    initEventListeners();
    initEditEventListeners();
    setDefaultDate();
    loadCategories(1);
});