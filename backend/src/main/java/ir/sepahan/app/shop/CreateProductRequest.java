package ir.sepahan.app.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        String imageUrl,
        @NotNull @Positive BigDecimal basePrice
) {
}
