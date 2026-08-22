package ir.sepahan.app.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateRewardRequest(@NotBlank String name, String description, @Positive int pointsCost, @PositiveOrZero Integer stockQuantity) {
}
