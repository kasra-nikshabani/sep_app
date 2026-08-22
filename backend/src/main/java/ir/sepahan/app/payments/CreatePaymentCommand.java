package ir.sepahan.app.payments;

import java.math.BigDecimal;

public record CreatePaymentCommand(BigDecimal amount, String callbackUrl, String description, String orderId) {
}
