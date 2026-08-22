package ir.sepahan.app.loyalty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(UUID id, LoyaltyTransactionType type, int points, String sourceType,
                                   String description, OffsetDateTime createdAt) {
    public static TransactionResponse of(LoyaltyTransaction txn) {
        return new TransactionResponse(txn.getId(), txn.getType(), txn.getPoints(), txn.getSourceType(),
                txn.getDescription(), txn.getCreatedAt());
    }
}
