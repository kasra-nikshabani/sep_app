package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String shippingMethodName,
        String shippingRecipientName,
        String shippingCity,
        OffsetDateTime expiresAt,
        boolean requiresManualReview,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse of(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(), order.getStatus(), order.getSubtotalAmount(), order.getDiscountAmount(),
                order.getShippingAmount(), order.getTotalAmount(), order.getShippingMethod().getName(),
                order.getShippingRecipientName(), order.getShippingCity(), order.getExpiresAt(),
                order.isRequiresManualReview(), order.getCreatedAt(),
                items.stream().map(OrderItemResponse::of).toList());
    }
}
