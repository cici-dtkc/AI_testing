<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Comic Store - Chat Người Mua - Người Bán</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/chat.css">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp" />
<div class="chat-container">
    <div class="chat-area">
        <div class="chat-header" id="chatHeader">
            <img src="https://i.pravatar.cc/150?img=1" alt="" class="user-avatar">
            <div class="header-info">
                <h3>ComicStore</h3>
                <small><i class="fas fa-circle online-dot"></i> Đang hoạt động</small>
            </div>
            <button class="header-action"><i class="fas fa-ellipsis-v"></i></button>
        </div>
        <div class="chat-messages" id="chatMessages">
            <div class="message received">
                <div class="bubble">Chào bạn! Mình có thể giúp gì ạ?</div>
                <div class="timestamp">10:32</div>
            </div>
            <div class="message sent">
                <div class="bubble">Mình cần tư vấn về sản phẩm</div>
                <div class="timestamp">10:33</div>
            </div>
        </div>
        <div class="typing-indicator" id="typingIndicator" style="display: none;">
            <span></span><span></span><span></span>
        </div>
        <div class="chat-input">
            <label class="input-action" for="imageInput" title="Gửi ảnh">
                <i class="fas fa-image"></i>
            </label>
            <input type="file" id="imageInput" accept="image/*" style="display: none;"/>
            <textarea id="messageInput" placeholder="Nhập tin nhắn..." rows="1"></textarea>
            <button class="input-action emoji-btn"><i class="fas fa-smile"></i></button>
            <button class="send-btn" onclick="sendMessage()"><i class="fas fa-paper-plane"></i></button>
        </div>
        <div id="imagePreview" class="image-preview" style="display: none;">
            <img id="previewImg" src="" alt="Preview"/>
            <button class="close-preview" onclick="closePreview()">×</button>
            <button class="send-image-btn" onclick="sendImage()">Gửi</button>
        </div>
    </div>
</div>
<jsp:include page="/frontend/public/Footer.jsp" />
<script>
    const STORAGE_KEY = 'comicstore_chat_history';
    let conversations = {};
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            conversations = JSON.parse(saved);
        }
    } catch (e) {
        console.warn("Không thể đọc lịch sử chat:", e);
    }
    const defaultConversations = {
        1: {
            user: { name: "ComicStore", avatar: "https://i.pravatar.cc/150?img=1", online: true },
            messages: [
                { text: "Chào bạn! Mình có thể giúp gì ạ?", type: "received", time: "10:32" },
                { text: "Mình cần tư vấn về sản phẩm", type: "sent", time: "10:33" }
            ]
        }
    };
    Object.keys(defaultConversations).forEach(id => {
        if (!conversations[id]) {
            conversations[id] = defaultConversations[id];
        }
    });
    const currentUserId = 1;
    function saveToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations));
        } catch (e) {
            console.warn("Không thể lưu lịch sử chat:", e);
        }
    }
    function loadConversation() {
        const conv = conversations[currentUserId];
        const header = document.getElementById('chatHeader');
        const messagesDiv = document.getElementById('chatMessages');
        header.innerHTML = `
        <img src="${conv.user.avatar}" alt="" class="user-avatar">
        <div class="header-info">
            <h3>Comic Store</h3>
            <small><i class="fas fa-circle online-dot"></i> ${conv.user.online ? 'Đang hoạt động' : 'Offline'}</small>
        </div>
        <button class="header-action"><i class="fas fa-ellipsis-v"></i></button>
`;
        messagesDiv.innerHTML = '';
        conv.messages.forEach(msg => {
            const messageEl = document.createElement('div');
            messageEl.className = `message ${msg.type}`;
            messageEl.innerHTML = `
            <div class="bubble">${msg.text}</div>
            <div class="timestamp">${msg.time}</div>
        `;
            messagesDiv.appendChild(messageEl);
        });
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
    function sendMessage() {
        const input = document.getElementById('messageInput');
        const text = input.value.trim();
        if (!text) return;

        const time = new Date().toLocaleTimeString('vi', { hour: '2-digit', minute: '2-digit' });
        const messagesDiv = document.getElementById('chatMessages');
        const msg = document.createElement('div');
        msg.className = 'message sent';
        msg.innerHTML = `<div class="bubble">${text}</div><div class="timestamp">${time}</div>`;
        messagesDiv.appendChild(msg);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
        conversations[currentUserId].messages.push({ text, type: 'sent', time });
        saveToStorage();
        input.value = '';
        input.style.height = 'auto';
        showTyping();
        setTimeout(() => {
            hideTyping();
            const reply = getAutoReply();
            const replyTime = new Date().toLocaleTimeString('vi', { hour: '2-digit', minute: '2-digit' });
            addReceivedMessage(reply, replyTime);
            conversations[currentUserId].messages.push({ text: reply, type: 'received', time: replyTime });
            saveToStorage();
        }, 1000 + Math.random() * 1000);
    }

    function showTyping() {
        const typing = document.getElementById('typingIndicator');
        typing.style.display = 'flex';
        document.getElementById('chatMessages').scrollTop = document.getElementById('chatMessages').scrollHeight;
    }
    function addReceivedMessage(text, time) {
        const messagesDiv = document.getElementById('chatMessages');
        const msg = document.createElement('div');
        msg.className = 'message received';
        msg.innerHTML = `<div class="bubble">${text}</div><div class="timestamp">${time}</div>`;
        messagesDiv.appendChild(msg);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
    function getAutoReply() {
        const replies = [
            "Cảm ơn bạn! Mình đang kiểm tra giúp bạn nhé!",
            "Sản phẩm đang có sẵn ạ, bạn cần size nào?",
            "Mình sẽ gửi hình ảnh chi tiết ngay!",
            "Bạn muốn đặt hàng luôn không ạ?",
            "Đã nhận được yêu cầu, mình xử lý ngay!"
        ];
        return replies[Math.floor(Math.random() * replies.length)];
    }
    document.getElementById('messageInput').addEventListener('keypress', e => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    const textarea = document.getElementById('messageInput');
    textarea.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    document.addEventListener('DOMContentLoaded', () => {
        loadConversation();
    });
</script>
</body>
</html>