
console.log(' Firebase notification script loading...');

const getContextPath = () => document.body.dataset.contextPath || '';
const firebaseConfig = {
    apiKey: "AIzaSyAv9tMau9HTCHCyGWYUGh7ZVf4_0IVX9Jc",
    authDomain: "comic-5e20d.firebaseapp.com",
    projectId: "comic-5e20d",
    messagingSenderId: "766720100662",
    appId: "1:766720100662:web:4ddbd85b0d633e70f14b97"
};
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

console.log('Firebase initialized');

if ('serviceWorker' in navigator) {
    const contextPath = getContextPath();
    navigator.serviceWorker.register(
        contextPath + '/js/firebase-messaging-sw.js',
        {scope: '/'}
    )
        .then(registration => {
            console.log('=SW registered');
            messaging.swRegistration = registration;
        })
        .catch(err => {
            console.warn('SW failed (non-critical):', err.message);
        });
}

function initFirebaseMessaging() {
    console.log('📱 Initializing Firebase Messaging...');

    if (!("Notification" in window)) {
        console.log("Trình duyệt không hỗ trợ Notification API");
        return;
    }

    if (!("serviceWorker" in navigator)) {
        console.log("Trình duyệt không hỗ trợ Service Worker");
        return;
    }

    console.log(' Requesting notification permission...');

    Notification.requestPermission()
        .then(permission => {
            if (permission !== "granted") {
                console.log(" Quyền thông báo bị từ chối");
                return;
            }

            console.log("Quyền thông báo đã được cấp");

            // Lấy token với VAPID key
            return messaging.getToken({
                vapidKey: "BN76ewq9CfqebyTK_0Q2skO9cUhmyzWSIhR2L2eN6J_Vs_V7wMStLE-YVbRuaQgh_yWKrg91G5g8ejRSNU-qySM"
            });
        })
        .then(token => {
            if (token) {
                console.log(" FCM Token:", token);
                sendTokenToServer(token);
            } else {
                console.warn(" Không lấy được token");
            }
        })
        .catch(err => {
            console.error("Lỗi lấy FCM token:", err);
        });
}

// 5. Gửi token lên server
function sendTokenToServer(token) {
    const userId = document.body.dataset.userId;

    if (!userId) {
        console.log(" User chưa đăng nhập → không gửi token");
        return;
    }

    const contextPath = getContextPath();

    console.log('Sending token to server...');

    fetch(`${contextPath}/SaveFCMTokenServlet`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            token: token
        })
    })
        .then(response => {
            if (!response.ok) throw new Error("HTTP " + response.status);
            return response.json();
        })
        .then(data => {
            console.log("Token lưu thành công:", data);
        })
        .catch(err => {
            console.error(" Lỗi gửi token:", err);
        });
}
messaging.onMessage((payload) => {
    console.log("Nhận thông báo realtime:", payload);
    const notification = payload.notification || payload.data;
    if (!notification || !notification.title) {
        console.log("Payload không có title → bỏ qua");
        return;
    }
    const contextPath = getContextPath();
    const notificationOptions = {
        body: notification.body || "",
        icon: `${contextPath}/img/logo.png`,
        badge: `${contextPath}/img/logo.png`,
        tag: "comicstore-noti-" + Date.now(),
        data: notification.click_action || `${contextPath}/`
    };
    const n = new Notification(notification.title, notificationOptions);
    n.onclick = () => {
        window.focus();
        const clickAction = n.data || `${contextPath}/`;
        if (clickAction !== `${contextPath}/`) {
            window.location.href = clickAction;
        }
        n.close();
    };
    refreshNotificationBadge();
});
function refreshNotificationBadge() {
    const contextPath = getContextPath();
    fetch(`${contextPath}/NotificationServlet/count`)
        .then(r => r.json())
        .then(d => {
            const count = d.unread_count || 0;
            const badge = document.getElementById('notification-badge');

            if (badge) {
                const oldCount = parseInt(badge.textContent) || 0;
                if (count > oldCount && count > 0) {
                    badge.classList.add('badge-pulse');
                    setTimeout(() => badge.classList.remove('badge-pulse'), 1000);
                }
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';

                console.log('Badge updated:', count);
            }
        })
        .catch(err => console.error('Error refreshing badge:', err));
}

document.addEventListener("DOMContentLoaded", () => {
    const isLoggedIn = document.body.dataset.loggedIn === "true";
    const userId = document.body.dataset.userId;
    console.log('User status:', {isLoggedIn, userId});
    if (isLoggedIn && userId) {
        initFirebaseMessaging();
    } else {
    }
});

