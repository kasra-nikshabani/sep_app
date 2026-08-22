package ir.sepahan.app.loyalty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateEarningRuleRequest(@NotNull EarningSourceType sourceType, @NotNull @Positive BigDecimal pointsPerAmount) {
}
