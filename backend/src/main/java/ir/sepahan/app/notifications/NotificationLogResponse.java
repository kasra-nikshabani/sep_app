package ir.sepahan.app.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        NotificationChannel channel,
        String recipient,
        String subject,
        NotificationStatus status,
        String providerName,
        String errorMessage,
        OffsetDateTime createdAt
) {
    public static NotificationLogResponse of(NotificationLog log) {
        return new NotificationLogResponse(log.getId(), log.getChannel(), log.getRecipient(), log.getSubject(),
                log.getStatus(), log.getProviderName(), log.getErrorMessage(), log.getCreatedAt());
    }
}
