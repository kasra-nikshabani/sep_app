package ir.sepahan.app.ticketing;

import ir.sepahan.app.payments.Payment;
import java.util.UUID;

public record PayReservationResponse(UUID paymentId, String redirectUrl) {
    public static PayReservationResponse of(Payment payment) {
        return new PayReservationResponse(payment.getId(), payment.getRedirectUrl());
    }
}
