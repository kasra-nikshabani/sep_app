package ir.sepahan.app.notifications;

/** فقط داخلی -- توسط NotificationService گرفته می‌شود و هرگز به فراخوانی‌کننده منتقل نمی‌شود (Best-effort). */
public class NotificationProviderException extends RuntimeException {

    public NotificationProviderException(String message) {
        super(message);
    }

    public NotificationProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
