package ir.sepahan.app.notifications;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * پیاده‌سازی واقعی روی FCM HTTP v1 (از طریق Firebase Admin SDK رسمی -- OAuth2 خودکار از روی
 * Service Account، بدون پیاده‌سازی دستی امضای JWT)، تحقیق‌شده در همین فاز -- نه حدس. جزئیات
 * کامل و مقایسه با مسیر Expo Push Service در ADR-0018. مسیر مستقیم FCM انتخاب شد چون هم‌الگوی
 * بقیه‌ی Providerهای پروژه (SMS.ir/SMTP/Zibal) است -- بدون واسطه‌ی شخص‌ثالث.
 */
@Component
@ConditionalOnProperty(name = "sepahan.notifications.push.provider", havingValue = "fcm")
public class FirebaseCloudMessagingPushProvider implements PushProvider {

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseCloudMessagingPushProvider(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public String name() {
        return "fcm";
    }

    @Override
    @CircuitBreaker(name = "fcm")
    @Retry(name = "fcm")
    public void send(String deviceToken, String title, String body) {
        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            // UNREGISTERED یعنی اپ حذف شده یا Token دیگر معتبر نیست -- طبق مستندات رسمی FCM
            // (نه هر خطای دیگری، مثل خطای موقتی شبکه/سرویس) -- NotificationService این نوع
            // خاص را می‌گیرد تا DeviceToken متناظر را غیرفعال کند.
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                throw new DeviceTokenUnregisteredException("FCM: Token دیگر معتبر نیست (UNREGISTERED)");
            }
            throw new NotificationProviderException("ارسال FCM ناموفق بود: " + e.getMessagingErrorCode(), e);
        }
    }
}
