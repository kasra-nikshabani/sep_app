package ir.sepahan.app.payments;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * تنها نقطه‌ی ورود ماژول‌های دیگر (ticketing، در آینده shop/wallet) به Payment --
 * طبق ADR-0011 هیچ‌کدام نباید مستقیماً PaymentProvider را صدا بزنند.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider provider;
    private final ApplicationEventPublisher eventPublisher;
    private final String callbackBaseUrl;

    public PaymentService(PaymentRepository paymentRepository, PaymentProvider provider,
                           ApplicationEventPublisher eventPublisher,
                           @Value("${sepahan.payments.callback-base-url}") String callbackBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.provider = provider;
        this.eventPublisher = eventPublisher;
        this.callbackBaseUrl = callbackBaseUrl;
    }

    /**
     * Idempotent طبق ADR-0011: اگر Payment در انتظار برای همین (purpose, referenceId)
     * از قبل وجود دارد (مثلاً کاربر دکمه را دوبار زده)، همان را برمی‌گرداند -- Payment
     * تکراری نمی‌سازد و به Provider هم دوباره درخواست نمی‌زند.
     */
    @Transactional
    public Payment createPayment(PaymentPurpose purpose, UUID referenceId, UUID userId, BigDecimal amount, String description) {
        Payment existing = paymentRepository.findByPurposeAndReferenceIdAndStatus(purpose, referenceId, PaymentStatus.pending)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Payment payment = new Payment(purpose, referenceId, userId, provider.name(), amount);
        payment = paymentRepository.save(payment);

        String callbackUrl = callbackBaseUrl + "/api/v1/payments/" + payment.getId() + "/callback";
        PaymentRequestResult result = provider.createPayment(
                new CreatePaymentCommand(amount, callbackUrl, description, payment.getId().toString()));

        payment.setTrackId(result.trackId());
        payment.setRedirectUrl(result.redirectUrl());
        return paymentRepository.save(payment);
    }

    /**
     * هرگز به پارامترهای Query String کال‌بک اعتماد نمی‌کند -- همیشه سرور-به-سرور Verify
     * می‌کند (طبق ADR-0011، مستقیماً از مستندات رسمی Zibal). Idempotent: اگر قبلاً
     * پردازش شده (paid/failed)، دوباره Event منتشر نمی‌شود.
     *
     * مبلغ بازگشتی از Provider با مبلغ اصلی Payment مقایسه می‌شود -- درسی که مستقیماً
     * از یک آسیب‌پذیری واقعی قبلی («Fix critical payment amount tampering vulnerability»
     * در سیستم فوتبال Django) گرفته شده: هرگز نباید صرفاً به موفقیت Verify اکتفا کرد،
     * باید مطمئن شد دقیقاً همان مبلغی که درخواست شده پرداخت شده.
     */
    @Transactional
    public Payment handleCallback(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.pending) {
            return payment; // قبلاً پردازش شده -- Idempotent، نه خطا
        }

        PaymentVerificationResult verification = provider.verify(payment.getTrackId());
        payment.setRawResponse(verification.rawJson());

        boolean amountMatches = verification.amount() == null
                || verification.amount().compareTo(payment.getAmount()) == 0;

        if (verification.success() && amountMatches) {
            payment.setStatus(PaymentStatus.paid);
            paymentRepository.save(payment);
            eventPublisher.publishEvent(new PaymentSucceededEvent(
                    payment.getId(), payment.getPurpose(), payment.getReferenceId(), payment.getUserId(), payment.getAmount()));
        } else {
            payment.setStatus(PaymentStatus.failed);
            paymentRepository.save(payment);
        }

        return payment;
    }
}
