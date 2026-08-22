package ir.sepahan.app.payments;

import java.math.BigDecimal;

public record PaymentVerificationResult(boolean success, String message, BigDecimal amount, String rawJson) {
}
