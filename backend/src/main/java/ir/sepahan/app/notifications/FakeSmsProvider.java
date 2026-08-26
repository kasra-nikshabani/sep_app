package ir.sepahan.app.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** پیش‌فرض در همه‌ی محیط‌ها مگر تغییر صریح تنظیمات -- بدون تماس شبکه‌ای واقعی، هم‌الگوی FakePaymentProvider. */
@Component
@ConditionalOnProperty(name = "sepahan.notifications.sms.provider", havingValue = "fake", matchIfMissing = true)
public class FakeSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(FakeSmsProvider.class);

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public void send(String mobile, String message) {
        // شماره‌ی کامل و متن پیام عمداً Log نمی‌شوند -- این Logهای JSON از Phase 17 به Loki
        // ارسال می‌شوند؛ همان دلیلی که FakePushProvider از قبل Token را کامل Log نمی‌کرد (Phase 19).
        log.info("[FakeSmsProvider] ارسال شبیه‌سازی‌شده به {}***", mobile.length() > 4 ? mobile.substring(0, 4) : mobile);
    }
}
