package ir.sepahan.app.payments;

import java.math.BigDecimal;

/**
 * Provider Interface طبق ADR-0005/ADR-0011 -- هیچ کد دامنه‌ای (ticketing، ...) نباید
 * مستقیماً به یک Provider خاص وابسته باشد، فقط به این Interface.
 */
public interface PaymentProvider {

    String name();

    PaymentRequestResult createPayment(CreatePaymentCommand command);

    /** تأیید سرور-به-سرور -- تنها منبع قابل‌اعتماد برای «آیا واقعاً پرداخت شد؟» (ADR-0011). */
    PaymentVerificationResult verify(String trackId);

    PaymentInquiryResult inquire(String trackId);

    /**
     * پیش‌فرض: پشتیبانی نمی‌شود. طبق ADR-0011، Zibal هیچ Endpoint عمومی مستندی برای
     * Refund ندارد -- این یک محدودیت واقعی Provider است، نه کاستی این کد.
     */
    default RefundResult refund(String trackId, BigDecimal amount) {
        throw new UnsupportedOperationException(name() + " از Refund از طریق API پشتیبانی نمی‌کند (ADR-0011)");
    }
}
