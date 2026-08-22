package ir.sepahan.app.shop;

import jakarta.validation.constraints.NotBlank;

public record RequestReturnRequest(@NotBlank String reason) {
}
