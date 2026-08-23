package ir.sepahan.app.notifications;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * دفترکل هر تلاش ارسال (موفق یا ناموفق) -- برای Audit/عیب‌یابی (بند ۸ بریف: Logging جزء
 * الزامات هر Integration است). status=failed هرگز باعث شکست جریان اصلی فراخوانی‌کننده
 * نمی‌شود -- ارسال Notification همیشه Best-effort است (ADR-0015).
 */
@Entity
@Table(name = "notification_log", schema = "notifications")
public class NotificationLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column
    private String subject;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "provider_name", nullable = false, length = 40)
    private String providerName;

    @Column(name = "error_message")
    private String errorMessage;

    protected NotificationLog() {
        // JPA
    }

    public NotificationLog(NotificationChannel channel, String recipient, String subject, String content,
                            NotificationStatus status, String providerName, String errorMessage) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.providerName = providerName;
        this.errorMessage = errorMessage;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
