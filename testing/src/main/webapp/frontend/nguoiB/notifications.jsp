<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông báo</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>

<jsp:include page="/frontend/public/header.jsp"/>

<main>
    <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>

    <div class="notification-container">
        <div class="tabs">
            <div class="tab active" data-type="ALL">Tất cả</div>
            <div class="tab" data-type="ORDER">Đơn Hàng</div>
            <div class="tab" data-type="EVENT">Sự kiện</div>
        </div>
        <div class="line"></div>
        <div id="notification-list-area" style="min-height:300px; max-height:480px; overflow-y:auto;">
            <div style="text-align:center;padding:60px;color:#999;">Đang tải thông báo...</div>
        </div>
        <div style="text-align:center;padding:20px;">
            <button id="mark-all-read-btn" style="padding:10px 20px;background:#007bff;color:white;border:none;border-radius:4px;cursor:pointer;display:none;">
                Đánh dấu tất cả đã đọc
            </button>
        </div>
    </div>
</main>
<jsp:include page="/frontend/public/Footer.jsp"/>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const urlParams = new URLSearchParams(window.location.search);
        const typeParam = urlParams.get('type') || 'ALL';

        document.querySelectorAll('.notification-container .tab').forEach(tab => {
            tab.classList.remove('active');
            if (tab.dataset.type === typeParam) {
                tab.classList.add('active');
            }
        });

        loadNotifications();

        document.querySelectorAll('.notification-container .tab').forEach(tab => {
            tab.addEventListener('click', function () {
                document.querySelectorAll('.notification-container .tab').forEach(t =>
                    t.classList.remove('active')
                );
                this.classList.add('active');
                loadNotifications();
            });
        });

        document.getElementById('mark-all-read-btn').addEventListener('click', markAllRead);
    });

    async function loadNotifications() {
        const container = document.querySelector('.notification-container');
        const listArea = document.getElementById('notification-list-area');
        const activeTab = container.querySelector('.tab.active');
        let type = activeTab ? (activeTab.dataset.type || 'ALL') : 'ALL';

        listArea.innerHTML = '<div style="text-align:center;padding:60px;color:#999;">Đang tải thông báo...</div>';

        try {
            const response = await fetch('${pageContext.request.contextPath}/NotificationServlet/list?type=' + type, {
                credentials: 'include'
            });

            if (!response.ok) throw new Error('HTTP ' + response.status);

            const data = await response.json();

            if (!data.notifications || data.notifications.length === 0) {
                listArea.innerHTML = '<div style="text-align:center;padding:80px;color:#999;font-size:16px;">Không có thông báo nào.</div>';
                document.getElementById('mark-all-read-btn').style.display = 'none';
                return;
            }
            document.getElementById('mark-all-read-btn').style.display = 'block';
            let html = '';
            data.notifications.forEach(function(n) {
                const unreadStyle = n.is_read ? '' : 'background:#f0f8ff;font-weight:600;border-left:4px solid #007bff;padding-left:16px;';
                const title = n.title || 'Thông báo';
                const message = (n.message || '').replace(/\n/g, '<br>');
                const formattedDate = n.formatted_date || '';
                const notiId = n.id || 0;
                let icon = '<i class="fa-solid fa-bell" style="color:#666;"></i>';
                if (n.type === 'ORDER_CONFIRMED')  icon = '<i class="fa-solid fa-circle-check" style="color:#28a745;"></i>';
                else if (n.type === 'ORDER_SHIPPED')   icon = '<i class="fa-solid fa-truck" style="color:#007bff;"></i>';
                else if (n.type === 'ORDER_CANCELLED') icon = '<i class="fa-solid fa-circle-xmark" style="color:#dc3545;"></i>';
                else if (n.type === 'ORDER_DELIVERED') icon = '<i class="fa-solid fa-box" style="color:#6f42c1;"></i>';
                else if (n.type === 'REFUND_APPROVED') icon = '<i class="fa-solid fa-money-bill-wave" style="color:#fd7e14;"></i>';
                else if (n.type === 'REFUND_REJECTED') icon = '<i class="fa-solid fa-ban" style="color:#dc3545;"></i>';
                else if (n.type === 'ORDER_UPDATE')    icon = '<i class="fa-solid fa-box-open" style="color:#17a2b8;"></i>';
                else if (n.type === 'PROMOTION')       icon = '<i class="fa-solid fa-tag" style="color:#e83e8c;"></i>';
                else if (n.type === 'FLASH_SALE_UPCOMING') icon = '<i class="fa-solid fa-fire" style="color:#ff6b35;"></i>';
                else if (n.type === 'FLASH_SALE_STARTED')  icon = '<i class="fa-solid fa-bolt" style="color:#ffc107;"></i>';
                else if (n.type === 'VOUCHER_NEW')         icon = '<i class="fa-solid fa-ticket" style="color:#20c997;"></i>';
                else if (n.type === 'VOUCHER_EXPIRING')    icon = '<i class="fa-solid fa-clock" style="color:#fd7e14;"></i>';

                html += '<div id="noti-' + notiId + '"style="display:flex;gap:12px;align-items:flex-start;padding:16px;margin-bottom:4px;border-bottom:1px solid #eee;border-radius:6px;cursor:pointer;' + unreadStyle + '" '
                    + 'onclick="markReadAndGo(' + notiId + ')">'
                    +   '<div style="font-size:22px;padding-top:2px;flex-shrink:0;">' + icon + '</div>'
                    +   '<div style="flex:1;">'
                    +     '<strong style="font-size:15px;display:block;margin-bottom:4px;' + (n.is_read ? '' : 'font-weight:700;') + '">' + title + '</strong>'
                    +     '<div style="color:#555;font-size:13px;margin-bottom:6px;line-height:1.5;">' + message + '</div>'
                    +     '<small style="color:#aaa;font-size:12px;"><i class="fa-regular fa-clock"></i> ' + formattedDate + '</small>'
                    +   '</div>'
                    + '</div>';
            });
            listArea.innerHTML = html;
            const hash = window.location.hash;
            if (hash && hash.startsWith('#noti-')) {
                const targetEl = document.querySelector(hash);
                if (targetEl) {
                    setTimeout(function() {
                        targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        targetEl.style.transition = 'background 0.5s';
                        targetEl.style.background = '#fff3cd';
                        setTimeout(function() {
                            targetEl.style.background = '';
                        }, 2000);
                    }, 300);
                }
            }

        } catch (err) {
            console.error('Lỗi load thông báo:', err);
            listArea.innerHTML = '<div style="text-align:center;padding:60px;color:red;">Lỗi tải thông báo</div>';
        }
    }

    async function markReadAndGo(id) {
        try {
            await fetch('${pageContext.request.contextPath}/NotificationServlet/mark-read?id=' + id, {
                method: 'POST',
                credentials: 'include'
            });
            loadNotifications();
        } catch (err) {
            console.error('Lỗi đánh dấu đã đọc:', err);
        }
    }

    async function markAllRead() {
        await new Promise((resolve, reject) => {
            showConfirmPopup('Đánh dấu tất cả thông báo là đã đọc?', 'Xác nhận', resolve, reject);
        });
        try {
            await fetch('${pageContext.request.contextPath}/NotificationServlet/mark-all-read', {
                method: 'POST',
                credentials: 'include'
            });
            loadNotifications();

            const badge = document.getElementById('notification-badge');
            if (badge) {
                badge.textContent = '0';
                badge.style.display = 'none';
            }
            const badgeCount = document.getElementById('header-badge-count');
            if (badgeCount) badgeCount.textContent = '(0)';
        } catch (err) {
            showToastUser('Lỗi thực hiện', 'error');
        }
    }
</script>

<script>
    function showToastUser(message, type = 'success') {
        const existing = document.getElementById('user-toast');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.id = 'user-toast';
        toast.classList.add(type);
        toast.textContent = message;
        document.body.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add('show'));

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    function showConfirmPopup(message, confirmText = 'Xác nhận', onConfirm, onCancel) {
        if (!document.getElementById('rc-popup-style')) {
            const s = document.createElement('style');
            s.id = 'rc-popup-style';
            s.textContent = `
                .rc-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.3);z-index:9999;display:flex;align-items:center;justify-content:center;opacity:0;transition:opacity .2s ease}
                .rc-overlay.show{opacity:1}
                .rc-popup{background:#fff;border-radius:12px;padding:24px 24px 18px;width:100%;max-width:320px;box-shadow:0 20px 60px rgba(0,0,0,.18);text-align:center;transform:translateY(16px) scale(.97);transition:transform .25s ease,opacity .25s ease;opacity:0}
                .rc-overlay.show .rc-popup{transform:translateY(0) scale(1);opacity:1}
                .rc-popup p{margin:0 0 20px;font-size:16px;color:#333;line-height:1.5;font-weight:600}
                .rc-popup-actions{display:flex;gap:10px}
                .rc-btn-cancel,.rc-btn-confirm{flex:1;padding:9px 0;border-radius:7px;border:none;font-size:14px;font-weight:500;cursor:pointer;transition:all .2s ease}
                .rc-btn-cancel{background:#f3f3f3;color:#555}
                .rc-btn-cancel:hover{background:#e8e8e8}
                .rc-btn-confirm{background:linear-gradient(90deg,#28a745,#218838);color:#fff;box-shadow:0 3px 10px rgba(40,167,69,.3)}
                .rc-btn-confirm:hover{background:linear-gradient(90deg,#218838,#1e7e34);transform:translateY(-1px)}
            `;
            document.head.appendChild(s);
        }

        const overlay = document.createElement('div');
        overlay.className = 'rc-overlay';
        overlay.innerHTML =
            '<div class="rc-popup">' +
            '<p>' + message + '</p>' +
            '<div class="rc-popup-actions">' +
            '<button class="rc-btn-cancel">Hủy</button>' +
            '<button class="rc-btn-confirm">' + confirmText + '</button>' +
            '</div></div>';
        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add('show'));

        function close(confirmed) {
            overlay.classList.remove('show');
            setTimeout(() => overlay.remove(), 250);
            if (confirmed && onConfirm) onConfirm();
            else if (!confirmed && onCancel) onCancel();
        }

        overlay.querySelector('.rc-btn-confirm').addEventListener('click', () => close(true));
        overlay.querySelector('.rc-btn-cancel').addEventListener('click', () => close(false));
        document.addEventListener('keydown', function esc(e) {
            if (e.key === 'Escape') {
                document.removeEventListener('keydown', esc);
                close(false);
            }
        });
    }
</script>

</body>
</html>