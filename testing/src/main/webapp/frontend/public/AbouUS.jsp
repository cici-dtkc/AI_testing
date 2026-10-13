<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>About Us</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/AboutUs.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>

<body>

<jsp:include page="/frontend/public/header.jsp" />


    <div class="container">
        <div>
            <section class="hero">
                <div class="hero-text">
                    <h1>About Us</h1>
                    <p>Chúng tôi mang đến trải nghiệm đọc truyện tranh hiện đại, tiện lợi và đầy cảm hứng cho độc giả
                        Việt Nam.</p>
                </div>
                <div>
                    <img id="logo" src="../../img/AboutUs.png" alt="Comic Store">
                </div>
            </section>
        </div>
        <section class="about-us" id="about">

            <h2>Về chúng tôi</h2>
            <p>
                Comic Store là nơi hội tụ của những câu chuyện kỳ ảo, hài hước, cảm động và đầy cảm hứng từ khắp nơi
                trên thế giới. Chúng tôi mang đến cho độc giả Việt Nam trải nghiệm đọc truyện tranh hiện đại, tiện lợi
                và phong phú – từ manga Nhật Bản, manhwa Hàn Quốc đến comic phương Tây và truyện tranh Việt.
            </p>
            <p>
                Mỗi đầu truyện đều được tuyển chọn kỹ lưỡng, đảm bảo chất lượng nội dung, hình ảnh và bản dịch, giúp bạn
                đắm chìm trong thế giới truyện tranh một cách trọn vẹn nhất.
            </p>
        </section>
        <section class="mission" id="mission">
            <h3>Sứ mệnh của chúng tôi</h3>
            <p>
                Chúng tôi mong muốn xây dựng một cộng đồng yêu truyện tranh sôi động, nơi mọi người có thể khám phá,
                chia sẻ và kết nối qua những câu chuyện đầy màu sắc. ComicVerse không chỉ là nơi bán truyện – mà là nơi
                khơi nguồn cảm hứng và nuôi dưỡng đam mê.
            </p>
        </section>
        <section class="features" id="features">
            <article class="feature-card">
                <h4>Kho truyện phong phú</h4>
                <p>Hơn 10.000 đầu truyện đủ thể loại: hành động, lãng mạn, hài hước, kinh dị, giả tưởng, slice of
                    life...</p>
                <p>Cập nhật liên tục các bộ truyện hot, trending và mới phát hành.</p>
            </article>
            <article class="feature-card">
                <h4>Tư vấn chọn truyện</h4>
                <p>Gợi ý truyện theo sở thích, độ tuổi và tâm trạng của bạn.</p>
                <p>Đội ngũ biên tập viên am hiểu truyện tranh luôn sẵn sàng hỗ trợ.</p>
            </article>
            <article class="feature-card">
                <h4>Trải nghiệm đọc truyện đỉnh cao</h4>
                <p>Chất lượng hình ảnh sắc nét, bản dịch chuẩn chỉnh.</p>
                <p>Dịch vụ chăm sóc khách hàng tận tâm, thân thiện.</p>
            </article>
        </section>
    </div>
</body>
<jsp:include page="/frontend/public/Footer.jsp" />


</html>