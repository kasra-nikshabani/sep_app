package ir.sepahan.app.shop;

import ir.sepahan.app.payments.Payment;
import java.util.UUID;

public record PayOrderResponse(UUID paymentId, String redirectUrl) {
    public static PayOrderResponse of(Payment payment) {
        return new PayOrderResponse(payment.getId(), payment.getRedirectUrl());
    }
}
