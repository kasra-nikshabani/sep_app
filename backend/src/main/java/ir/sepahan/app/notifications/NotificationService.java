package ir.sepahan.app.notifications;

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

    public NotificationService(SmsProvider smsProvider, EmailProvider emailProvider, PushProvider pushProvider,
                                NotificationLogRepository logRepository, DeviceTokenRepository deviceTokenRepository) {
        this.smsProvider = smsProvider;
        this.emailProvider = emailProvider;
        this.pushProvider = pushProvider;
        this.logRepository = logRepository;
        this.deviceTokenRepository = deviceTokenRepository;
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

    /** ارسال به همه‌ی دستگاه‌های فعال کاربر -- اگر کاربر هیچ دستگاه ثبت‌شده‌ای نداشته باشد، فهرست خالی برمی‌گردد. */
    public List<NotificationLog> sendPushToUser(UUID userId, String title, String body) {
        return deviceTokenRepository.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId).stream()
                .map(deviceToken -> sendPush(deviceToken.getToken(), title, body))
                .toList();
    }

    private NotificationLog attempt(NotificationChannel channel, String recipient, String subject, String content,
                                     String providerName, Runnable action) {
        NotificationLog log;
        try {
            action.run();
            log = new NotificationLog(channel, recipient, subject, content, NotificationStatus.sent, providerName, null);
        } catch (NotificationProviderException e) {
            log = new NotificationLog(channel, recipient, subject, content, NotificationStatus.failed, providerName, e.getMessage());
        }
        return logRepository.save(log);
    }
}
