package ir.sepahan.app.payments;

import java.math.BigDecimal;

public record PaymentInquiryResult(boolean success, String message, int gatewayStatus, BigDecimal amount, String rawJson) {
}
