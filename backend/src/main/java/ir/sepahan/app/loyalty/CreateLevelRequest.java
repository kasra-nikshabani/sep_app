package ir.sepahan.app.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateLevelRequest(@NotBlank String name, @PositiveOrZero int minPoints, String benefits) {
}
