package ir.sepahan.app.shop;

import java.math.BigDecimal;

public record OrderItemResponse(String productName, String sku, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
    public static OrderItemResponse of(OrderItem item) {
        return new OrderItemResponse(item.getProductName(), item.getVariantSku(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
