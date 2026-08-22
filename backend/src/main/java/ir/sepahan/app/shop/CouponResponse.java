package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.UUID;

public record CouponResponse(UUID id, String code, CouponDiscountType discountType, BigDecimal discountValue, boolean active) {
    public static CouponResponse of(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.isActive());
    }
}
