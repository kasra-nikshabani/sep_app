package ir.sepahan.app.payments;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * برای Dev/Test بدون نیاز به شبکه یا اعتبار Provider واقعی -- طبق ADR-0011،
 * پیش‌فرض `sepahan.payments.provider` همین است تا هیچ تماس شبکه‌ای ناخواسته‌ای
 * در توسعه/تست خودکار رخ ندهد.
 */
@Component
@ConditionalOnProperty(name = "sepahan.payments.provider", havingValue = "fake", matchIfMissing = true)
public class FakePaymentProvider implements PaymentProvider {

    private final Map<String, BigDecimal> createdPayments = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public PaymentRequestResult createPayment(CreatePaymentCommand command) {
        String trackId = "FAKE-" + UUID.randomUUID();
        createdPayments.put(trackId, command.amount());
        return new PaymentRequestResult(trackId, "http://localhost:8081/fake-gateway/" + trackId);
    }

    @Override
    public PaymentVerificationResult verify(String trackId) {
        BigDecimal amount = createdPayments.get(trackId);
        if (amount == null) {
            return new PaymentVerificationResult(false, "trackId ناشناخته", null, null);
        }
        return new PaymentVerificationResult(true, "success (fake)", amount, "{\"fake\":true}");
    }

    @Override
    public PaymentInquiryResult inquire(String trackId) {
        BigDecimal amount = createdPayments.get(trackId);
        if (amount == null) {
            return new PaymentInquiryResult(false, "trackId ناشناخته", -1, null, null);
        }
        return new PaymentInquiryResult(true, "success (fake)", 1, amount, "{\"fake\":true}");
    }
}
