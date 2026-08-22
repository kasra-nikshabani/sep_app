package ir.sepahan.app.payments;

import java.util.UUID;

public record PaymentCallbackResponse(UUID paymentId, PaymentStatus status) {
    public static PaymentCallbackResponse of(Payment payment) {
        return new PaymentCallbackResponse(payment.getId(), payment.getStatus());
    }
}
