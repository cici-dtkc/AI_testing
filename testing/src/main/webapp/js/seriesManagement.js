const contextPath = document.querySelector('meta[name="context-path"]')?.content || '';


let deleteSeriesId = null;


function openDeleteConfirm(seriesId, seriesName) {
    deleteSeriesId = seriesId;
    document.getElementById("deleteSeriesName").textContent = seriesName;
    document.getElementById("deleteConfirmModal").style.display = "flex";
}


function confirmDeleteSeries() {
    if (!deleteSeriesId) {
        alert('Không tìm thấy series cần xóa');
        return;
    }

    const btn = document.getElementById('confirmDeleteBtn');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xóa...';

    // Gửi request xóa
    fetch(`${contextPath}/DeleteSeriesServlet?id=${deleteSeriesId}`)
        .then(response => {
            if (response.redirected) {
                window.location.href = response.url;
            } else {
                return response.json();
            }
        })
        .then(data => {
            if (data && data.success) {
                alert('Xóa series thành công!');
                window.location.reload();
            } else if (data) {
                alert('Lỗi: ' + (data.message || 'Không thể xóa series'));
                btn.disabled = false;
                btn.textContent = 'Xóa';
            }
        })
        .catch(error => {
            console.error('Delete error:', error);
            alert('Có lỗi xảy ra khi xóa series');
            btn.disabled = false;
            btn.textContent = 'Xóa';
        });
}

function cancelDelete() {
    document.getElementById("deleteConfirmModal").style.display = "none";
    deleteSeriesId = null;
}

// Bind sự kiện khi trang load
function bindDeleteButtons() {
    document.querySelectorAll('.delete-series-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const id = this.dataset.id;
            const name = this.dataset.name;
            openDeleteConfirm(id, name);
        });
    });
}


function searchSeries() {
    const searchInput = document.querySelector('.search-input');
    const keyword = searchInput.value.trim();

    if (!keyword) {
        // Nếu không có từ khóa, load lại trang
        window.location.href = `${contextPath}/SeriesManagement`;
        return;
    }

    const tbody = document.getElementById('seriesTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="5" style="text-align: center; padding: 40px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; color: #ff4c4c;"></i>
                <p style="margin-top: 10px;">Đang tìm kiếm...</p>
            </td>
        </tr>
    `;
    fetch(`${contextPath}/SeriesManagement?action=search&keyword=${encodeURIComponent(keyword)}`)
        .then(response => {
            if (!response.ok) throw new Error('Lỗi tìm kiếm');
            return response.json();
        })
        .then(data => {
            if (data.success) {
                renderSeriesTable(data.seriesList);
                if (data.seriesList.length === 0) {
                    tbody.innerHTML = `
                        <tr>
                            <td colspan="5" style="text-align: center; padding: 40px;">
                                <i class="fas fa-inbox" style="font-size: 48px; color: #999;"></i>
                                <p style="margin-top: 10px; color: #999;">Không tìm thấy series nào</p>
                            </td>
                        </tr>
                    `;
                }
            } else {
                showError('Có lỗi xảy ra khi tìm kiếm');
            }
        })
        .catch(error => {
            console.error('Search error:', error);
            showError('Không thể kết nối đến máy chủ');
        });
}

function renderSeriesTable(seriesList) {
    const tbody = document.getElementById('seriesTableBody');

    if (!seriesList || seriesList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 40px;">
                    <i class="fas fa-inbox" style="font-size: 48px; color: #999;"></i>
                    <p style="margin-top: 10px; color: #999;">Không có series nào</p>
                </td>
            </tr>
        `;
        return;
    }

    let html = '';
    seriesList.forEach(s => {
        html += `
            <tr>
                <td>${s.id}</td>
                <td>${s.seriesName}</td>
                <td>${s.totalVolumes} tập</td>
                <td>${s.status}</td>
                <td class="action-cell">
                    <div class="action-wrapper">
                        <button class="edit-series-btn"
                                data-id="${s.id}"
                                data-name="${s.seriesName}"
                                data-vol="${s.totalVolumes}"
                                data-status="${s.status}"
                                data-desc="${s.description || ''}"
                                data-cover="${s.coverUrl || ''}">
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        
                        <div class="menu-container">
                            <button class="more-btn">⋮</button>
                            <div class="dropdown-menu">
                                <input type="hidden" class="series-id" value="${s.id}">
                                <label>
                                    <input type="radio" name="display_S${s.id}" value="show" ${!s.hidden ? 'checked' : ''}> Hiển thị
                                </label>
                                <label>
                                    <input type="radio" name="display_S${s.id}" value="hide" ${s.hidden ? 'checked' : ''}> Ẩn đi
                                </label>
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;

    // Bind lại sự kiện
    bindEditButtons();
    bindMoreButtons();
    bindVisibilityChange();
}

function addNewSeries() {
    const form = document.querySelector('#addSeriesModal form');
    const formData = new FormData(form);

    // Validate
    const seriesName = formData.get('seriesName');
    const seriesVolumes = formData.get('seriesVolumes');

    if (!seriesName || !seriesVolumes) {
        alert('Vui lòng nhập đầy đủ thông tin bắt buộc!');
        return;
    }

    const saveBtn = document.getElementById('saveNewSeries');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';

    fetch(`${contextPath}/AddSeriesServlet`, {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Thêm series thành công!');
                window.location.reload();
            } else {
                alert('Lỗi: ' + (data.message || 'Không thể thêm series'));
            }
        })
        .catch(error => {
            console.error('Add error:', error);
            alert('Có lỗi xảy ra khi thêm series');
        })
        .finally(() => {
            saveBtn.disabled = false;
            saveBtn.textContent = originalText;
        });
}

function updateSeries() {
    const seriesId = document.getElementById('editSeriesId').value;
    const seriesName = document.getElementById('editSeriesName').value;
    const seriesVolumes = document.getElementById('editSeriesVolumes').value;
    const seriesStatus = document.getElementById('editSeriesStatus').value;
    const seriesDescription = document.getElementById('editSeriesDescription').value;

    if (!seriesName || !seriesVolumes) {
        alert('Vui lòng nhập đầy đủ thông tin!');
        return;
    }

    const formData = new FormData();
    formData.append('id', seriesId);
    formData.append('seriesName', seriesName);
    formData.append('totalVolumes', seriesVolumes);
    formData.append('status', seriesStatus);
    formData.append('description', seriesDescription);

    const coverFile = document.getElementById('editSeriesCoverFile');
    if (coverFile && coverFile.files.length > 0) {
        formData.append('seriesCover', coverFile.files[0]);
    }

    const saveBtn = document.getElementById('saveEditSeries');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang cập nhật...';

    fetch(`${contextPath}/UpdateSeriesServlet`, {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Cập nhật series thành công!');
                window.location.reload();
            } else {
                alert('Lỗi: ' + (data.message || 'Không thể cập nhật'));
            }
        })
        .catch(error => {
            console.error('Update error:', error);
            alert('Có lỗi xảy ra khi cập nhật');
        })
        .finally(() => {
            saveBtn.disabled = false;
            saveBtn.textContent = originalText;
        });
}

function bindEditButtons() {
    document.querySelectorAll('.edit-series-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const id = this.dataset.id;
            const name = this.dataset.name;
            const vol = this.dataset.vol;
            const status = this.dataset.status;
            const desc = this.dataset.desc || '';
            const cover = this.dataset.cover || '';

            document.getElementById('editSeriesId').value = id;
            document.getElementById('editSeriesName').value = name;
            document.getElementById('editSeriesVolumes').value = vol;
            document.getElementById('editSeriesStatus').value = status;
            document.getElementById('editSeriesDescription').value = desc;

            if (cover) {
                const preview = document.getElementById('editSeriesPreview');
                if (preview) {
                    preview.src = cover;
                    preview.style.display = 'block';
                }
            }

            document.getElementById('editSeriesModal').style.display = 'flex';
        });
    });
}

function bindMoreButtons() {
    document.querySelectorAll('.more-btn').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            const container = this.parentElement;

            document.querySelectorAll('.menu-container').forEach(m => {
                if (m !== container) m.classList.remove('active');
            });

            container.classList.toggle('active');
        });
    });
}

function bindVisibilityChange() {
    document.querySelectorAll('.dropdown-menu input[type=radio]').forEach(radio => {
        radio.addEventListener('change', function () {
            const seriesId = this.closest('.dropdown-menu').querySelector('.series-id').value;
            const action = this.value;

            fetch(`${contextPath}/SeriesManagement`, {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: new URLSearchParams({id: seriesId, action: action})
            })
                .then(res => {
                    if (res.ok) {
                        console.log(`Cập nhật series ${seriesId} → ${action}`);
                        // Đóng menu
                        this.closest('.menu-container').classList.remove('active');
                    } else {
                        alert('Có lỗi khi cập nhật trạng thái');
                    }
                })
                .catch(err => {
                    console.error('Error:', err);
                    alert('Không thể kết nối đến máy chủ');
                });
        });
    });
}

function showError(message) {
    const tbody = document.getElementById('seriesTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="5" style="text-align: center; padding: 40px; color: #f44336;">
                <i class="fas fa-exclamation-triangle" style="font-size: 32px;"></i>
                <p style="margin-top: 10px;">${message}</p>
            </td>
        </tr>
    `;
}

document.addEventListener('DOMContentLoaded', function () {
    bindDeleteButtons();
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', confirmDeleteSeries);
    }

    const cancelBtn = document.getElementById('cancelDeleteBtn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', cancelDelete);
    }
    const deleteModal = document.getElementById('deleteConfirmModal');
    if (deleteModal) {
        deleteModal.addEventListener('click', function (e) {
            if (e.target === this) {
                cancelDelete();
            }
        });
    }
    const searchInput = document.querySelector('.search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                searchSeries();
            }
        });
    }

    const searchIcon = document.querySelector('.search-box .fa-magnifying-glass');
    if (searchIcon) {
        searchIcon.addEventListener('click', searchSeries);
    }
    const addBtn = document.querySelector('.add-btn');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            document.getElementById('addSeriesModal').style.display = 'flex';
        });
    }

    const saveNewBtn = document.getElementById('saveNewSeries');
    if (saveNewBtn) {
        saveNewBtn.addEventListener('click', function (e) {
            e.preventDefault();
            addNewSeries();
        });
    }

    // Nút cập nhật
    const saveEditBtn = document.getElementById('saveEditSeries');
    if (saveEditBtn) {
        saveEditBtn.addEventListener('click', function (e) {
            e.preventDefault();
            updateSeries();
        });
    }

    // Đóng popup
    document.querySelectorAll('.close-add-series, .close-edit-series').forEach(btn => {
        btn.addEventListener('click', function () {
            this.closest('.modal-overlay').style.display = 'none';
        });
    });

    // Đóng popup khi click nền
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', function (e) {
            if (e.target === this) {
                this.style.display = 'none';
            }
        });
    });

    // Upload ảnh cho popup thêm
    const addBox = document.getElementById('newSeriesImageBox');
    const addInput = document.getElementById('newSeriesCoverFile');
    const addPreview = document.getElementById('newSeriesPreview');

    if (addBox && addInput && addPreview) {
        addBox.addEventListener('click', () => addInput.click());

        addInput.addEventListener('change', function () {
            if (this.files && this.files[0]) {
                const reader = new FileReader();
                reader.onload = e => {
                    addPreview.src = e.target.result;
                    addPreview.style.display = 'block';
                };
                reader.readAsDataURL(this.files[0]);
            }
        });
    }

    // Upload ảnh cho popup sửa
    const editBox = document.getElementById('editSeriesImageBox');
    const editInput = document.getElementById('editSeriesCoverFile');
    const editPreview = document.getElementById('editSeriesPreview');

    if (editBox && editInput) {
        editBox.addEventListener('click', () => editInput.click());

        if (editInput && editPreview) {
            editInput.addEventListener('change', function () {
                if (this.files && this.files[0]) {
                    const reader = new FileReader();
                    reader.onload = e => {
                        editPreview.src = e.target.result;
                        editPreview.style.display = 'block';
                    };
                    reader.readAsDataURL(this.files[0]);
                }
            });
        }
    }

    // Bind các sự kiện cho các phần tử có sẵn
    bindEditButtons();
    bindMoreButtons();
    bindVisibilityChange();

    // Đóng dropdown khi click ra ngoài
    document.addEventListener('click', () => {
        document.querySelectorAll('.menu-container').forEach(m => {
            m.classList.remove('active');
        });
    });
});