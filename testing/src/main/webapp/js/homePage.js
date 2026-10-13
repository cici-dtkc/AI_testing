
document.addEventListener("DOMContentLoaded", function () {
    const banner = document.querySelector('.banner');
    const listImage = document.querySelector('.list-images');
    const imgs = document.querySelectorAll('.list-images img');
    const dots = document.querySelectorAll('.dot');
    const btnPrev = document.querySelector('.banner .btn.prev');
    const btnNext = document.querySelector('.banner .btn.next');

    if (!banner || imgs.length === 0) return;

    let index = 0;
    let interval;

    function updateSlider() {
        const width = banner.clientWidth;
        listImage.style.transform = `translateX(${-width * index}px)`;

        dots.forEach((dot, i) => {
            dot.classList.toggle('active', i === index);
        });
    }

    function nextSlide() {
        index = (index + 1) % imgs.length;
        updateSlider();
    }

    function prevSlide() {
        index = (index - 1 + imgs.length) % imgs.length;
        updateSlider();
    }

    interval = setInterval(nextSlide, 3000);

    if (btnNext) btnNext.addEventListener('click', () => {
        nextSlide();
        clearInterval(interval);
        interval = setInterval(nextSlide, 3000);
    });

    if (btnPrev) btnPrev.addEventListener('click', () => {
        prevSlide();
        clearInterval(interval);
        interval = setInterval(nextSlide, 3000);
    });

    dots.forEach((dot, i) => {
        dot.addEventListener('click', () => {
            index = i;
            updateSlider();
            clearInterval(interval);
            interval = setInterval(nextSlide, 3000);
        });
    });

    window.addEventListener('resize', updateSlider);

    let loaded = 0;
    imgs.forEach(img => {
        if (img.complete) loaded++;
        else img.addEventListener('load', () => { loaded++; if (loaded === imgs.length) updateSlider(); });
    });

    if (loaded === imgs.length) updateSlider();
    else imgs.forEach(img => img.addEventListener('load', () => { if (++loaded === imgs.length) { updateSlider(); interval = setInterval(nextSlide, 3000); } }));
});

function initFlashSaleCountdown() {
    const countdownElement = document.getElementById('flash-sale-countdown');

    if (!countdownElement) {
        console.log('⚠️ No flash sale countdown element found');
        return;
    }

    const endTimeStr = countdownElement.getAttribute('data-end-time');
    if (!endTimeStr) {
        console.error(' No end time data attribute found');
        countdownElement.innerHTML = 'Không có thông tin thời gian';
        return;
    }
    const endTimeMillis = parseInt(endTimeStr);
    if (!endTimeMillis || isNaN(endTimeMillis)) {
        console.error('Invalid end time:', endTimeStr);
        countdownElement.innerHTML = 'Thời gian không hợp lệ';
        return;
    }
    console.log('Flash Sale Countdown initialized');
    console.log('   End time millis:', endTimeMillis);
    console.log('   End time date:', new Date(endTimeMillis));
    console.log('   Current time:', new Date());
    let countdownInterval;
    function updateCountdown() {
        const now = new Date().getTime();
        const distance = endTimeMillis - now;
        console.log('Distance:', distance, 'ms');
        if (distance < 0) {
            countdownElement.innerHTML = '<span style="color: #f44336;">⏰ Flash Sale đã kết thúc!</span>';
            if (countdownInterval) {
                clearInterval(countdownInterval);
            }
            setTimeout(() => {
                console.log('Reloading page...');
                location.reload();
            }, 3000);
            return;
        }
        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);
        let timeString = '';
        if (days > 0) {
            timeString = `${days} ngày ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        } else {
            timeString = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        }

        countdownElement.innerHTML = `⏰ Kết thúc sau: <strong style="font-size: 18px;">${timeString}</strong>`;
    }
    updateCountdown();
    countdownInterval = setInterval(updateCountdown, 1000);
}
document.addEventListener("DOMContentLoaded", initFlashSaleCountdown);
document.addEventListener("DOMContentLoaded", function () {
    const sliders = document.querySelectorAll('.product-slider');
    sliders.forEach(slider => {
        const track = slider.querySelector('.slider-track');
        const prevBtn = slider.querySelector('.arrow.prev');
        const nextBtn = slider.querySelector('.arrow.next');
        const items = slider.querySelectorAll('.product-item');

        let currentPosition = 0;

        function recalc() {
            const itemWidth = items[0].offsetWidth + 10;
            const totalItems = items.length;
            const trackWidth = totalItems * itemWidth;
            const containerWidth = slider.offsetWidth;
            const maxPosition = containerWidth - trackWidth;
            return { itemWidth, maxPosition };
        }

        function moveSlider(position) {
            track.style.transform = `translateX(${position}px)`;
        }

        prevBtn.addEventListener('click', () => {
            const { itemWidth } = recalc();
            currentPosition += itemWidth;
            if (currentPosition > 0) currentPosition = 0;
            moveSlider(currentPosition);
        });

        nextBtn.addEventListener('click', () => {
            const { itemWidth, maxPosition } = recalc();
            currentPosition -= itemWidth;
            if (currentPosition < maxPosition) currentPosition = maxPosition;
            moveSlider(currentPosition);
        });

        moveSlider(currentPosition);
    });

    const moreBtn = document.getElementById("more-btn-popup-slider");
    if (moreBtn) {
        moreBtn.addEventListener("click", function () {
            const popup = document.querySelector("#product-slider-popup");
            if (popup) {
                popup.style.display = "block";
            }
        });
    }
});
/**
 * Hiển thị chi tiết comic trong popup bên phải
 */
function showTopComicDetail(index) {
    const allPopups = document.querySelectorAll('.sach-chi-tiet');
    allPopups.forEach(popup => {
        popup.style.display = 'none';
    });
    const selectedPopup = document.querySelector(`.pop-detail-home${index}`);
    if (selectedPopup) {
        selectedPopup.style.display = 'flex';
    }
    const allItems = document.querySelectorAll('.sach-item');
    allItems.forEach((item, i) => {
        if (i === index - 1) {
            item.style.backgroundColor = '#fff3e0';
        } else {
            item.style.backgroundColor = '';
        }
    });
}
document.addEventListener('DOMContentLoaded', function() {
    showTopComicDetail(1);
});