package ir.sepahan.app.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

public record AddVariantRequest(
        @NotBlank String sku,
        Map<String, String> attributes,
        @Min(0) int initialStock,
        Integer weightGrams,
        BigDecimal priceOverride
) {
}
