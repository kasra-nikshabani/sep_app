package ir.sepahan.app.loyalty;

import jakarta.validation.constraints.NotBlank;

public record AdjustPointsRequest(int delta, @NotBlank String reason) {
}
