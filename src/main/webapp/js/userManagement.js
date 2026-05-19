
function renderUserPagination(paginationContainer, totalPages, currentPage, onPageClick) {
    paginationContainer.innerHTML = '';

    if (totalPages <= 1) return;

    const windowSize = 3;
    const groupIndex = Math.floor((currentPage - 1) / windowSize);
    const groupStart = groupIndex * windowSize + 1;
    const groupEnd   = Math.min(groupStart + windowSize - 1, totalPages);


    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn prev-btn';
    prevBtn.innerHTML = '&#171;';
    prevBtn.disabled = groupStart === 1;
    prevBtn.addEventListener('click', function () {
        if (groupStart > 1) onPageClick(groupStart - 1);
    });
    paginationContainer.appendChild(prevBtn);

    // Nút số trang
    for (let i = groupStart; i <= groupEnd; i++) {
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (i === currentPage ? ' active' : '');
        btn.textContent = i;
        const pageIndex = i;
        btn.addEventListener('click', function () { onPageClick(pageIndex); });
        paginationContainer.appendChild(btn);
    }

    // Nút »
    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn next-btn';
    nextBtn.innerHTML = '&#187;';
    nextBtn.disabled = groupEnd >= totalPages;
    nextBtn.addEventListener('click', function () {
        if (groupEnd < totalPages) onPageClick(groupEnd + 1);
    });
    paginationContainer.appendChild(nextBtn);
}

function initUserPagination() {
    const ROWS_PER_PAGE = 10;
    const tbody = document.getElementById('userTableBody');
    const paginationContainer = document.getElementById('tablePagination');

    if (!tbody || !paginationContainer) return;

    const rows = Array.from(tbody.querySelectorAll('tr')).filter(r =>
        !r.classList.contains('pagination-row') && r.cells.length > 1
    );

    const totalPages = Math.ceil(rows.length / ROWS_PER_PAGE);
    let currentPage = 1;

    function showPage(page) {
        const start = (page - 1) * ROWS_PER_PAGE;
        const end = start + ROWS_PER_PAGE;
        rows.forEach((r, idx) => {
            r.style.display = (idx >= start && idx < end) ? '' : 'none';
        });
    }

    function goToPage(page) {
        currentPage = page;
        showPage(page);
        renderUserPagination(paginationContainer, totalPages, currentPage, goToPage);
    }

    renderUserPagination(paginationContainer, totalPages, currentPage, goToPage);
    showPage(1);
}

document.addEventListener('DOMContentLoaded', function () {

    document.getElementById('searchInput').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); document.getElementById('searchForm').submit(); }
    });
    document.querySelector('.fa-magnifying-glass').addEventListener('click', function () {
        document.getElementById('searchForm').submit();
    });
    document.getElementById('levelFilter').addEventListener('change', function () {
        document.getElementById('searchForm').submit();
    });

    document.querySelectorAll('.kebab-btn').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            document.querySelectorAll('.menu-dropdown').forEach(menu => {
                if (menu !== this.nextElementSibling) menu.classList.remove('show');
            });
            this.nextElementSibling.classList.toggle('show');
        });
    });

    document.addEventListener('click', function (e) {
        if (!e.target.closest('.popup-overlay') && !e.target.closest('.kebab-menu')) {
            document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
        }
    });

    document.querySelectorAll('.menu-dropdown').forEach(menu => {
        menu.addEventListener('click', e => e.stopPropagation());
    });

    document.querySelectorAll('.popup-overlay').forEach(popup => {
        popup.addEventListener('click', function (e) {
            if (e.target === this) {
                this.style.display = 'none';
                document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
            }
        });
    });
    document.querySelectorAll('.popup-box').forEach(box => {
        box.addEventListener('click', e => e.stopPropagation());
    });

    document.querySelectorAll('.view-detail').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const spent = parseFloat(this.dataset.spent) || 0;
            const points = parseInt(this.dataset.points) || 0;
            let createdAt = 'Chưa có thông tin';
            if (this.dataset.createdAt && this.dataset.createdAt !== 'null') {
                try { createdAt = new Date(this.dataset.createdAt).toLocaleDateString('vi-VN'); } catch (e) {}
            }

            document.getElementById('detailName').textContent     = this.dataset.name;
            document.getElementById('detailEmail').textContent    = this.dataset.email;
            document.getElementById('detailPhone').textContent    = this.dataset.phone || 'Chưa cập nhật';
            document.getElementById('detailSpent').textContent    = spent.toLocaleString('vi-VN') + 'đ';
            document.getElementById('detailPoints').textContent   = points.toLocaleString('vi-VN') + ' xu';
            document.getElementById('detailCreatedAt').textContent = createdAt;

            const badge = document.getElementById('detailLevelBadge');
            badge.textContent = this.dataset.level;
            badge.className   = 'level-badge level-' + this.dataset.level;

            document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
            document.getElementById('detailPopup').style.display = 'flex';
        });
    });

    document.querySelectorAll('.upgrade-user').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            document.getElementById('upgradeUserId').value = this.dataset.id;
            document.getElementById('popupUserName').innerText = 'Tên: ' + this.dataset.name;

            const level = this.dataset.currentLevel;
            const normalizedLevel = level.charAt(0).toUpperCase() + level.slice(1).toLowerCase();
            document.getElementById('upgradeSelect').value = normalizedLevel;

            document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
            document.getElementById('upgradePopup').style.display = 'flex';
        });
    });

    document.getElementById('upgradeCancel').onclick = function () {
        document.getElementById('upgradePopup').style.display = 'none';
    };

    document.getElementById('upgradeConfirm').addEventListener('click', function () {
        const userId   = document.getElementById('upgradeUserId').value;
        const newLevel = document.getElementById('upgradeSelect').value;

        this.disabled     = true;
        this.textContent  = 'Đang xử lý...';

        fetch(BASE_URL + '/admin/user-management', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'action=upgrade&userId=' + userId + '&newLevel=' + newLevel
        })
            .then(r => r.json())
            .then(data => {
                if (data.status === 'success') {
                    document.getElementById('successPopupMessage').textContent = data.message;
                    document.getElementById('successPopup').style.display = 'flex';
                    setTimeout(() => {
                        document.getElementById('successPopup').style.display = 'none';
                        location.reload();
                    }, 2000);
                } else {
                    this.disabled = false;
                    this.textContent = 'Xác nhận';
                    document.getElementById('errorPopupMessage').innerHTML = data.message.replace(/\n/g, '<br>');
                    document.getElementById('errorPopup').style.display = 'flex';
                }
            })
            .catch(() => {
                this.disabled = false;
                this.textContent = 'Xác nhận';
                document.getElementById('errorPopupMessage').textContent = 'Có lỗi xảy ra khi nâng cấp';
                document.getElementById('errorPopup').style.display = 'flex';
            });

        document.getElementById('upgradePopup').style.display = 'none';
    });

    document.querySelectorAll('.permanent-lock').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const userId   = this.dataset.id;
            const userName = this.dataset.name;

            const loadingAlert = document.createElement('div');
            loadingAlert.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.3);z-index:9999;';
            loadingAlert.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang kiểm tra vi phạm...';
            document.body.appendChild(loadingAlert);

            fetch(BASE_URL + '/admin/user-management', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'action=check-violation&userId=' + userId
            })
                .then(r => r.json())
                .then(data => {
                    if (document.body.contains(loadingAlert)) document.body.removeChild(loadingAlert);

                    if (data.hasViolation) {
                        const violations = data.violations.join('\n• ');
                        document.getElementById('lockUserId').value = userId;
                        document.getElementById('lockMessage').innerHTML =
                            '<strong style="color:#dc3545;"> KHÓA VĨNH VIỄN TÀI KHOẢN</strong>' +
                            '<div style="margin:15px 0;padding:10px;background:#fff3cd;border-left:4px solid #ffc107;border-radius:4px;">' +
                            '<strong>Người dùng:</strong> ' + userName + '<br>' +
                            '<strong style="color:#dc3545;">Vi phạm phát hiện (' + data.violationCount + '):</strong><br>• ' +
                            violations.replace(/\\n/g, '<br>• ') +
                            '</div>' +
                            '<div style="background:#f8d7da;padding:10px;border-radius:4px;margin-top:10px;">' +
                            '<strong style="color:#721c24;"> CẢNH BÁO:</strong> Hành động này KHÔNG THỂ HOÀN TÁC!' +
                            '</div>';

                        document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
                        document.getElementById('lockPopup').style.display = 'flex';
                    } else {
                        document.getElementById('cannotLockMessage').innerHTML =
                            '<strong>Người dùng:</strong> ' + userName + '<br><br>' +
                            '<strong>Lý do:</strong> Chưa có vi phạm nào được ghi nhận';
                        document.getElementById('cannotLockPopup').style.display = 'flex';
                        document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
                    }
                })
                .catch(error => {
                    if (document.body.contains(loadingAlert)) document.body.removeChild(loadingAlert);
                    console.error('Error:', error);
                    alert(' Có lỗi xảy ra khi kiểm tra vi phạm.');
                });
        });
    });

    document.getElementById('confirmLock').addEventListener('click', function () {
        const userId = document.getElementById('lockUserId').value;
        this.disabled  = true;
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang khóa...';

        fetch(BASE_URL + '/admin/user-management', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'action=lock&userId=' + userId
        })
            .then(r => r.json())
            .then(data => {
                if (data.status === 'success') { alert('KHÓA THÀNH CÔNG\n\n' + data.message); location.reload(); }
                else { alert(' KHÓA THẤT BẠI\n\n' + data.message); this.disabled = false; this.textContent = 'Khóa vĩnh viễn'; }
            })
            .catch(() => { alert(' Có lỗi xảy ra.'); this.disabled = false; this.textContent = 'Khóa vĩnh viễn'; });

        document.getElementById('lockPopup').style.display = 'none';
    });

    initUserPagination();

    const current = window.location.pathname.split('/').pop();
    document.querySelectorAll('.sidebar li a').forEach(link => {
        if (link.getAttribute('href') === current) link.classList.add('active');
    });

    // Sắp xếp theo tổng chi tiêu(thấp -> cao, cao -> thấp)
    const spentSortBtn = document.getElementById('spentSortBtn');
    const spentSortDropdown = document.getElementById('spentSortDropdown');

    spentSortBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        spentSortDropdown.classList.toggle('show');
    });

    document.querySelectorAll('.sort-item').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const sortOrder = this.dataset.sort;

            document.querySelectorAll('.sort-item').forEach(i => i.classList.remove('active'));
            this.classList.add('active');

            spentSortBtn.className = sortOrder === 'asc'
                ? 'fas fa-sort-up'
                : 'fas fa-sort-down';

            const tbody = document.getElementById('userTableBody');
            const rows = Array.from(tbody.querySelectorAll('tr')).filter(r =>
                !r.classList.contains('pagination-row') && r.cells.length > 1
            );

            rows.sort((a, b) => {
                const getSpent = row => {
                    const link = row.querySelector('.view-detail');
                    return link ? parseFloat(link.dataset.spent) || 0 : 0;
                };
                return sortOrder === 'asc'
                    ? getSpent(a) - getSpent(b)
                    : getSpent(b) - getSpent(a);
            });

            const paginationRow = tbody.querySelector('.pagination-row');
            rows.forEach(r => tbody.insertBefore(r, paginationRow));

            initUserPagination();
        });
    });

    document.addEventListener('click', function (e) {
        if (!e.target.closest('#spentSortBtn') && !e.target.closest('#spentSortDropdown')) {
            spentSortDropdown.classList.remove('show');
        }
    });

    document.getElementById('statusFilter').addEventListener('change', function () {
        document.getElementById('searchForm').submit();
    });

    document.querySelectorAll('.unlock-user').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const userId = this.dataset.id;
            const userName = this.dataset.name;

            document.getElementById('unlockUserId').value = userId;
            document.getElementById('unlockMessage').innerHTML =
                'Bạn có chắc muốn mở khóa tài khoản của <strong>' + userName + '</strong>?';
            document.getElementById('unlockPopup').style.display = 'flex';
            document.querySelectorAll('.menu-dropdown').forEach(m => m.classList.remove('show'));
        });
    });

    document.getElementById('confirmUnlock').addEventListener('click', function () {
        const userId = document.getElementById('unlockUserId').value;
        this.disabled = true;
        this.textContent = 'Đang mở khóa...';

        fetch(BASE_URL + '/admin/user-management', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'action=unlock&userId=' + userId
        })
            .then(r => r.json())
            .then(data => {
                document.getElementById('unlockPopup').style.display = 'none';
                if (data.status === 'success') {
                    document.getElementById('successPopupMessage').textContent = 'Mở khóa tài khoản thành công!';
                    document.getElementById('successPopup').style.display = 'flex';
                    setTimeout(() => {
                        document.getElementById('successPopup').style.display = 'none';
                        location.reload();
                    }, 2000);
                } else {
                    document.getElementById('errorPopupMessage').textContent = data.message;
                    document.getElementById('errorPopup').style.display = 'flex';
                }
            })
            .catch(() => {
                document.getElementById('unlockPopup').style.display = 'none';
                document.getElementById('errorPopupMessage').textContent = 'Có lỗi xảy ra khi mở khóa';
                document.getElementById('errorPopup').style.display = 'flex';
                this.disabled = false;
                this.textContent = 'Mở khóa';
            });
    });
});

(function autoRefresh() {
    setInterval(function () {
        if (document.querySelector('.popup-overlay[style*="display: flex"]')) return;

        const currentSearch = document.getElementById('searchInput').value;
        const currentLevel  = document.getElementById('levelFilter').value;
        let url = BASE_URL + '/admin/user-management';
        const params = [];

        if (currentSearch) params.push('search=' + encodeURIComponent(currentSearch));
        if (currentLevel)  params.push('level='  + encodeURIComponent(currentLevel));
        if (params.length) url += '?' + params.join('&');

        window.location.href = url;
    }, 30000);
})();

document.getElementById('expenditureFilter').addEventListener('change', function () {
    document.getElementById('searchForm').submit();
});
