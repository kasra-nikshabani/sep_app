package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * تنها پیاده‌سازی فعلی {@link ShippingProvider} -- فقط هزینه را طبق نرخ Configurable
 * (نه Hardcode) محاسبه می‌کند، بدون هیچ اتصال شبکه‌ای واقعی.
 */
@Component
public class InternalShippingProvider implements ShippingProvider {

    private static final BigDecimal GRAMS_PER_KG = BigDecimal.valueOf(1000);

    @Override
    public String name() {
        return "internal-manual";
    }

    @Override
    public BigDecimal calculateCost(ShippingMethod method, int totalWeightGrams) {
        BigDecimal weightKg = BigDecimal.valueOf(totalWeightGrams).divide(GRAMS_PER_KG, 3, RoundingMode.HALF_UP);
        return method.getBaseRate()
                .add(method.getPerKgRate().multiply(weightKg))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
