<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chính Sách Vận Chuyển/Đóng Gói - ComicBook</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            padding: 0;
            background-color: #f9f9f9;
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
            text-align: center;
            color: #007bff;
            margin-bottom: 20px;
        }

        h2 {
            color: #0056b3;
            margin-top: 20px;
            margin-bottom: 10px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }

        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }

        th {
            background-color: #f0f0f0;
        }

        ul {
            list-style-type: disc;
            padding-left: 20px;
        }

        .note {
            font-style: italic;
            color: #666;
            margin-top: 10px;
        }

        .highlight {
            font-weight: bold;
            color: #dc3545;
        }

        .effective-date {
            text-align: center;
            font-size: 0.9em;
            color: #666;
            margin-top: 20px;
        }
    </style>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

</head>
<body>
<jsp:include page="/frontend/public/header.jsp" />

<div class="container">
    <h1>CHÍNH SÁCH VẬN CHUYỂN/ĐÓNG GÓI</h1>
    <p>Áp dụng cho toàn bộ đơn hàng của Quý Khách tại ComicBook</p>

    <h2>1. Chính sách vận chuyển</h2>
    <p>ComicBook cung cấp dịch vụ giao hàng toàn quốc, gửi hàng tận nơi đến địa chỉ cung cấp của Quý khách. Thời gian
        giao hàng dự kiến phụ thuộc vào kho có hàng và địa chỉ nhận hàng của Quý khách.</p>
    <ul>
        <li>Với đa phần đơn hàng, ComicBook cần vài giờ làm việc để kiểm tra thông tin và đóng gói hàng. Nếu các sản
            phẩm đều có sẵn hàng, ComicBook sẽ nhanh chóng bàn giao cho đối tác vận chuyển. Nếu đơn hàng có sản phẩm sắp
            phát hành, ComicBook sẽ ưu tiên giao những sản phẩm có hàng trước cho Quý khách hàng.
        </li>
    </ul>
    <h3>Bảng thời gian dự kiến</h3>
    <table>
        <tr>
            <th>Tuyến</th>
            <th>Khu vực</th>
            <th>Thời gian dự kiến</th>
        </tr>
        <tr>
            <td>Hồ Chí Minh – Hồ Chí Minh</td>
            <td>Nội Thành</td>
            <td>1 – 2 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Ngoại Thành</td>
            <td>1 – 2 ngày</td>
        </tr>
        <tr>
            <td>Hồ Chí Minh – Miền Nam</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>2 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>2 – 3 ngày</td>
        </tr>
        <tr>
            <td>Hồ Chí Minh – Miền Trung</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>3 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>3 – 4 ngày</td>
        </tr>
        <tr>
            <td>Hồ Chí Minh – Miền Bắc</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>4 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>4 – 5 ngày</td>
        </tr>
        <tr>
            <td>Hà Nội – Hà Nội</td>
            <td>Nội Thành</td>
            <td>1 – 2 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Ngoại Thành</td>
            <td>1 – 2 ngày</td>
        </tr>
        <tr>
            <td>Hà Nội – Miền Bắc</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>2 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>2 – 3 ngày</td>
        </tr>
        <tr>
            <td>Hà Nội – Miền Trung</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>3 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>3 – 4 ngày</td>
        </tr>
        <tr>
            <td>Hà Nội – Miền Nam</td>
            <td>Trung tâm Tỉnh, Thành phố, Thị xã</td>
            <td>4 ngày</td>
        </tr>
        <tr>
            <td></td>
            <td>Huyện, xã</td>
            <td>4 – 5 ngày</td>
        </tr>
    </table>
    <div class="note">
        <p><strong>Lưu ý:</strong></p>
        <ul>
            <li>Trong một số trường hợp, hàng nằm không có sẵn tại kho gần nhất, thời gian giao hàng có thể chậm hơn so
                với dự kiến do điều hàng. Các phí vận chuyển phát sinh, ComicBook sẽ hỗ trợ hoàn toàn.
            </li>
            <li>Ngày làm việc là từ thứ hai đến thứ sáu, không tính thứ bảy, chủ nhật và ngày nghỉ lễ, tết, nghỉ bù, và
                không bao gồm các tuyến huyện đảo xa.
            </li>
        </ul>
    </div>

    <h2>2. Bảng giá dịch vụ vận chuyển hàng hóa</h2>
    <table>
        <tr>
            <th>Khu vực giao</th>
            <th>Phí vận chuyển (đã bao gồm VAT)</th>
        </tr>
        <tr>
            <td>Nội thành Hồ Chí Minh, Hà Nội</td>
            <td>20.000 VNĐ/2kg (+2.000 VNĐ mỗi 1kg tiếp theo)</td>
        </tr>
        <tr>
            <td>Các khu vực còn lại</td>
            <td>32.000 VNĐ/2kg (+3.000 VNĐ mỗi 1kg tiếp theo)</td>
        </tr>
    </table>
    <p>Quý khách kiểm tra phí vận chuyển tại bước “Thanh toán”.</p>
    <div class="effective-date">Chính sách này có hiệu lực vào ngày 02 tháng 07 năm 2024.</div>

    <h2>3. Một số lưu ý khi nhận hàng</h2>
    <ul>
        <li>Trước khi tiến hành giao hàng cho Quý khách, bưu tá của Đối tác vận chuyển sẽ liên hệ qua số điện thoại của
            Quý khách trước khoảng 3 đến 5 phút để xác nhận giao hàng.
        </li>
        <li>Nếu Quý khách không thể có mặt trong đợt nhận hàng thứ nhất, ComicBook sẽ cố gắng liên lạc lại thêm ít nhất
            2 lần nữa (trong 02 ca giao hàng khác nhau) để sắp xếp thời gian giao hàng, Quý khách vui lòng để ý điện
            thoại để liên hệ được với bưu tá giao hàng.
        </li>
        <li>Nếu qua 3 lần liên hệ giao hàng, ComicBook vẫn không thể liên lạc được với Quý khách để giao hàng, ComicBook
            sẽ thông báo cho Quý khách về việc hủy đơn hàng. Trong trường hợp Quý khách đã thanh toán trước cho đơn
            hàng, Quý khách sẽ nhận lại tiền vào tài khoản trong vòng 5 - 7 ngày làm việc, phụ thuộc vào tiến độ xử lý
            của ngân hàng. Số tiền Quý khách nhận lại sẽ trừ lại chi phí vận chuyển phát sinh từ việc giao hàng nhưng
            Quý khách không nhận hàng.
        </li>
        <li>Trong trường hợp Quý khách không đồng ý nhận hàng với xuất phát nguyên nhân từ hàng hóa của ComicBook không
            đảm bảo, không đúng như mô tả, giao trễ so với cam kết,... Đơn hàng của Quý khách sẽ được hoàn lại cho chúng
            tôi và được hủy trên hệ thống ComicBook. Nếu Quý khách đã thanh toán trước cho đơn hàng, Quý khách sẽ nhận
            lại tiền vào tài khoản trong vòng 5 - 7 ngày làm việc, phụ thuộc vào tiến độ xử lý của ngân hàng. Số tiền
            Quý khách nhận lại sẽ là toàn bộ số tiền đã thanh toán cho đơn hàng (bao gồm phí vận chuyển).
        </li>
        <li>Trong trường hợp đơn hàng đang giao đến Quý khách có ngoại quan bên ngoài hộp hàng hóa có dấu hiệu bị rách,
            móp, ướt, thủng, mất niêm phong,…Quý khách vui lòng kiểm tra kỹ chất lượng sản phẩm bên trong trước khi nhận
            hàng. Quý khách hoàn toàn có quyền từ chối nhận hàng và báo về cho chúng tôi qua hotline 1900636467 để được
            hỗ trợ giao lại đơn hàng mới hoặc hủy đơn hàng, hoàn tiền.
        </li>
        <li>Trong trường hợp Quý khách không có nhu cầu nhận hàng, Quý khách có thể báo với bên vận chuyển và/hoặc CSKH
            (qua Hotline 1900636467) về việc này. Đơn hàng của Quý khách sẽ được hoàn lại cho chúng tôi và được hủy trên
            hệ thống. Trong trường hợp Quý khách đã thanh toán trước cho đơn hàng, Quý khách sẽ nhận lại tiền vào tài
            khoản trong vòng 5 - 7 ngày làm việc, phụ thuộc vào tiến độ xử lý của ngân hàng. Số tiền Quý khách nhận lại
            sẽ trừ lại chi phí vận chuyển phát sinh từ việc giao hàng nhưng Quý khách không nhận.
        </li>
        <li>ComicBook sẽ thông báo ngay đến Quý khách nếu có sự chậm chễ về thời gian giao hàng so với thời gian dự kiến
            ở trên. Trong phạm vi pháp luật cho phép, chúng tôi sẽ không chịu trách nhiệm cho bất cứ tổn thất nào, các
            khoản nợ, thiệt hại hoặc chi phí phát sinh từ việc giao hàng trễ. Trường hợp phát sinh chậm trễ trong việc
            giao hàng, nếu Quý khách không còn nhu cầu nhận hàng, ComicBook cam kết sẽ hỗ trợ Quý khách hủy đơn hàng,
            nếu Quý khách đã thanh toán trước cho đơn hàng, Quý khách sẽ nhận lại tiền vào tài khoản trong vòng 5 - 7
            ngày làm việc, phụ thuộc vào tiến độ xử lý của ngân hàng. Số tiền Quý khách nhận lại sẽ là toàn bộ số tiền
            đã thanh toán cho đơn hàng (bao gồm phí vận chuyển).
        </li>
        <li>Sản phẩm được đóng gói theo tiêu chuẩn đóng gói của ComicBook, nếu Quý khách có nhu cầu đóng gói đặc biệt
            khác, vui lòng báo trước cho chúng tôi khi đặt hàng hàng và cho phép chúng tôi được tính thêm phí cho nhu
            cầu đặc biệt này.
        </li>
        <li>Mọi thông tin về việc thay đổi sản phẩm hay hủy bỏ đơn hàng, đề nghị Quý khách thông báo sớm để ComicBook có
            thể điều chỉnh lại đơn hàng. Quý khách có thể liên hệ với chúng tôi qua số điện thoại hotline: 1900636467
            hoặc qua địa chỉ email cskh@ComicBook.vn.
        </li>
    </ul>

    <h2>4. Tra cứu thông tin vận chuyển đơn hàng</h2>
    <p>ComicBook sử dụng dịch vụ giao hàng của các Đối tác vận chuyển để thực hiện giao đơn hàng đến Quý khách.</p>
    <p>Quý khách hoàn toàn có thể tự tra cứu thông tin lộ trình vận chuyển Đơn hàng bằng 02 cách sau đây:</p>
    <ul>
        <li>Quý khách tự truy cập trang web tra cứu thông tin của các Đối tác vận chuyển, nhập mã vận đơn để tiến hành
            tra cứu.
        </li>
        <li>Quý khách liên hệ với bộ phận chăm sóc khách hàng của ComicBook qua hotline 1900636467 để được hỗ trợ tra
            cứu tình hình vận chuyển đơn hàng.
        </li>
    </ul>
    <p>ComicBook cung cấp địa chỉ website của các Đối tác vận chuyển để Quý khách tra cứu tình hình vận chuyển đơn
        hàng:</p>
    <ul>
        <li>Tổng công ty Bưu Điện Việt Nam – Vietnam Post</li>
        <li>Công Ty TNHH Nin Sing Logistics (NINJA VAN)</li>
        <li>Công ty TNHH Giao Hàng FLEX SPEED - LEX</li>
    </ul>
    <div class="effective-date">Chính sách sẽ được áp dụng và có hiệu lực từ ngày 17/05/2024.</div>
</div>
<jsp:include page="/frontend/public/Footer.jsp" />

</body>
</html>