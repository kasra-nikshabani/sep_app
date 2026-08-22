package ir.sepahan.app.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotNull CouponDiscountType discountType,
        @NotNull @Positive BigDecimal discountValue,
        BigDecimal minOrderAmount,
        Integer maxUsesTotal,
        Integer maxUsesPerUser,
        OffsetDateTime validFrom,
        OffsetDateTime validTo
) {
}
