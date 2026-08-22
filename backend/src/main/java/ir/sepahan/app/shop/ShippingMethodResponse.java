package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.UUID;

public record ShippingMethodResponse(UUID id, String name, BigDecimal baseRate, BigDecimal perKgRate) {
    public static ShippingMethodResponse of(ShippingMethod method) {
        return new ShippingMethodResponse(method.getId(), method.getName(), method.getBaseRate(), method.getPerKgRate());
    }
}
