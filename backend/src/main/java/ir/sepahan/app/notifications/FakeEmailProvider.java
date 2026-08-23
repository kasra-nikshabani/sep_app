package ir.sepahan.app.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sepahan.notifications.email.provider", havingValue = "fake", matchIfMissing = true)
public class FakeEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailProvider.class);

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public void send(String to, String subject, String body) {
        log.info("[FakeEmailProvider] ارسال شبیه‌سازی‌شده به {} -- موضوع: {}", to, subject);
    }
}
