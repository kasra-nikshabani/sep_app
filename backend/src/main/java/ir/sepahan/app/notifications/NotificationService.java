package ir.sepahan.app.notifications;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * تنها نقطه‌ی ورود ماژول‌های دیگر به ارسال Notification -- طبق ADR-0005 هیچ‌کدام نباید
 * مستقیم Provider را صدا بزنند. ارسال همیشه Best-effort است: شکست Provider هرگز
 * Exception به فراخوانی‌کننده پرتاب نمی‌کند (یک پیامک ناموفق نباید یک سفارش را Rollback کند)؛
 * در عوض در {@link NotificationLog} با status=failed ثبت می‌شود.
 */
@Service
public class NotificationService {

    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;
    private final PushProvider pushProvider;
    private final NotificationLogRepository logRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final MeterRegistry meterRegistry;

    public NotificationService(SmsProvider smsProvider, EmailProvider emailProvider, PushProvider pushProvider,
                                NotificationLogRepository logRepository, DeviceTokenRepository deviceTokenRepository,
                                MeterRegistry meterRegistry) {
        this.smsProvider = smsProvider;
        this.emailProvider = emailProvider;
        this.pushProvider = pushProvider;
        this.logRepository = logRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.meterRegistry = meterRegistry;
    }

    public NotificationLog sendSms(String mobile, String message) {
        return attempt(NotificationChannel.sms, mobile, null, message, smsProvider.name(),
                () -> smsProvider.send(mobile, message));
    }

    public NotificationLog sendEmail(String to, String subject, String body) {
        return attempt(NotificationChannel.email, to, subject, body, emailProvider.name(),
                () -> emailProvider.send(to, subject, body));
    }

    public NotificationLog sendPush(String deviceToken, String title, String body) {
        return attempt(NotificationChannel.push, deviceToken, title, body, pushProvider.name(),
                () -> pushProvider.send(deviceToken, title, body));
    }

    /**
     * ارسال به همه‌ی دستگاه‌های فعال کاربر -- اگر کاربر هیچ دستگاه ثبت‌شده‌ای نداشته باشد، فهرست
     * خالی برمی‌گردد. برخلاف {@link #sendPush}، این متد مستقیم {@link DeviceToken} را در اختیار دارد
     * پس می‌تواند {@link DeviceTokenUnregisteredException} را جدا تشخیص دهد و آن رکورد را غیرفعال
     * کند -- بدون این کار، تلاش‌های بعدی دوباره به همان Token مرده (اپ حذف‌شده) ارسال می‌شدند (ADR-0018).
     */
    public List<NotificationLog> sendPushToUser(UUID userId, String title, String body) {
        return deviceTokenRepository.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId).stream()
                .map(deviceToken -> sendPushToDevice(deviceToken, title, body))
                .toList();
    }

    private NotificationLog sendPushToDevice(DeviceToken deviceToken, String title, String body) {
        try {
            pushProvider.send(deviceToken.getToken(), title, body);
            return saveLog(NotificationChannel.push, deviceToken.getToken(), title, body,
                    NotificationStatus.sent, pushProvider.name(), null);
        } catch (DeviceTokenUnregisteredException e) {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
            return saveLog(NotificationChannel.push, deviceToken.getToken(), title, body,
                    NotificationStatus.failed, pushProvider.name(), e.getMessage());
        } catch (NotificationProviderException e) {
            return saveLog(NotificationChannel.push, deviceToken.getToken(), title, body,
                    NotificationStatus.failed, pushProvider.name(), e.getMessage());
        }
    }

    /**
     * {@code attempt} عمداً هیچ Exception ای را هرگز به فراخوانی‌کننده پرتاب نمی‌کند (Best-effort،
     * طبق Javadoc کلاس) -- این ثبات برای sendSms/sendEmail/sendPush تک‌توکنی حفظ می‌شود؛ فقط
     * {@link #sendPushToDevice} (که مستقیم به DeviceToken دسترسی دارد) جدا از این مسیر مشترک،
     * خودش خطای Provider را می‌گیرد تا در صورت لزوم رکورد را غیرفعال کند.
     */
    private NotificationLog attempt(NotificationChannel channel, String recipient, String subject, String content,
                                     String providerName, Runnable action) {
        try {
            action.run();
            return saveLog(channel, recipient, subject, content, NotificationStatus.sent, providerName, null);
        } catch (NotificationProviderException e) {
            return saveLog(channel, recipient, subject, content, NotificationStatus.failed, providerName, e.getMessage());
        }
    }

    private NotificationLog saveLog(NotificationChannel channel, String recipient, String subject, String content,
                                     NotificationStatus status, String providerName, String errorMessage) {
        // Phase 17 (ADR-0019): تنها نقطه‌ی مشترک همه‌ی کانال‌ها/Providerها -- یک شمارنده این‌جا
        // دید یکپارچه می‌دهد که Resilience4j به‌تنهایی نمی‌دهد (fake/smtp اصلاً Circuit Breaker ندارند).
        meterRegistry.counter("sepahan.notifications.sent", "channel", channel.name(), "provider", providerName,
                "status", status.name()).increment();
        return logRepository.save(new NotificationLog(channel, recipient, subject, content, status, providerName, errorMessage));
    }
}
