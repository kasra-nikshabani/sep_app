package ir.sepahan.app.notifications;

public interface EmailProvider {

    String name();

    /** @throws NotificationProviderException در صورت شکست -- توسط NotificationService گرفته می‌شود. */
    void send(String to, String subject, String body);
}
