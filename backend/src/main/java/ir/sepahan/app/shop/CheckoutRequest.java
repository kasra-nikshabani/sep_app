package ir.sepahan.app.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutRequest(
        @NotNull UUID shippingMethodId,
        String couponCode,
        @NotBlank String recipientName,
        @NotBlank String phone,
        @NotBlank String province,
        @NotBlank String city,
        @NotBlank String addressLine,
        String postalCode
) {
}
