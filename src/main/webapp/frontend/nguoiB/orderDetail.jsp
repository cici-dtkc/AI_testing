<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chi tiết đơn hàng #${order.id}</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/FooterStyle.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/publicCss/nav.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/profile.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/css/UserBCss/orderDetail.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<jsp:include page="/frontend/public/header.jsp"/>

<main>
  <jsp:include page="/frontend/nguoiB/ASideUser.jsp"/>

  <div class="order-history-container">

    <div class="detail-back-row">
      <a href="${pageContext.request.contextPath}/order-history" class="btn-back">
        <i class="fa-solid fa-arrow-left"></i> Trở lại
      </a>
      <h2 class="detail-title">Chi tiết đơn hàng <span>#${order.id}</span></h2>
    </div>

    <div class="detail-card">
      <div class="stepper-wrap">
        <c:set var="s" value="${order.status}"/>

        <div class="step done">
          <div class="step-icon"><i class="fa-solid fa-file-lines"></i></div>
          <div class="step-label">Đơn Hàng Đã Đặt</div>
          <div class="step-time">${order.orderDateFormatted}</div>
        </div>
        <div class="step-line ${s == 'Pending' ? '' : 'done'}"></div>

        <div class="step ${s == 'Pending' ? 'pending-step' : 'done'}">
          <div class="step-icon"><i class="fa-solid fa-circle-dollar-to-slot"></i></div>
          <div class="step-label">Đã Xác Nhận</div>
          <div class="step-time">${order.confirmedAtFormatted}</div>
        </div>
        <div class="step-line ${s == 'Pending' || s == 'AwaitingPickup' ? '' : 'done'}"></div>

        <div class="step ${s == 'Pending' || s == 'AwaitingPickup' ? 'pending-step' : 'done'}">
          <div class="step-icon"><i class="fa-solid fa-truck"></i></div>
          <div class="step-label">Đã Giao Cho ĐVVC</div>
          <div class="step-time">${order.shippingAtFormatted}</div>
        </div>
        <div class="step-line ${s == 'Completed' || s == 'Cancelled' || s == 'Returned' ? 'done' : ''}"></div>

        <div class="step ${s == 'Completed' ? 'done' : (s == 'Cancelled' ? 'cancelled-step' : (s == 'Returned' ? 'returned-step' : 'pending-step'))}">
          <div class="step-icon">
            <c:choose>
              <c:when test="${s == 'Cancelled'}"><i class="fa-solid fa-ban"></i></c:when>
              <c:when test="${s == 'Returned'}"><i class="fa-solid fa-rotate-left"></i></c:when>
              <c:otherwise><i class="fa-solid fa-box-open"></i></c:otherwise>
            </c:choose>
          </div>
          <div class="step-label">
            <c:choose>
              <c:when test="${s == 'Cancelled'}">Đã Hủy</c:when>
              <c:when test="${s == 'Returned'}">Trả Hàng</c:when>
              <c:otherwise>Đã Nhận Được Hàng</c:otherwise>
            </c:choose>
          </div>
          <div class="step-time">
            <c:choose>
            <c:when test="${s == 'Completed'}">${order.completedAtFormatted}</c:when>
            <c:when test="${s == 'Cancelled'}">${order.cancelledAtFormatted}</c:when>
            <c:when test="${s == 'Returned'}">${order.returnedAtFormatted}</c:when>
            <c:otherwise>—</c:otherwise>
            </c:choose>
          </div>
        </div>
        <div class="step-line ${s == 'Completed' ? 'done' : ''}"></div>

        <div class="step ${s == 'Completed' ? 'done' : 'pending-step'}">
          <div class="step-icon"><i class="fa-solid fa-star"></i></div>
          <div class="step-label">Đánh Giá</div>
          <div class="step-time">—</div>
        </div>
      </div>
    </div>

    <div class="detail-card">
      <div class="card-header">
        <i class="fa-solid fa-clock-rotate-left"></i> Lịch sử vận chuyển
        <c:if test="${not empty order.trackingCode}">
                    <span class="tracking-chip">
                        <i class="fa-solid fa-barcode"></i> ${order.trackingCode}
                    </span>
        </c:if>
      </div>

      <div class="timeline">

        <c:if test="${order.status == 'Completed'}">
          <div class="tl-item tl-active">
            <div class="tl-dot tl-dot--done"><i class="fa-solid fa-check"></i></div>
            <div class="tl-body">
              <div class="tl-time">${order.completedAtFormatted}</div>
              <div class="tl-title">Đã giao</div>
              <div class="tl-desc tl-desc--green">Giao hàng thành công</div>
            </div>
          </div>
        </c:if>

        <c:if test="${order.status == 'Cancelled'}">
          <div class="tl-item tl-active">
            <div class="tl-dot tl-dot--cancel"><i class="fa-solid fa-ban"></i></div>
            <div class="tl-body">
              <div class="tl-time">${order.cancelledAtFormatted}</div>
              <div class="tl-title">Đơn hàng đã bị hủy</div>
            </div>
          </div>
        </c:if>

        <c:if test="${order.status == 'Returned'}">
          <div class="tl-item tl-active">
            <div class="tl-dot tl-dot--cancel"><i class="fa-solid fa-rotate-left"></i></div>
            <div class="tl-body">
              <div class="tl-time">${order.returnedAtFormatted}</div>
              <div class="tl-title">Yêu cầu trả hàng/hoàn tiền</div>
            </div>
          </div>
        </c:if>

        <c:if test="${order.status == 'Shipping' || order.status == 'Completed'}">
          <div class="tl-item">
            <div class="tl-dot tl-dot--done"><i class="fa-solid fa-truck"></i></div>
            <div class="tl-body">
              <div class="tl-time">${order.shippingAtFormatted}</div>
              <div class="tl-title">Đang vận chuyển</div>
              <div class="tl-desc">Đơn hàng sẽ sớm được giao, vui lòng chú ý điện thoại</div>
            </div>
          </div>
        </c:if>

        <c:if test="${order.status == 'AwaitingPickup' || order.status == 'Shipping' || order.status == 'Completed'}">
          <div class="tl-item">
            <div class="tl-dot tl-dot--done"><i class="fa-solid fa-box"></i></div>
            <div class="tl-body">
              <div class="tl-time">${order.confirmedAtFormatted}</div>
              <div class="tl-title">Đang được chuẩn bị</div>
              <div class="tl-desc">Người bán đang chuẩn bị hàng</div>
            </div>
          </div>
        </c:if>

        <div class="tl-item">
          <div class="tl-dot tl-dot--done"><i class="fa-solid fa-file-lines"></i></div>
          <div class="tl-body">
            <div class="tl-time">${order.orderDateFormatted}</div>
            <div class="tl-title">Đặt hàng thành công</div>
            <div class="tl-desc">Đơn hàng đã được đặt</div>
          </div>
        </div>

      </div>
    </div>

    <div class="detail-card">
      <div class="card-header"><i class="fa-solid fa-location-dot"></i> Địa chỉ nhận hàng</div>
      <div class="addr-grid">
        <div class="addr-left">
          <div class="addr-name">${order.recipientName}</div>
          <div class="addr-phone">(+84) ${order.shippingPhone}</div>
          <div class="addr-text">${order.shippingAddress}</div>
        </div>
        <c:if test="${not empty order.trackingCode}">
          <div class="addr-right">
            <div class="provider-label">
              <c:choose>
                <c:when test="${order.shippingProvider == 'express'}">SPX Express</c:when>
                <c:otherwise>SPX Standard</c:otherwise>
              </c:choose>
            </div>
            <div class="provider-code">${order.trackingCode}</div>
          </div>
        </c:if>
      </div>
    </div>

    <div class="detail-card">
      <div class="card-header"><i class="fa-solid fa-book-open"></i> Sản phẩm</div>
      <c:forEach var="itemData" items="${items}">
        <c:set var="item" value="${itemData.item}"/>
        <c:set var="comic" value="${itemData.comic}"/>
        <c:if test="${comic != null}">
          <div class="product-row">
            <img src="${comic.thumbnailUrl}" alt="${comic.nameComics}" class="product-thumb"
                 onerror="this.src='https://via.placeholder.com/80x110?text=No+Image'">
            <div class="product-meta">
              <div class="product-name">${comic.nameComics}</div>
              <div class="product-qty">Số lượng: x${item.quantity}</div>
              <div class="product-price">
                <c:choose>
                  <c:when test="${itemData.hasActiveFlashSale}">
                    <del class="price-orig">
                      <fmt:formatNumber value="${comic.price}" type="number" groupingUsed="true"/>đ
                    </del>
                    <span class="price-final">${itemData.formattedDisplayPrice}đ</span>
                    <span class="badge-flash">⚡ Flash Sale -${itemData.flashSaleDiscount}%</span>
                  </c:when>
                  <c:otherwise>
                    <c:if test="${comic.discountPrice < comic.price}">
                      <del class="price-orig">
                        <fmt:formatNumber value="${comic.price}" type="number" groupingUsed="true"/>đ
                      </del>
                    </c:if>
                    <span class="price-final">${itemData.formattedDisplayPrice}đ</span>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
            <div class="product-subtotal">
              <fmt:formatNumber value="${item.priceAtPurchase * item.quantity}" type="number" groupingUsed="true"/>đ
            </div>
          </div>
        </c:if>
      </c:forEach>


      <div class="order-summary">
        <c:set var="rawItemTotal" value="0"/>
        <c:forEach var="itemData" items="${items}">
          <c:set var="rawItemTotal"
                 value="${rawItemTotal + itemData.item.priceAtPurchase * itemData.item.quantity}"/>
        </c:forEach>
        <div class="summary-row">
          <span>Tổng tiền hàng</span>
          <span><fmt:formatNumber value="${rawItemTotal}" type="number" groupingUsed="true"/>đ</span>
        </div>
        <div class="summary-row">
          <span>Phí vận chuyển</span>
          <span><fmt:formatNumber value="${order.shippingFee + order.shippingDiscount}" type="number" groupingUsed="true"/>đ</span>
        </div>

          <c:if test="${order.shippingDiscount != null and order.shippingDiscount > 0}">
            <div class="summary-row discount-row">
              <span>Giảm giá phí vận chuyển</span>
              <span>-<fmt:formatNumber value="${order.shippingDiscount}" type="number" groupingUsed="true"/>đ</span>
            </div>
          </c:if>

          <c:if test="${order.productDiscount != null and order.productDiscount > 0}">
            <div class="summary-row discount-row">
              <span>Giảm giá sản phẩm</span>
              <span>-<fmt:formatNumber value="${order.productDiscount}" type="number" groupingUsed="true"/>đ</span>
            </div>
          </c:if>

        <c:if test="${order.pointUsed > 0}">
          <div class="summary-row discount-row">
            <span>Giảm giá C-Point</span>
            <span>-<fmt:formatNumber value="${order.pointUsed}" type="number" groupingUsed="true"/>đ</span>
          </div>
        </c:if>
        <div class="summary-row total-row">
          <span>Thành tiền</span>
          <span class="total-amount"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</span>
        </div>
        <div class="summary-row payment-method-row">
          <span>Phương thức thanh toán</span>
          <span>${paymentMethod != null ? paymentMethod : 'COD'}</span>
        </div>
      </div>
    </div>

  </div>
</main>
<jsp:include page="/frontend/public/Footer.jsp"/>
</body>
</html>
