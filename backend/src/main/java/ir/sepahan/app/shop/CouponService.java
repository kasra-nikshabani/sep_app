package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * اعتبارسنجی/مصرف کد تخفیف. {@link #validateForRedemption} باید داخل همان
 * Transaction ساخت سفارش صدا زده شود (طبق ADR-0012) -- چون قفل بدبینانه‌ی
 * {@link CouponRepository#findByCodeForUpdate} فقط تا پایان همان Transaction نگه‌داشته می‌شود.
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    public CouponService(CouponRepository couponRepository, CouponRedemptionRepository couponRedemptionRepository) {
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
    }

    public Coupon validateForRedemption(String code, UUID userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new CouponInvalidException("کد تخفیف نامعتبر است"));

        if (!coupon.isActive()) {
            throw new CouponInvalidException("این کد تخفیف غیرفعال است");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new CouponInvalidException("این کد تخفیف هنوز فعال نشده است");
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            throw new CouponInvalidException("این کد تخفیف منقضی شده است");
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CouponInvalidException("حداقل مبلغ سفارش برای این کد " + coupon.getMinOrderAmount() + " ریال است");
        }
        if (coupon.getMaxUsesTotal() != null
                && couponRedemptionRepository.countByCouponId(coupon.getId()) >= coupon.getMaxUsesTotal()) {
            throw new CouponInvalidException("ظرفیت استفاده از این کد تخفیف تمام شده است");
        }
        if (coupon.getMaxUsesPerUser() != null
                && couponRedemptionRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getMaxUsesPerUser()) {
            throw new CouponInvalidException("شما قبلاً از این کد تخفیف استفاده کرده‌اید");
        }
        return coupon;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = switch (coupon.getDiscountType()) {
            case percentage -> subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            case fixed_amount -> coupon.getDiscountValue();
        };
        return discount.min(subtotal);
    }

    public void recordRedemption(Coupon coupon, Order order, UUID userId) {
        couponRedemptionRepository.save(new CouponRedemption(coupon, order, userId));
    }
}
