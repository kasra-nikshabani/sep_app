package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String name,
        String slug,
        String description,
        String imageUrl,
        BigDecimal basePrice,
        boolean active,
        List<ProductVariantResponse> variants
) {
    public static ProductResponse of(Product product, List<ProductVariant> variants) {
        return new ProductResponse(
                product.getId(), product.getCategory().getId(), product.getCategory().getName(),
                product.getName(), product.getSlug(), product.getDescription(), product.getImageUrl(),
                product.getBasePrice(), product.isActive(),
                variants.stream().map(ProductVariantResponse::of).toList());
    }
}
