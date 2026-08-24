package ir.sepahan.app.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * پیش‌فرض در همه‌ی محیط‌ها مگر تغییر صریح تنظیمات -- هم‌الگوی FakeSmsProvider/FakeEmailProvider.
 * تا Phase 15 (بدون اپ موبایل واقعی) تنها پیاده‌سازی ممکن بود؛ از Phase 16 به بعد
 * {@link FirebaseCloudMessagingPushProvider} پیاده‌سازی واقعی است (ADR-0018).
 */
@Component
@ConditionalOnProperty(name = "sepahan.notifications.push.provider", havingValue = "fake", matchIfMissing = true)
public class FakePushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FakePushProvider.class);

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public void send(String deviceToken, String title, String body) {
        log.info("[FakePushProvider] ارسال شبیه‌سازی‌شده به Token {}... -- عنوان: {}",
                deviceToken.length() > 8 ? deviceToken.substring(0, 8) : deviceToken, title);
    }
}
