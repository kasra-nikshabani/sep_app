package ir.sepahan.app.loyalty;

import java.util.UUID;

public record AccountResponse(UUID id, int pointsBalance, int lifetimePoints, String levelName) {
    public static AccountResponse of(LoyaltyAccount account) {
        return new AccountResponse(account.getId(), account.getPointsBalance(), account.getLifetimePoints(),
                account.getLevel() != null ? account.getLevel().getName() : null);
    }
}
