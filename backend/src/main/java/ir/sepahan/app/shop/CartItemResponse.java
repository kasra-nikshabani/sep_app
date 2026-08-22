package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productVariantId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineSubtotal
) {
    public static CartItemResponse of(CartItem item) {
        BigDecimal unitPrice = item.getProductVariant().getEffectivePrice();
        return new CartItemResponse(
                item.getId(), item.getProductVariant().getId(), item.getProductVariant().getProduct().getName(),
                item.getProductVariant().getSku(), unitPrice, item.getQuantity(),
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
