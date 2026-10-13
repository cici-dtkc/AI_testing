importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyAv9tMau9HTCHCyGWYUGh7ZVf4_0IVX9Jc",
    authDomain: "comic-5e20d.firebaseapp.com",
    projectId: "comic-5e20d",
    messagingSenderId: "766720100662",
    appId: "1:766720100662:web:4ddbd85b0d633e70f14b97"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    const { title, body } = payload.notification;
    self.registration.showNotification(title, { body, icon: '/LTW_Nhom5/img/logo.png' });
});