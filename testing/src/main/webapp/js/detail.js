document.addEventListener('DOMContentLoaded', function () {
    const sliders = document.querySelectorAll('#slider-suggestions .product-slider');
    sliders.forEach((slider, idx) => {
    const track = slider.querySelector('.slider-track');
    const items = slider.querySelectorAll('.product-item');
    const prevBtn = slider.querySelector('.arrow.prev');
    const nextBtn = slider.querySelector('.arrow.next');
    const viewport = slider.querySelector('.slider-viewport');

    console.log(`Slider ${idx}:`, {
    track: !!track,
    items: items.length,
    prevBtn: !!prevBtn,
    nextBtn: !!nextBtn,
    viewport: !!viewport
});
    if (!track || !prevBtn || !nextBtn || items.length === 0) {
    console.error(`Slider ${idx} missing elements`);
    return;
}
    let position = 0;
    function update() {
    const item = items[0];
    const itemStyle = getComputedStyle(item);

    const itemWidth = item.offsetWidth;
    const gap = parseInt(itemStyle.marginRight) || 0;
    const moveDistance = itemWidth + gap;

    const viewportWidth = viewport.offsetWidth;
    const trackWidth = track.scrollWidth;

    const maxTranslate = Math.max(0, trackWidth - viewportWidth);

    let translateValue = position * moveDistance;
    if (translateValue > maxTranslate) {
    translateValue = maxTranslate;
    position--;
}

    track.style.transform = `translateX(-${translateValue}px)`;

    prevBtn.disabled = (translateValue <= 0);
    nextBtn.disabled = (translateValue >= maxTranslate);
}

    nextBtn.addEventListener('click', function (e) {
    e.preventDefault();
    e.stopPropagation();
    if (!nextBtn.disabled) {
    position++;
    update();
}
});

    prevBtn.addEventListener('click', function (e) {
    e.preventDefault();
    e.stopPropagation();
    if (!prevBtn.disabled) {
    position--;
    update();
}
});
    update();
});
});
