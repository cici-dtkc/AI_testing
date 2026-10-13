let isInitialized = false;

document.addEventListener('DOMContentLoaded', function () {
    if (isInitialized) return;
    isInitialized = true;

    const addBtn = document.querySelector('.add-btn');
    const addModal = document.getElementById('addModal');
    const saveBtn = addModal.querySelector('.save-btn');
    const cancelBtns = addModal.querySelectorAll('.cancel-btn');
    const addForm = document.getElementById('addForm');

    addBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        addModal.style.display = 'flex';
        resetForm();
    });

    cancelBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            addModal.style.display = 'none';
            resetForm();
        });
    });

    addModal.addEventListener('click', (e) => {
        if (e.target === addModal) {
            addModal.style.display = 'none';
            resetForm();
        }
    });

    setupImageUpload();

    saveBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();

        console.log('💾 Save button clicked!');

        if (!validateForm()) {
            return;
        }

        const formData = new FormData(addForm);

        const volumeInput = document.querySelector('[name="volume"]');
        if (volumeInput && volumeInput.value) {
            formData.set('volume', volumeInput.value);
            console.log('📖 Volume:', volumeInput.value);
        }

        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';

        try {
            const url = contextPath + '/admin/products/add';
            console.log('Sending request to:', url);
            const response = await fetch(url, {
                method: 'POST',
                body: formData
            });
            console.log('Response status:', response.status);
            const result = await response.json();
            console.log('Result:', result);
            if (result.success) {
                showNotification('Thêm truyện thành công!', 'success');
                addModal.style.display = 'none';
                resetForm();
                setTimeout(() => {
                    refreshComicsTable();
                }, 500);
            } else {
                showNotification(result.message || 'Có lỗi xảy ra', 'error');
                if (result.errors && result.errors.length > 0) {
                    showErrors(result.errors);
                }
            }
        } catch (error) {
            console.error('Error:', error);
            showNotification('Lỗi kết nối server: ' + error.message, 'error');
        } finally {
            saveBtn.disabled = false;
            saveBtn.innerHTML = 'Lưu';
        }
    });
});
async function refreshComicsTable() {
    const tbody = document.getElementById('productTableBody');
    tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 20px;">' +
        '<i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #ff4c4c;"></i>' +
        '<p style="margin-top: 10px;">Đang tải lại...</p></td></tr>';

    try {
        const url = contextPath + '/admin/products/list?page=1';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Server error: ' + response.status);
        }
        const data = await response.json();
        if (data.success && data.comics) {
            updateTableWithNewData(data.comics);
            if (typeof updatePagination === 'function') {
                updatePagination(data.currentPage, data.totalPages, data.totalComics);
            }
            bindEventListeners();
        } else {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #f44336;">Không thể tải dữ liệu</td></tr>';
        }
    } catch (error) {
        console.error('Error refreshing table:', error);
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #f44336;">Lỗi: ' + error.message + '</td></tr>';
    }
}

function updateTableWithNewData(comics) {
    const tbody = document.getElementById('productTableBody');
    if (!comics || comics.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #999;">' +
            '<i class="fas fa-inbox" style="font-size: 48px; display: block; margin-bottom: 10px;"></i>' +
            '<p style="margin: 0; font-size: 16px;">Chưa có truyện nào</p></td></tr>';
        return;
    }

    let html = '';

    comics.forEach(function (comic) {
        html += '<tr>' +
            '<td>' + comic.id + '</td>' +
            '<td>' + (comic.nameComics || '-') + '</td>' +
            '<td>' + (comic.seriesName || '-') + '</td>' +
            '<td>' + (comic.categoryName || 'Chưa phân loại') + '</td>' +
            '<td>' + (comic.author || '-') + '</td>' +
            '<td>' + formatPrice(comic.price) + ' đ</td>' +
            '<td>' + comic.stockQuantity + ' quyển</td>' +
            '<td class="review-cell">' +
            '<button class="view-review-btn" data-comic="' + comic.id + '" title="Xem review">' +
            '<i class="fa-solid fa-eye"></i>' +
            '</button>' +
            '</td>' +
            '<td class="action-cell">' +
            '<button class="edit-btn" data-comic-id="' + comic.id + '"><i class="fa-solid fa-pen-to-square"></i></button>' +
            '<div class="menu-container">' +
            '<button class="more-btn">⋮</button>' +
            '<div class="dropdown-menu">' +
            '<label><input type="radio" name="display-' + comic.id + '" checked> Hiển thị</label>' +
            '<label><input type="radio" name="display-' + comic.id + '"> Ẩn sản phẩm</label>' +
            '</div>' +
            '</div>' +
            '</td>' +
            '</tr>';
    });

    tbody.innerHTML = html;
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN').format(price);
}

function bindEventListeners() {
    document.querySelectorAll('.view-review-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const comicId = this.dataset.comic;
            const popup = document.getElementById('review-' + comicId);
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
function validateForm() {
    const nameComics = document.querySelector('[name="nameComics"]').value.trim();
    const author = document.querySelector('[name="author"]').value.trim();
    const categoryId = document.querySelector('[name="categoryId"]').value;
    const priceInput = document.querySelector('[name="price"]').value.trim();
    const stockQuantity = document.querySelector('[name="stockQuantity"]').value;
    const volumeInput = document.querySelector('[name="volume"]').value;

    const errors = [];

    if (!nameComics) {
        errors.push('Vui lòng nhập tên truyện');
    }

    if (!author) {
        errors.push('Vui lòng nhập tên tác giả');
    }

    if (!categoryId) {
        errors.push('Vui lòng chọn thể loại');
    }

    const price = parseFloat(priceInput.replace(/,/g, ''));
    if (!priceInput || isNaN(price) || price <= 0) {
        errors.push('Giá phải lớn hơn 0');
    }

    if (!stockQuantity || parseInt(stockQuantity) < 0) {
        errors.push('Số lượng không được âm');
    }

    if (volumeInput && volumeInput.trim() !== '') {
        const volume = parseInt(volumeInput);
        if (isNaN(volume) || volume < 1) {
            errors.push('Số tập phải là số nguyên dương (từ 1 trở lên)');
        }
    }

    if (errors.length > 0) {
        showErrors(errors);
        return false;
    }

    return true;
}

function setupImageUpload() {
    const uploadBoxes = document.querySelectorAll('#addModal .image-upload');

    uploadBoxes.forEach(uploadBox => {
        const input = uploadBox.querySelector('.imgInput');
        const preview = uploadBox.querySelector('.imgPreview');
        const icon = uploadBox.querySelector('.icon');
        const label = uploadBox.querySelector('.label');
        const imgBox = uploadBox.querySelector('.img-box');

        const newImgBox = imgBox.cloneNode(true);
        imgBox.parentNode.replaceChild(newImgBox, imgBox);

        const freshImgBox = uploadBox.querySelector('.img-box');
        const freshInput = uploadBox.querySelector('.imgInput');
        const freshPreview = uploadBox.querySelector('.imgPreview');
        const freshIcon = uploadBox.querySelector('.icon');
        const freshLabel = uploadBox.querySelector('.label');

        freshImgBox.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            freshInput.click();
        });

        freshInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
            if (!validTypes.includes(file.type)) {
                showNotification('Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WEBP)', 'error');
                freshInput.value = '';
                return;
            }

            const maxSize = 10 * 1024 * 1024;
            if (file.size > maxSize) {
                showNotification('Kích thước ảnh không được vượt quá 10MB', 'error');
                freshInput.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = (e) => {
                freshPreview.src = e.target.result;
                freshPreview.style.display = 'block';
                freshIcon.style.display = 'none';
                freshLabel.style.display = 'none';
            };
            reader.readAsDataURL(file);
        });
    });
}

function resetForm() {
    const form = document.getElementById('addForm');
    form.reset();

    document.querySelectorAll('#addModal .image-upload').forEach(uploadBox => {
        const preview = uploadBox.querySelector('.imgPreview');
        const icon = uploadBox.querySelector('.icon');
        const label = uploadBox.querySelector('.label');

        preview.style.display = 'none';
        preview.src = '';
        icon.style.display = 'block';
        label.style.display = 'block';
    });

    const errorMsg = document.querySelector('#addModal .error-message');
    if (errorMsg) errorMsg.remove();
}

function showErrors(errors) {
    const errorHtml = errors.map(err => `<li>${err}</li>`).join('');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.innerHTML = `
        <div style="background: #fee; border: 1px solid #fcc; padding: 15px; border-radius: 5px; margin: 10px 0;">
            <strong style="color: #c33;">Lỗi:</strong>
            <ul style="margin: 5px 0 0 20px; color: #c33;">
                ${errorHtml}
            </ul>
        </div>
    `;

    const modal = document.getElementById('addModal');
    const existingError = modal.querySelector('.error-message');
    if (existingError) {
        existingError.remove();
    }

    const form = document.getElementById('addForm');
    form.parentElement.insertBefore(errorDiv, form);

    setTimeout(() => errorDiv.remove(), 5000);
}

function showNotification(message, type = 'info') {
    const notif = document.createElement('div');
    notif.className = `notification notification-${type}`;
    notif.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        background: ${type === 'success' ? '#4CAF50' : '#f44336'};
        color: white;
        border-radius: 5px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        z-index: 10000;
        animation: slideIn 0.3s ease-out;
        max-width: 400px;
    `;
    notif.textContent = message;

    document.body.appendChild(notif);

    setTimeout(() => {
        notif.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => notif.remove(), 300);
    }, 3000);
}

const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);