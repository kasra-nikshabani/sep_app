package ir.sepahan.app.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * تنها پیاده‌سازی فعلی -- بدون اپ موبایل واقعی (Phase 15) و بدون تصمیم روی
 * FCM/APNs، اتصال Push واقعی این فاز حدس‌زدن یک API بود؛ طبق تصمیم صریح کارفرما
 * کنار گذاشته شد (ADR-0015). ثبت/لغو Device Token همچنان واقعی است.
 */
@Component
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
