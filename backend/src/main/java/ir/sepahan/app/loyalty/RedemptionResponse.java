package ir.sepahan.app.loyalty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RedemptionResponse(UUID id, String rewardName, int pointsSpent, String redemptionCode,
                                  RedemptionStatus status, OffsetDateTime createdAt, OffsetDateTime fulfilledAt) {
    public static RedemptionResponse of(RewardRedemption redemption) {
        return new RedemptionResponse(redemption.getId(), redemption.getReward().getName(), redemption.getPointsSpent(),
                redemption.getRedemptionCode(), redemption.getStatus(), redemption.getCreatedAt(), redemption.getFulfilledAt());
    }
}
