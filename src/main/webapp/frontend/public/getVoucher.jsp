<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="vn.edu.hcmuaf.fit.ttltw_nhom6.model.Banner" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Voucher</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/getVourcher.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>

<body>
<jsp:include page="/frontend/public/header.jsp"/>

<div class="container">
    <div class="voucher-group">

        <div class="voucher-time">
            <h1>Ưu đãi khung giờ ngập tràn</h1>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 10.000đ - Toàn sàn</div>
                    <div class="voucher-desc">Đơn từ 130.000đ · Không áp dụng Manga, Ngoại văn</div>
                    <div class="voucher-code-box">FHS10KT11</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:30%"></div></div>
                        <span class="voucher-qty-text">Còn 70/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 20.000đ - Toàn sàn</div>
                    <div class="voucher-desc">Đơn từ 240.000đ · Không áp dụng Manga, Ngoại văn</div>
                    <div class="voucher-code-box">FHS20KT11</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:55%"></div></div>
                        <span class="voucher-qty-text">Còn 45/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 40.000đ - Toàn sàn</div>
                    <div class="voucher-desc">Đơn từ 490.000đ · Không áp dụng Manga, Ngoại văn</div>
                    <div class="voucher-code-box">FHS40KT11</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:20%"></div></div>
                        <span class="voucher-qty-text">Còn 80/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 80.000đ - Toàn sàn</div>
                    <div class="voucher-desc">Đơn từ 990.000đ · Không áp dụng Manga, Ngoại văn</div>
                    <div class="voucher-code-box">FHS80KT11</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:10%"></div></div>
                        <span class="voucher-qty-text">Còn 90/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="voucher-ship">
            <h1>Ưu đãi vận chuyển ngập tràn</h1>

            <div class="voucher-card">
                <div class="voucher-side-bar shipping"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 10.000đ phí ship</div>
                    <div class="voucher-desc">Đơn từ 130.000đ · Áp dụng toàn sàn</div>
                    <div class="voucher-code-box">FHS10KT11</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:40%"></div></div>
                        <span class="voucher-qty-text">Còn 60/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar shipping"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 20.000đ phí ship</div>
                    <div class="voucher-desc">Đơn từ 200.000đ · Áp dụng toàn sàn</div>
                    <div class="voucher-code-box">SHIP20K2025</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:65%"></div></div>
                        <span class="voucher-qty-text">Còn 35/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 15/12/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar shipping"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                        <span class="voucher-badge badge-single-use">1 lần/khách</span>
                    </div>
                    <div class="voucher-title">Miễn phí vận chuyển</div>
                    <div class="voucher-desc">Đơn từ 50.000đ · Không áp dụng COD</div>
                    <div class="voucher-code-box">FREESHIP50</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:47%"></div></div>
                        <span class="voucher-qty-text">Còn 53/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 31/12/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar shipping"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 5.000đ - Giao nhanh</div>
                    <div class="voucher-desc">Không yêu cầu giá trị đơn · Chỉ áp dụng giao nhanh</div>
                    <div class="voucher-code-box">SHIP5KFAST</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:80%"></div></div>
                        <span class="voucher-qty-text">Còn 20/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 01/01/2026</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar shipping"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-shipping">
                            <i class="fa-solid fa-truck" style="font-size:9px"></i> Vận chuyển
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                        <span class="voucher-badge badge-single-use">1 lần/khách</span>
                    </div>
                    <div class="voucher-title">Giảm 15.000đ - Khách mới</div>
                    <div class="voucher-desc">Chỉ áp dụng khách hàng mới · Không áp dụng combo</div>
                    <div class="voucher-code-box">SHIP15KNEW</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:25%"></div></div>
                        <span class="voucher-qty-text">Còn 75/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 10/12/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="voucher-other">
            <h1>Ưu đãi ngoại lệ ngập tràn</h1>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 10.000đ - Trinh thám</div>
                    <div class="voucher-desc">Áp dụng truyện trinh thám · Không áp dụng combo</div>
                    <div class="voucher-code-box">KID10K2025</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:50%"></div></div>
                        <span class="voucher-qty-text">Còn 50/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 30/11/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 15.000đ - Mua 3 cuốn</div>
                    <div class="voucher-desc">Mua từ 3 cuốn trở lên · Không áp dụng truyện đang giảm</div>
                    <div class="voucher-code-box">BUY3GET15K</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:35%"></div></div>
                        <span class="voucher-qty-text">Còn 65/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 15/12/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 20.000đ - Hành động</div>
                    <div class="voucher-desc">Áp dụng truyện hành động · Không áp dụng bản đặc biệt</div>
                    <div class="voucher-code-box">BL20K2025</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:60%"></div></div>
                        <span class="voucher-qty-text">Còn 40/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 31/12/2025</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>

            <!-- Card: Cuối tuần -->
            <div class="voucher-card">
                <div class="voucher-side-bar product"></div>
                <div class="voucher-card-inner">
                    <div class="voucher-badge-row">
                        <span class="voucher-badge badge-target-product">
                            <i class="fa-solid fa-tag" style="font-size:9px"></i> Sản phẩm
                        </span>
                        <span class="voucher-badge badge-status-unused">Chưa dùng</span>
                    </div>
                    <div class="voucher-title">Giảm 12.000đ - Cuối tuần</div>
                    <div class="voucher-desc">Áp dụng Thứ 7 &amp; Chủ Nhật · Không áp dụng Flash Sale</div>
                    <div class="voucher-code-box">WEEKEND12K</div>
                    <div class="voucher-qty-wrap">
                        <div class="voucher-qty-bar"><div class="voucher-qty-fill" style="width:45%"></div></div>
                        <span class="voucher-qty-text">Còn 55/100 lượt</span>
                    </div>
                    <div class="voucher-footer">
                        <span class="voucher-expiry">HSD: 01/01/2026</span>
                        <button class="voucher-save-btn" onclick="saveVoucher(this)">Lưu</button>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
<jsp:include page="/frontend/public/Footer.jsp"/>

<script>
    function saveVoucher(btn) {
        if (btn.classList.contains('saved')) return;
        btn.textContent = 'Đã lưu';
        btn.classList.add('saved');
    }
</script>
</body>
</html>