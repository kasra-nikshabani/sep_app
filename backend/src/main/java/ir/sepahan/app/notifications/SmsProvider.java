package ir.sepahan.app.notifications;

/** Provider Interface طبق ADR-0005/بند ۷-۸ بریف -- کد دامنه هرگز مستقیم به یک شرکت خاص SMS وابسته نیست. */
public interface SmsProvider {

    String name();

    /** @throws NotificationProviderException در صورت شکست -- توسط NotificationService گرفته می‌شود. */
    void send(String mobile, String message);
}
