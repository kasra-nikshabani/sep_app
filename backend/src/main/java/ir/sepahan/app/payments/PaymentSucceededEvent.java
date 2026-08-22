package ir.sepahan.app.payments;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * منتشر می‌شود بعد از Verify موفق سرور-به-سرور. طبق ADR-0011، ماژول‌های مصرف‌کننده
 * (مثلاً ticketing) این را می‌شنوند بدون این‌که payments چیزی از آن‌ها بداند.
 */
public record PaymentSucceededEvent(UUID paymentId, PaymentPurpose purpose, UUID referenceId, UUID userId, BigDecimal amount) {
}
