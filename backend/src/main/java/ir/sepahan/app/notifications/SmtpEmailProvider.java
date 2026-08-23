package ir.sepahan.app.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * پیاده‌سازی واقعی روی SMTP استاندارد (spring-boot-starter-mail، تأییدشده در همین فاز -- تنها
 * Dependency جدید این فاز). هیچ Credential واقعی SMTP برای این سیستم جدید هنوز وجود ندارد --
 * هم‌الگوی Zibal در Phase 9: پیاده‌سازی آماده است، پیکربندی واقعی قبل از Production لازم است.
 */
@Component
@ConditionalOnProperty(name = "sepahan.notifications.email.provider", havingValue = "smtp")
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailProvider(JavaMailSender mailSender, @Value("${sepahan.notifications.email.smtp.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public String name() {
        return "smtp";
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new NotificationProviderException("ارسال Email از طریق SMTP ناموفق بود", e);
        }
    }
}
