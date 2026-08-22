package ir.sepahan.app.loyalty;

import java.math.BigDecimal;
import java.util.UUID;

public record EarningRuleResponse(UUID id, EarningSourceType sourceType, BigDecimal pointsPerAmount, boolean active) {
    public static EarningRuleResponse of(PointsEarningRule rule) {
        return new EarningRuleResponse(rule.getId(), rule.getSourceType(), rule.getPointsPerAmount(), rule.isActive());
    }
}
