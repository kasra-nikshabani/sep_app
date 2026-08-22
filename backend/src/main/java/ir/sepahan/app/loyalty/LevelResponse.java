package ir.sepahan.app.loyalty;

import java.util.UUID;

public record LevelResponse(UUID id, String name, int minPoints, String benefits) {
    public static LevelResponse of(LoyaltyLevel level) {
        return new LevelResponse(level.getId(), level.getName(), level.getMinPoints(), level.getBenefits());
    }
}
