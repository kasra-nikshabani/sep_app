package ir.sepahan.app.loyalty;

import java.util.UUID;

public record RewardResponse(UUID id, String name, String description, int pointsCost, Integer stockQuantity, boolean active) {
    public static RewardResponse of(LoyaltyReward reward) {
        return new RewardResponse(reward.getId(), reward.getName(), reward.getDescription(), reward.getPointsCost(),
                reward.getStockQuantity(), reward.isActive());
    }
}
