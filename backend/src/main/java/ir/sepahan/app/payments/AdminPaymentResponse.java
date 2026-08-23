package ir.sepahan.app.payments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPaymentResponse(
        UUID id,
        PaymentPurpose purpose,
        UUID referenceId,
        UUID userId,
        String provider,
        String trackId,
        BigDecimal amount,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static AdminPaymentResponse of(Payment payment) {
        return new AdminPaymentResponse(
                payment.getId(),
                payment.getPurpose(),
                payment.getReferenceId(),
                payment.getUserId(),
                payment.getProvider(),
                payment.getTrackId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
