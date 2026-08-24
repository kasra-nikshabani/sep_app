package ir.sepahan.app.notifications;

/**
 * FCM/APNs توکن را نامعتبر یا لغوشده اعلام کرده (مثلاً اپ حذف شده). زیرنوع خاصی از
 * {@link NotificationProviderException} است تا {@link NotificationService#sendPushToUser}
 * بتواند دقیقاً همین حالت را از بقیه‌ی خطاهای موقتی (شبکه/سرویس) تشخیص دهد و آن
 * {@link DeviceToken} را غیرفعال کند -- بدون این تشخیص، ارسال بعدی دوباره به همان توکن مرده
 * تلاش می‌کرد.
 */
public class DeviceTokenUnregisteredException extends NotificationProviderException {

    public DeviceTokenUnregisteredException(String message) {
        super(message);
    }
}
