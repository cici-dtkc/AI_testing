<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="vi_VN"/>
<fmt:setBundle basename="java.text.DecimalFormatSymbols"/>
<!doctype html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Thanh toán</title>

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/checkout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
</head>

<body>

<jsp:include page="/frontend/public/header.jsp"/>

<div class="titleCheckout">
    <h1>THANH TOÁN</h1>
</div>

<div class="mainContainer" style="display: flex">
    <div class="container">
        <main>
            <form id="orderForm" action="${pageContext.request.contextPath}/order" method="post">
                <section class="address">
                    <c:if test="${not empty defaultAddress}">
                        <div class="default-address-notice">
                            <i class="fas fa-check-circle"></i>
                            <span>Đang sử dụng địa chỉ mặc định của bạn. Bạn có thể thay đổi nếu cần.</span>
                        </div>
                    </c:if>

                    <div class="form-group">
                        <label>Họ và tên người nhận: *</label>
                        <input type="text" name="receiverName"
                               value="${defaultRecipientName != null ? defaultRecipientName : ''}"
                               placeholder="Nhập họ tên" required>
                    </div>

                    <div class="form-group">
                        <label>Số điện thoại: *</label>
                        <input type="text" name="receiverPhone"
                               value="${defaultPhone != null ? defaultPhone : ''}"
                               placeholder="Nhập số điện thoại" required>
                    </div>

                    <div class="form-group">
                        <label>Tỉnh/Thành Phố: *</label>
                        <select name="province" id="province" required>
                            <option value="">-- Chọn Tỉnh/Thành phố --</option>
                        </select>
                        <input type="hidden" id="provinceHidden" name="provinceHidden">
                        <div id="provinceLoading" class="loading-indicator" style="display: none;">
                            <i class="fas fa-spinner fa-spin"></i> Đang tải danh sách tỉnh/thành phố...
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Quận/Huyện: *</label>
                        <select name="district" id="district" required disabled>
                            <option value="">-- Chọn Quận/Huyện --</option>
                        </select>
                        <input type="hidden" id="districtHidden" name="districtHidden">
                        <div id="districtLoading" class="loading-indicator" style="display: none;">
                            <i class="fas fa-spinner fa-spin"></i> Đang tải danh sách quận/huyện...
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Phường/Xã: *</label>
                        <select name="ward" id="ward" required disabled>
                            <option value="">-- Chọn Phường/Xã --</option>
                        </select>
                        <input type="hidden" id="wardHidden" name="wardHidden">

                        <div id="wardLoading" class="loading-indicator" style="display: none;">
                            <i class="fas fa-spinner fa-spin"></i> Đang tải danh sách phường/xã...
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Địa chỉ nhận hàng: *</label>
                        <input type="text" name="address"
                               value="${defaultStreetAddress != null ? defaultStreetAddress : ''}"
                               placeholder="Nhập địa chỉ cụ thể (số nhà, tên đường)" required>
                    </div>

                    <div class="form-group" style="padding-left: 0;">
                        <label style="display: inline-flex; align-items: center; cursor: pointer; font-weight: normal;">
                            <input type="checkbox" name="setDefaultAddress" id="setDefaultAddress" value="true"
                            ${not empty defaultAddress ? 'checked' : ''}>
                            <span class="checkbox-label-text">Đặt địa chỉ này làm địa chỉ mặc định</span>
                        </label>
                    </div>
                    <input type="hidden" id="defaultProvince" value="${defaultProvince != null ? defaultProvince : ''}">
                    <input type="hidden" id="defaultDistrict" value="${defaultDistrict != null ? defaultDistrict : ''}">
                    <input type="hidden" id="defaultWard" value="${defaultWard != null ? defaultWard : ''}">
                </section>

                <section class="shipping">
                    <h2>Phương thức Vận chuyển: *</h2>
                    <div id="shippingLoading" style="display:none;">
                        <i class="fas fa-spinner fa-spin"></i> Đang tính phí vận chuyển...
                    </div>
                    <div id="shippingError" style="display:none; color:red;"></div>

                    <label>
                        <input type="radio" name="shipping" value="standard" data-fee="25000" checked>
                        Giao hàng Tiêu Chuẩn - <span id="standardFeeDisplay">25.000 đ</span>
                        <span id="standardETA" style="color: #888; font-size: 13px"></span>
                    </label><br>
                    <label>
                        <input type="radio" name="shipping" value="express" data-fee="50000">
                        Giao hàng Hỏa Tốc - <span id="expressFeeDisplay">50.000 đ</span>
                        <span id="expressETA" style="color:#888; font-size:13px;"></span>
                    </label>

                    <input type="hidden" name="shippingFee" id="hiddenShippingFee" value="25000">
                </section>

                <section class="payment">
                    <h2>Phương thức Thanh toán: *</h2>
                    <label class="payment-option">
                        <input type="radio" name="payment" value="COD" checked>
                        <img src="https://cdn-icons-png.flaticon.com/512/2489/2489756.png"
                             alt="COD" style="width:28px;vertical-align:middle;margin:0 8px 0 4px;">
                        Thanh toán khi nhận hàng (COD)
                    </label><br>

                    <label class="payment-option">
                        <input type="radio" name="payment" value="momo">
                        <img src="${pageContext.request.contextPath}/img/momo.png"
                             alt="MoMo" style="width:28px;vertical-align:middle;margin:0 8px 0 4px;">
                        Ví Momo
                    </label><br>

                    <label class="payment-option">
                        <input type="radio" name="payment" value="vnpay">
                        <img src="https://cdn.haitrieu.com/wp-content/uploads/2022/10/Icon-VNPAY-QR.png"
                             alt="VNPay" style="width:28px;vertical-align:middle;margin:0 8px 0 4px;">
                        VNPay
                    </label><br>
                </section>

                <section class="promotion">
                    <h2>Điểm thưởng:</h2>
                    <div class="usePoint">
                        <p>Sử dụng <strong>${user.points != null ? user.points : 0}</strong> xu (1 xu = 1đ)</p>
                        <input type="checkbox" name="usePoints" id="usePoints"
                        ${(user.points == null || user.points <= 0) ? 'disabled' : ''}>
                    </div>
                </section>

                <section class="promotion">
                    <h2>Chọn voucher:</h2>
                    <div class="voucher-action-row">
                        <div id="selectedVoucherDisplay" style="display:none;" class="selected-voucher-info">
                            <i class="fas fa-tag"></i>
                            <span id="selectedVoucherText"></span>
                            <button type="button" onclick="removeVoucher()" class="remove-voucher-btn">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>

                        <button type="button" id="openVoucherBtn" onclick="openVoucherPopup()">
                            <i class="fas fa-ticket-alt"></i> Chọn voucher
                        </button>
                        <input type="hidden" name="voucherCode" id="voucherCode">
                    </div>
                </section>
            </form>
        </main>
    </div>

    <div class="totalCost">
        <section class="order-summary">
            <h2>Tổng đơn đặt hàng</h2>
            <div class="items">
                <c:forEach var="item" items="${selectedItems}">
                    <div class="itemSummary">
                        <div class="item">
                            <img src="${item.comic.thumbnailUrl}" alt="${item.comic.nameComics}">
                            <div class="item-details">
                                <p>${item.comic.nameComics}</p>
                                <span class="item-quantity">x${item.quantity}</span>
                            </div>
                        </div>
                        <span class="item-price">
                            <fmt:formatNumber value="${item.finalPrice * item.quantity}"
                                              type="number" groupingUsed="true"/> đ
                        </span>
                    </div>
                </c:forEach>
            </div>

            <div class="summary-row">
                <p>Tạm tính:</p>
                <span id="subtotal">
                    <fmt:formatNumber value="${checkoutSubtotal}" type="number" groupingUsed="true"/> đ
                </span>
            </div>
            <div class="summary-row">
                <p>Phí vận chuyển:</p>
                <span id="shippingFee">
                    <fmt:formatNumber value="${shippingFee}" type="number" groupingUsed="true"/> đ
                </span>
            </div>
            <div class="summary-row" id="pointsDiscountRow" style="display: none;">
                <p>Giảm giá từ xu:</p>
                <span id="pointsDiscount">0đ</span>
            </div>
            <div class="summary-row" id="voucherDiscountRow" style="display: none;">
                <p>Giảm giá từ voucher:</p>
                <span id="voucherDiscountSpan">0đ</span>
            </div>
            <div class="summary-row total-row">
                <p><strong>TỔNG THANH TOÁN:</strong></p>
                <span id="totalAmount">
                    <fmt:formatNumber value="${checkoutTotal}" type="number" groupingUsed="true"/> đ
                </span>
            </div>
            <button type="button" id="checkout-qr">ĐẶT HÀNG</button>
        </section>
    </div>
</div>

<div id="voucherOverlay" onclick="closeVoucherPopup(event)" style="display:none;">
    <div id="voucherPopup" onclick="event.stopPropagation()">
        <div class="vpopup-header">
            <h3>Chọn voucher</h3>
            <button onclick="closeVoucherPopup(null)" aria-label="Đóng"><i class="fas fa-times"></i></button>
        </div>
        <div id="voucherList"></div>
        <div class="vpopup-footer">
            <button type="button" onclick="confirmVoucherSelection()">Áp dụng</button>
        </div>
    </div>
</div>

<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    const API_BASE = "https://provinces.open-api.vn/api";
    const provinceSelect = document.getElementById("province");
    const districtSelect = document.getElementById("district");
    const wardSelect = document.getElementById("ward");
    const provinceLoading = document.getElementById("provinceLoading");
    const districtLoading = document.getElementById("districtLoading");
    const wardLoading = document.getElementById("wardLoading");
    const defaultProvince = document.getElementById('defaultProvince')?.value || '';
    const defaultDistrict = document.getElementById('defaultDistrict')?.value || '';
    const defaultWard = document.getElementById('defaultWard')?.value || '';

    const GHN_TOKEN = "bb67bb35-1796-11f1-b2c0-1ab1fd37f3b7";
    const GHN_SHOP_ID = 6302109;
    const FROM_DISTRICT = 3695;

    let ghnDistrictId = null;
    let ghnWardCode = null;

    function normalize(name) {
        if (!name) return '';
        return name.toLowerCase()
            .replace(/^(phường|ph\.|xã|thị trấn|tt\.|quận|q\.|huyện|h\.|tỉnh|thành phố|tp\.|city|province)\s*/i, '')
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/\s+/g, ' ')
            .trim();
    }

    function fuzzyMatch(a, b) {
        if (!a || !b) return false;
        const na = normalize(a), nb = normalize(b);
        return na === nb || na.includes(nb) || nb.includes(na);
    }

    async function getGHNProvinces() {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/master-data/province", {
            headers: {"Token": GHN_TOKEN}
        });
        return (await res.json()).data || [];
    }

    async function getGHNDistricts(provinceId) {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/master-data/district", {
            method: "POST",
            headers: {"Content-Type": "application/json", "Token": GHN_TOKEN},
            body: JSON.stringify({province_id: provinceId})
        });
        return (await res.json()).data || [];
    }

    async function getGHNWards(districtId) {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/master-data/ward", {
            method: "POST",
            headers: {"Content-Type": "application/json", "Token": GHN_TOKEN},
            body: JSON.stringify({district_id: districtId})
        });
        return (await res.json()).data || [];
    }

    async function getAvailableServices(fromDistrict, toDistrict) {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services", {
            method: "POST",
            headers: {"Content-Type": "application/json", "Token": GHN_TOKEN},
            body: JSON.stringify({shop_id: GHN_SHOP_ID, from_district: fromDistrict, to_district: toDistrict})
        });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message);
        return json.data || [];
    }

    async function calcFee(serviceId, toDistrict, toWard) {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Token": GHN_TOKEN,
                "ShopId": String(GHN_SHOP_ID)
            },
            body: JSON.stringify({
                service_id: serviceId,
                from_district_id: FROM_DISTRICT,
                to_district_id: toDistrict,
                to_ward_code: toWard,
                weight: 300,
                <%--insurance_value: ${checkoutSubtotal}--%>
                insurance_value: 0
            })
        });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message);
        return json.data.total;
    }

    async function updateShippingFee() {
        const provinceName = provinceSelect.options[provinceSelect.selectedIndex]?.text || '';
        const districtName = districtSelect.options[districtSelect.selectedIndex]?.text || '';
        const wardName = wardSelect.options[wardSelect.selectedIndex]?.text || '';

        if (!provinceName || provinceName.startsWith('--') ||
            !districtName || districtName.startsWith('--') ||
            !wardName || wardName.startsWith('--')) return;

        const loadingEl = document.getElementById('shippingLoading');
        const errorEl = document.getElementById('shippingError');
        const stdDisplay = document.getElementById('standardFeeDisplay');
        const expDisplay = document.getElementById('expressFeeDisplay');
        const stdRadio = document.querySelector('input[name="shipping"][value="standard"]');
        const expRadio = document.querySelector('input[name="shipping"][value="express"]');

        try {
            if (loadingEl) loadingEl.style.display = 'block';
            if (errorEl) errorEl.style.display = 'none';

            const ghnProvinces = await getGHNProvinces();
            const province = ghnProvinces.find(p => fuzzyMatch(p.ProvinceName, provinceName));
            if (!province) throw new Error("Không tìm thấy tỉnh: " + provinceName);

            const ghnDistricts = await getGHNDistricts(province.ProvinceID);
            const district = ghnDistricts.find(d => fuzzyMatch(d.DistrictName, districtName));
            if (!district) throw new Error("Không tìm thấy quận/huyện: " + districtName);
            ghnDistrictId = district.DistrictID;

            const ghnWards = await getGHNWards(ghnDistrictId);
            const ward = ghnWards.find(w => fuzzyMatch(w.WardName, wardName));
            if (!ward) throw new Error("Không tìm thấy phường/xã: " + wardName);
            ghnWardCode = ward.WardCode;

            const availableServices = await getAvailableServices(FROM_DISTRICT, ghnDistrictId);

            const stdService = availableServices.find(s => s.service_type_id === 2 || s.service_type_id === 5);
            const expService = availableServices.find(s => s.service_type_id === 1);

            let stdFee = null, expFee = null;

            if (stdService) {
                try {
                    stdFee = await calcFee(stdService.service_id, ghnDistrictId, ghnWardCode);
                } catch (e) {
                    console.warn("Lỗi tính phí tiêu chuẩn:", e.message);
                }
            }

            if (expService) {
                try {
                    expFee = await calcFee(expService.service_id, ghnDistrictId, ghnWardCode);
                } catch (e) {
                    console.warn("Lỗi tính phí hỏa tốc:", e.message);
                }
            }

            // if (stdFee !== null) {
            //     stdRadio.dataset.fee = stdFee;
            //     stdRadio.disabled = false;
            //     if (stdDisplay) stdDisplay.textContent = formatNumber(stdFee) + ' đ';
            // } else {
            //     stdRadio.disabled = true;
            //     if (stdDisplay) stdDisplay.textContent = 'Không khả dụng';
            //     if (stdRadio.checked && expFee !== null) expRadio.checked = true;
            // }
            //
            // if (expFee !== null) {
            //     expRadio.dataset.fee = expFee;
            //     expRadio.disabled = false;
            //     if (expDisplay) expDisplay.textContent = formatNumber(expFee) + ' đ';
            // } else {
            //     expRadio.disabled = true;
            //     if (expDisplay) expDisplay.textContent = 'Không khả dụng';
            // }
            const stdETA = document.getElementById('standardETA');
            const expETA = document.getElementById('expressETA');
            if (stdFee != null) {
                stdRadio.dataset.fee = stdFee;
                stdRadio.disabled = false;
                if (stdDisplay) stdDisplay.textContent = formatNumber(stdFee) + ' đ';
                try {
                    const eta = await getExpectedDelivery(stdService.service_id, ghnDistrictId, ghnWardCode);
                    if (stdETA) stdETA.textContent = '— Dự kiến: ' + eta;
                } catch (e) {
                    if (stdETA) stdETA.textContent = '';
                }
            } else {
                stdRadio.disabled = true;
                if (stdDisplay) stdDisplay.textContent = 'Không khả dụng';
                if (stdETA) stdETA.textContent = '';
                if (stdRadio.checked && expFee !== null) expRadio.checked = true;
            }

            if (expFee !== null) {
                expRadio.dataset.fee = expFee;
                expRadio.disabled = false;
                if (expDisplay) expDisplay.textContent = formatNumber(expFee) + ' đ';
                try {
                    const eta = await getExpectedDelivery(expService.service_id, ghnDistrictId, ghnWardCode);
                    if (expETA) expETA.textContent = '— Dự kiến: ' + eta;
                } catch (e) {
                    if (expETA) expETA.textContent = '';
                }
            } else {
                expRadio.disabled = true;
                if (expDisplay) expDisplay.textContent = 'Không khả dụng';
                if (expETA) expETA.textContent = '';
            }

            updateTotal();

            const checked = document.querySelector('input[name="shipping"]:checked');
            if (checked) {
                document.getElementById('hiddenShippingFee').value = checked.dataset.fee;
            }
        } catch (err) {
            console.error("GHN Error:", err);
            if (errorEl) {
                errorEl.textContent = "Không tính được phí ship: " + err.message;
                errorEl.style.display = 'block';
            }
            if (stdDisplay) stdDisplay.textContent = '25.000đ';
            if (expDisplay) expDisplay.textContent = '50.000đ';
            stdRadio.disabled = false;
            expRadio.disabled = false;
        } finally {
            if (loadingEl) loadingEl.style.display = 'none';
        }
    }

    async function loadProvinces() {
        if (provinceLoading) provinceLoading.style.display = "block";
        try {
            const res = await fetch(API_BASE + "/p/");
            if (!res.ok) throw new Error("HTTP " + res.status);
            const provinces = await res.json();

            provinceSelect.innerHTML = '<option value="">-- Chọn Tỉnh/Thành phố --</option>';
            let autoSelectCode = null;

            provinces.forEach(p => {
                const opt = document.createElement("option");
                opt.value = p.name;
                opt.dataset.code = p.code;
                opt.textContent = p.name;
                if (defaultProvince && fuzzyMatch(p.name, defaultProvince)) {
                    opt.selected = true;
                    autoSelectCode = p.code;
                }
                provinceSelect.appendChild(opt);
            });

            provinceSelect.disabled = false;
            console.log("Province auto-select code:", autoSelectCode, "| defaultProvince:", defaultProvince);
            if (autoSelectCode) loadDistricts(autoSelectCode, defaultDistrict);

        } catch (err) {
            console.error("Error loading provinces:", err);
            alert("Không thể tải danh sách tỉnh/thành phố: " + err.message);
        } finally {
            if (provinceLoading) provinceLoading.style.display = "none";
        }
    }

    async function loadDistricts(provinceCode, autoSelectDistrict = '') {
        districtSelect.innerHTML = '<option value="">-- Đang tải... --</option>';
        districtSelect.disabled = true;
        wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
        wardSelect.disabled = true;
        if (districtLoading) districtLoading.style.display = "block";

        try {
            const res = await fetch(API_BASE + "/p/" + provinceCode + "?depth=2");
            if (!res.ok) throw new Error("HTTP " + res.status);
            const data = await res.json();
            const districts = data.districts || [];

            districtSelect.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
            let autoSelectCode = null;

            districts.forEach(d => {
                const opt = document.createElement("option");
                opt.value = d.name;
                opt.dataset.code = d.code;
                opt.textContent = d.name;
                if (autoSelectDistrict && fuzzyMatch(d.name, autoSelectDistrict)) {
                    opt.selected = true;
                    autoSelectCode = d.code;
                    opt.value = d.name;
                }
                districtSelect.appendChild(opt);
            });

            districtSelect.disabled = false;
            console.log("Auto select district code:", autoSelectCode);
            if (autoSelectCode) loadWards(autoSelectCode, defaultWard);

        } catch (err) {
            console.error("Error loading districts:", err);
            alert("Không thể tải danh sách quận/huyện: " + err.message);
            districtSelect.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
        } finally {
            if (districtLoading) districtLoading.style.display = "none";
        }
    }

    async function loadWards(districtCode, autoSelectWard = '') {
        wardSelect.innerHTML = '<option value="">-- Đang tải... --</option>';
        wardSelect.disabled = true;
        if (wardLoading) wardLoading.style.display = "block";

        try {
            const res = await fetch(API_BASE + "/d/" + districtCode + "?depth=2");
            if (!res.ok) throw new Error("HTTP " + res.status);
            const data = await res.json();
            const wards = data.wards || [];

            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';

            // Debug
            console.log("defaultWard cần match:", autoSelectWard);
            console.log("Danh sách xã từ API:", wards.map(w => w.name));

            wards.forEach(w => {
                const opt = document.createElement("option");
                opt.value = w.name;
                opt.dataset.code = w.code;
                opt.textContent = w.name;

                if (autoSelectWard && fuzzyMatch(w.name, autoSelectWard)) {
                    opt.selected = true;
                }
                wardSelect.appendChild(opt);
            });

            wardSelect.disabled = false;

            if (autoSelectWard && wardSelect.value) {
                console.log("Ward auto-selected:", wardSelect.value);
                updateShippingFee();
            } else {
                console.warn("Không tìm thấy ward match với:", autoSelectWard);
            }

        } catch (err) {
            console.error("Error loading wards:", err);
            alert("Không thể tải danh sách phường/xã: " + err.message);
            wardSelect.innerHTML = '<option value="">-- Lỗi tải dữ liệu --</option>';
        } finally {
            if (wardLoading) wardLoading.style.display = "none";
        }
    }

    provinceSelect.addEventListener("change", function () {
        document.getElementById('provinceHidden').value = this.value;
        const code = this.options[this.selectedIndex]?.dataset.code;
        if (code) {
            loadDistricts(code);
        } else {
            districtSelect.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
            districtSelect.disabled = true;
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            wardSelect.disabled = true;
        }
    });

    districtSelect.addEventListener("change", function () {
        document.getElementById('districtHidden').value = this.value;
        const code = this.options[this.selectedIndex]?.dataset.code;
        if (code) {
            loadWards(code);
        } else {
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            wardSelect.disabled = true;
        }
    });

    wardSelect.addEventListener("change", function () {
        document.getElementById('wardHidden').value = this.value;
        if (this.value) updateShippingFee();
    });

    const shippingRadios = document.querySelectorAll('input[name="shipping"]');
    const usePointsCheckbox = document.getElementById('usePoints');
    const subtotal = ${checkoutSubtotal};
    const userPoints = ${user.points != null ? user.points : 0};
    const shippingFeeSpan = document.getElementById('shippingFee');
    const pointsDiscountRow = document.getElementById('pointsDiscountRow');
    const pointsDiscountSpan = document.getElementById('pointsDiscount');
    const totalAmountSpan = document.getElementById('totalAmount');

    function formatNumber(num) {
        return Number(num || 0).toLocaleString('vi-VN');
    }

    shippingRadios.forEach(r => r.addEventListener('change', updateTotal));
    if (usePointsCheckbox) {
        usePointsCheckbox.addEventListener('change', updateTotal);
        }

    const checkoutBtn = document.getElementById('checkout-qr');
    const orderForm = document.getElementById('orderForm');

    function validateCheckoutForm() {
        const recipientName = document.querySelector('input[name="receiverName"]');
        const receiverPhone = document.querySelector('input[name="receiverPhone"]');
        const address = document.querySelector('input[name="address"]');

        if (!recipientName?.value.trim()) {
            alert('Vui lòng nhập họ tên người nhận');
            recipientName.focus();
            return false;
        }
        if (!receiverPhone?.value.trim()) {
            alert('Vui lòng nhập số điện thoại');
            receiverPhone.focus();
            return false;
        }
        if (!/^[0-9]{10,11}$/.test(receiverPhone.value.trim())) {
            alert('Số điện thoại không hợp lệ (10-11 chữ số)');
            receiverPhone.focus();
            return false;
        }
        if (!provinceSelect.value) {
            alert('Vui lòng chọn Tỉnh/Thành phố');
            provinceSelect.focus();
            return false;
        }
        if (!districtSelect.value) {
            alert('Vui lòng chọn Quận/Huyện');
            districtSelect.focus();
            return false;
        }
        if (!wardSelect.value) {
            alert('Vui lòng chọn Phường/Xã');
            wardSelect.focus();
            return false;
        }
        if (!address?.value.trim()) {
            alert('Vui lòng nhập địa chỉ cụ thể');
            address.focus();
            return false;
        }
        return true;
    }

    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', function () {
            if (!validateCheckoutForm()) return;

            const checked = document.querySelector('input[name="shipping"]:checked');
            if (checked && checked.dataset.fee) {
                document.getElementById('hiddenShippingFee').value = checked.dataset.fee;
            }

            checkoutBtn.disabled = true;
            checkoutBtn.textContent = 'Đang xử lý...';
            districtSelect.disabled = false;
            wardSelect.disabled = false;
            orderForm.submit();
        });
    }

    window.addEventListener('DOMContentLoaded', function () {
        <c:if test="${not empty orderError}">
        showErrorPopup(`<c:out value="${orderError}" escapeXml="false"/>`);
        </c:if>

        <c:if test="${not empty orderSuccess}">
        showSuccessPopup(`<c:out value="${orderSuccess}" escapeXml="false"/>`);
        </c:if>
    });

    function showErrorPopup(message) {
        const backdrop = document.createElement('div');
        backdrop.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0,0,0,0.6); z-index: 10000;
        display: flex; align-items: center; justify-content: center;
    `;

        backdrop.innerHTML = `
        <div style="
            background: white;
            border-radius: 12px;
            padding: 30px;
            max-width: 400px;
            width: 90%;
            text-align: center;
        ">
            <h2 style="color: #dc3545; font-size: 22px; margin-bottom: 10px;">
                Sản phẩm hiện không đủ số lượng theo đơn của bạn hoặc sản phẩm đã hư hỏng
            </h2>
            <p style="color: #666; font-size: 15px; margin-bottom: 20px;">
                    ${message}
            </p>
            <button onclick="this.closest('div').parentElement.remove()"
                style="
                    background: #dc3545;
                    color: white;
                    border: none;
                    padding: 12px 30px;
                    border-radius: 6px;
                    font-size: 16px;
                    cursor: pointer;
                    font-weight: 600;">
                OK
            </button>
        </div>
    `;

        document.body.appendChild(backdrop);
        setTimeout(() => backdrop.remove(), 5000);
    }

    function showSuccessPopup(message) {
        const backdrop = document.createElement('div');
        backdrop.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0,0,0,0.6); z-index: 10000;
        display: flex; align-items: center; justify-content: center;
    `;
        backdrop.innerHTML = `
        <div style="background: white; border-radius: 12px; padding: 30px; max-width: 400px; width: 90%; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,0.3);">
            <h2 style="color: #333; font-size: 22px; margin-bottom: 10px;">Dat hang thanh cong!</h2>
            <p style="color: #666; font-size: 15px; margin-bottom: 20px;">${message}</p>
            <button onclick="this.closest('div').parentElement.remove()"
                style="background: #28a745; color: white; border: none; padding: 12px 30px; border-radius: 6px; font-size: 16px; cursor: pointer; font-weight: 600;">
                OK
            </button>
        </div>
    `;
        document.body.appendChild(backdrop);
        setTimeout(() => backdrop.remove(), 5000);
    }
    function updateTotal() {
        const selectedShipping = document.querySelector('input[name="shipping"]:checked');
        const shippingFee = selectedShipping ? parseInt(selectedShipping.dataset.fee) || 0 : 0;
        const hiddenFee = document.getElementById('hiddenShippingFee');
        if (hiddenFee) hiddenFee.value = shippingFee;

        let pointsDiscount = 0;

        if (usePointsCheckbox && usePointsCheckbox.checked) {
            pointsDiscount = Number(userPoints) || 0;
        }

        let voucherDiscount = 0;
        const voucherDiscountRow = document.getElementById('voucherDiscountRow');
        const voucherDiscountSpan = document.getElementById('voucherDiscountSpan');
        if (appliedVoucher) {
            const target = (appliedVoucher.discount_target || '').toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

            const isShipping = target.includes('van chuyen') || target === 'shipping';
            const isProduct  = target.includes('giam gia')   || target === 'product';

            let rawDiscount = 0;
            if (appliedVoucher.discount_type === 'percent') {
                const base = isShipping ? shippingFee : subtotal;
                rawDiscount = Math.floor(base * appliedVoucher.discount_value / 100);
            } else {
                rawDiscount = appliedVoucher.discount_value;
            }

            if (isProduct) {
                voucherDiscount = rawDiscount;
            } else if (isShipping) {
                voucherDiscount = Math.min(rawDiscount, shippingFee);
            }

            if (voucherDiscountRow) {
                voucherDiscountRow.style.display = 'flex';
                if (voucherDiscountSpan) voucherDiscountSpan.textContent = '-' + formatNumber(voucherDiscount) + ' đ';
            }
        } else {
            if (voucherDiscountRow) voucherDiscountRow.style.display = 'none';
        }

        const total = Math.max(0, subtotal + shippingFee - pointsDiscount - voucherDiscount);

        if (shippingFeeSpan) shippingFeeSpan.textContent = formatNumber(shippingFee) + ' đ';

        if (pointsDiscount > 0) {
            if (pointsDiscountRow) pointsDiscountRow.style.display = 'flex';
            if (pointsDiscountSpan) pointsDiscountSpan.textContent =
                '-' + formatNumber(pointsDiscount) + ' đ';
        } else {
            if (pointsDiscountRow) pointsDiscountRow.style.display = 'none';
        }

        if (totalAmountSpan) totalAmountSpan.textContent = formatNumber(total) + ' đ';
    }

    loadProvinces();
    updateTotal();
</script>
<script>
    async function getExpectedDelivery(serviceId, toDistrict, toWard) {
        const res = await fetch("https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/leadtime", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Token": GHN_TOKEN,
                "ShopId": String(GHN_SHOP_ID)
            },
            body: JSON.stringify({
                from_district_id: FROM_DISTRICT,
                to_district_id: toDistrict,
                to_ward_code: toWard,
                service_id: serviceId
            })
        });
        const json = await res.json();
        if (json.code !== 200) throw new Error(json.message);
        const ts = json.data.leadtime;
        const date = new Date(ts * 1000);
        return date.toLocaleDateString('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric'});
    }
</script>

<script>
    const userVouchers = ${vouchersJson != null ? vouchersJson : '[]'};
    let tempSelectedVoucher = null;
    let appliedVoucher = null;

    function openVoucherPopup() {
        const overlay = document.getElementById('voucherOverlay');
        overlay.style.display = 'flex';
        renderVoucherList();
    }

    function closeVoucherPopup(e) {
        if (e && e.target !== document.getElementById('voucherOverlay')) return;
        document.getElementById('voucherOverlay').style.display = 'none';
        tempSelectedVoucher = null;
    }

    function renderVoucherList() {
        const list = document.getElementById('voucherList');
        if (!userVouchers || userVouchers.length === 0) {
            list.innerHTML = '<div class="no-voucher-msg"><i class="fas fa-ticket-alt" style="font-size:32px;display:block;margin-bottom:10px;opacity:0.3"></i>Bạn chưa có voucher nào</div>';
            return;
        }

        list.innerHTML = userVouchers.map(function (v) {
            const isSelected = appliedVoucher && appliedVoucher.code === v.code;
            const isExpired = v.expired;
            const isShip = v.discount_target === 'Vận chuyển';

            const selectedClass = isSelected ? ' selected' : '';
            const expiredClass = isExpired ? ' expired' : '';
            const onclickAttr = isExpired ? '' : 'onclick="selectVoucher(\'' + v.code + '\')"';
            const checkedAttr = isSelected ? 'checked' : '';
            const disabledAttr = isExpired ? 'disabled' : '';

            const leftClass = isShip ? 'type-ship' : 'type-product';
            const leftIcon = isShip ? 'fa-truck' : 'fa-tag';
            const leftLabel = isShip ? 'VẬN CHUYỂN' : 'SẢN PHẨM';

            let discountText = '';
            if (v.discount_type === 'percent') {
                discountText = 'Giảm ' + v.discount_value + '%';
            } else {
                discountText = 'Giảm ' + formatNumber(v.discount_value) + 'đ';
            }

            const minOrderHtml = v.min_order_amount
                ? '<span><i class="fas fa-shopping-cart"></i> Đơn tối thiểu ' + formatNumber(v.min_order_amount) + 'đ</span>'
                : '';
            const expiryHtml = v.end_date
                ? '<span><i class="fas fa-clock"></i> HSD: ' + v.end_date + '</span>'
                : '';

            const checkboxOrBadge = isExpired
                ? '<span class="uv-badge-expired">Hết hạn</span>'
                : '<input type="checkbox" ' + checkedAttr + ' ' + disabledAttr
                + ' onchange="selectVoucher(\'' + v.code + '\')"'
                + ' onclick="event.stopPropagation()" id="vc_' + v.code + '">';

            return '<div class="voucher-item' + selectedClass + expiredClass + '" ' + onclickAttr + '>'
                + '<div class="voucher-item-left ' + leftClass + '">'
                + '<i class="fas ' + leftIcon + '"></i>'
                + '<span>' + leftLabel + '</span>'
                + '</div>'
                + '<div class="voucher-item-sep"></div>'
                + '<div class="voucher-item-body">'
                + '<div class="vdiscount">' + discountText + '</div>'
                + '<div class="vcode">Mã: <span>' + v.code + '</span></div>'
                + '<div class="vmeta">' + minOrderHtml + expiryHtml + '</div>'
                + '</div>'
                + '<div class="voucher-item-check">' + checkboxOrBadge + '</div>'
                + '</div>';
        }).join('');
    }

    function selectVoucher(code) {
        const v = userVouchers.find(x => x.code === code);
        if (!v || v.expired) return;
        if (tempSelectedVoucher?.code === code || appliedVoucher?.code === code) {
            tempSelectedVoucher = null;
        } else {
            tempSelectedVoucher = v;
        }
        userVouchers.forEach(x => {
            const cb = document.getElementById('vc_' + x.code);
            if (cb) cb.checked = (tempSelectedVoucher?.code === x.code);
            const item = cb?.closest('.voucher-item');
            if (item) item.classList.toggle('selected', tempSelectedVoucher?.code === x.code);
        });
    }

    function applyManualVoucher() {
        const code = document.getElementById('voucherInputManual').value.trim().toUpperCase();
        if (!code) {
            alert('Vui lòng nhập mã voucher');
            return;
        }
        const v = userVouchers.find(x => x.code === code);
        if (!v) {
            alert('Mã voucher không hợp lệ hoặc không thuộc về tài khoản của bạn');
            return;
        }
        if (v.expired) {
            alert('Voucher này đã hết hạn');
            return;
        }
        tempSelectedVoucher = v;
        renderVoucherList();
    }

    function confirmVoucherSelection() {
        appliedVoucher = tempSelectedVoucher || null;
        console.log('appliedVoucher:', JSON.stringify(appliedVoucher));
        document.getElementById('voucherCode').value = appliedVoucher?.code || '';
        const display = document.getElementById('selectedVoucherDisplay');
        const text = document.getElementById('selectedVoucherText');
        const btn = document.getElementById('openVoucherBtn');
        if (appliedVoucher) {
            let discountText = appliedVoucher.discount_type === 'percent'
                ? 'Giảm ' + appliedVoucher.discount_value + '%'
                : 'Giảm ' + formatNumber(appliedVoucher.discount_value) + 'đ';
            text.textContent = appliedVoucher.code + ' — ' + discountText;
            display.style.display = 'flex';
            btn.innerHTML = '<i class="fas fa-ticket-alt"></i> Thay đổi voucher';
        } else {
            display.style.display = 'none';
            btn.innerHTML = '<i class="fas fa-ticket-alt"></i> Chọn voucher';
        }
        document.getElementById('voucherOverlay').style.display = 'none';
        updateTotal();
    }

    function removeVoucher() {
        appliedVoucher = null;
        tempSelectedVoucher = null;
        document.getElementById('voucherCode').value = '';
        document.getElementById('selectedVoucherDisplay').style.display = 'none';
        document.getElementById('openVoucherBtn').innerHTML = '<i class="fas fa-ticket-alt"></i> Chọn voucher';
        updateTotal();
    }
</script>

</body>
</html>