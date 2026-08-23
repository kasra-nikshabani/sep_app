package ir.sepahan.app.notifications;

public interface PushProvider {

    String name();

    /** @throws NotificationProviderException در صورت شکست -- توسط NotificationService گرفته می‌شود. */
    void send(String deviceToken, String title, String body);
}
