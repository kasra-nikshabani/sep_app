package ir.sepahan.app.payments;

public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String provider, int resultCode, String message) {
        super("خطای Provider %s (result=%d): %s".formatted(provider, resultCode, message));
    }
}
