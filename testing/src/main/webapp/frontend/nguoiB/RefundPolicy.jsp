<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chính Sách Đổi Trả - ComicStore</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 0;
            background-color: #f4f4f4;
            color: #333;
        }
        .container {
            max-width: 800px;
            margin: 20px auto;
            padding: 20px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }
        h1 {
            color: #d32f2f;
            text-align: center;
            font-size: 28px;
        }
        h2 {
            color: #333;
            font-size: 22px;
            margin-top: 20px;
            border-bottom: 2px solid #d32f2f;
            padding-bottom: 5px;
        }
        p, ul {
            margin: 10px 0;
            font-size: 16px;
        }
        ul {
            list-style-type: disc;
            padding-left: 20px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 10px;
            text-align: left;
        }
        th {
            background-color: #d32f2f;
            color: #fff;
        }
        td {
            background-color: #fafafa;
        }
        .note {
            background-color: #fff3e0;
            padding: 10px;
            border-left: 4px solid #ff9800;
            margin: 10px 0;
        }
        .contact {
            text-align: center;
            margin-top: 20px;
            font-weight: bold;
        }
        a {
            color: #d32f2f;
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
        @media (max-width: 600px) {
            .container {
                margin: 10px;
                padding: 15px;
            }
            h1 {
                font-size: 24px;
            }
            h2 {
                font-size: 18px;
            }
            th, td {
                font-size: 14px;
                padding: 8px;
            }
        }
    </style>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>

<body>
<jsp:include page="/frontend/public/header.jsp" />
<div class="container">
    <h1>CHÍNH SÁCH ĐỔI / TRẢ / HOÀN TIỀN</h1>
    <p style="text-align: center;">Áp dụng cho toàn bộ đơn hàng tại <a href="https://www.ComicStore.com">ComicStore</a></p>

    <p>Chúng tôi luôn trân trọng sự tin tưởng và ủng hộ của quý khách khi mua sắm tại ComicStore. Để đảm bảo trải nghiệm dịch vụ thân thiện, xin quý khách kiểm tra kỹ thông tin sản phẩm, chất lượng và số lượng trước khi nhận hàng.</p>

    <h2>1. Thời gian áp dụng đổi/trả</h2>
    <table>
        <tr>
            <th>Thời gian</th>
            <th>Sản phẩm lỗi (do nhà cung cấp)</th>
            <th>Sản phẩm không lỗi</th>
            <th>Sản phẩm lỗi do người sử dụng</th>
        </tr>
        <tr>
            <td>7 ngày đầu</td>
            <td>Đổi mới/Trả không thu phí</td>
            <td>Trả không thu phí</td>
            <td>Bảo hành/sửa chữa có phí</td>
        </tr>
        <tr>
            <td>8-30 ngày</td>
            <td>Bảo hành</td>
            <td>Không hỗ trợ</td>
            <td>Bảo hành/sửa chữa có phí</td>
        </tr>
        <tr>
            <td>Sau 30 ngày</td>
            <td>Bảo hành (nếu có)</td>
            <td>Không hỗ trợ</td>
            <td>Không hỗ trợ</td>
        </tr>
    </table>
    <div class="note">
        <p><strong>Lưu ý:</strong></p>
        <ul>
            <li>Thông báo lỗi (hư hỏng, sai/thiếu hàng) trong <strong>2 ngày</strong> kể từ khi nhận hàng.</li>
            <li>Fahasa sẽ liên hệ tối đa 3 lần trong 7 ngày. Nếu không liên hệ được, yêu cầu sẽ bị từ chối.</li>
            <li>Thời gian xử lý khiếu nại: Tối đa <strong>30 ngày làm việc</strong>.</li>
        </ul>
    </div>

    <h2>2. Các trường hợp đổi/trả</h2>
    <ul>
        <li>Lỗi kỹ thuật do nhà cung cấp (sách thiếu trang, sút gáy, sản phẩm điện tử không hoạt động...).</li>
        <li>Giao nhầm/thiếu sản phẩm, phụ kiện, quà tặng.</li>
        <li>Hàng hóa kém chất lượng, hư hại do vận chuyển.</li>
        <li>Sản phẩm không đúng mô tả.</li>
        <li>Khách đặt nhầm/không còn nhu cầu (sản phẩm chưa sử dụng, còn nguyên vẹn).</li>
    </ul>

    <h2>3. Điều kiện đổi/trả</h2>
    <ul>
        <li>Sản phẩm còn nguyên bao bì, tem, mác, chưa sử dụng.</li>
        <li>Đầy đủ phụ kiện, quà tặng kèm theo (nếu có).</li>
        <li>Cung cấp hóa đơn GTGT (nếu có).</li>
        <li>Cung cấp hình ảnh/clip đối chứng theo yêu cầu.</li>
    </ul>

    <h2>4. Quy trình đổi/trả</h2>
    <p>Liên hệ qua <strong>Hotline: 1900636467</strong> hoặc email: <a href="mailto:cskh@ComicStore.vn">cskh@ComicStore.vn</a> với tiêu đề “Đổi Trả Đơn Hàng [Mã đơn hàng]”. Cung cấp:</p>
    <ul>
        <li>Video quay kiện hàng trước và trong khi mở.</li>
        <li>Hình ảnh tem kiện hàng, tình trạng sản phẩm.</li>
        <li>Video/hình chụp lỗi sản phẩm (rõ nét, đầy đủ thông tin).</li>
    </ul>

    <h2>5. Cách thức chuyển sản phẩm đổi/trả</h2>
    <p>Đóng gói sản phẩm như ban đầu (bao gồm phụ kiện, quà tặng, hóa đơn). Quay video đóng gói để làm bằng chứng. Fahasa sẽ cập nhật tiến trình qua email/điện thoại.</p>

    <h2>6. Thời gian hoàn tiền</h2>
    <table>
        <tr>
            <th>Phương thức thanh toán</th>
            <th>Thời gian hoàn tiền</th>
        </tr>
        <tr>
            <td>ATM nội địa/Zalo Pay/VNPay</td>
            <td>5-7 ngày làm việc</td>
        </tr>
        <tr>
            <td>Chuyển khoản</td>
            <td>5-7 ngày làm việc</td>
        </tr>
        <tr>
            <td>Visa/Master/JCB</td>
            <td>1-3 tuần (tùy ngân hàng)</td>
        </tr>
        <tr>
            <td>Ví Momo/Moca/ZaloPay/ShopeePay</td>
            <td>1-3 ngày làm việc</td>
        </tr>
        <tr>
            <td>Fpoint</td>
            <td>24 giờ</td>
        </tr>
    </table>

    <div class="note">
        <p><strong>Lưu ý khác:</strong></p>
        <ul>
            <li>Không áp dụng đổi/trả cho sách cũ, hàng chăm sóc cá nhân, đơn hàng bán sỉ.</li>
            <li>Hoàn tiền qua tài khoản ngân hàng cho đơn thanh toán COD.</li>
        </ul>
    </div>

    <div class="contact">
        <p>Liên hệ hỗ trợ: <strong>Hotline 1900636467</strong> | <a href="mailto:cskh@ComicStore.vn">cskh@ComicStore.vn</a></p>
        <p>Chính sách có hiệu lực từ <strong>01/08/2022</strong>.</p>
    </div>
</div>
<jsp:include page="/frontend/public/Footer.jsp" />
</body>
</html>