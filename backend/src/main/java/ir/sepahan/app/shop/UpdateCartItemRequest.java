package ir.sepahan.app.shop;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@Min(0) int quantity) {
}
