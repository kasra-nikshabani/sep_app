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
        log.info("[FakeSmsProvider] ارسال شبیه‌سازی‌شده به {}: {}", mobile, message);
    }
}
