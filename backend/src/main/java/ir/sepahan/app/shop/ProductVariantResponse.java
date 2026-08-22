package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        Map<String, String> attributes,
        BigDecimal price,
        int stockQuantity,
        int weightGrams,
        boolean active
) {
    public static ProductVariantResponse of(ProductVariant variant) {
        return new ProductVariantResponse(variant.getId(), variant.getSku(), variant.getAttributes(),
                variant.getEffectivePrice(), variant.getStockQuantity(), variant.getWeightGrams(), variant.isActive());
    }
}
