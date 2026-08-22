package ir.sepahan.app.shop;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * هزینه‌ی ارسال = baseRate + perKgRate * وزن(kg) -- یک فرمول واحد برای هم نرخ
 * ثابت (perKgRate=0) و هم نرخ بر اساس وزن، به‌جای دو نوع/مسیر کد جدا (Configurable
 * طبق ADR-0012، بدون اتصال واقعی به هیچ شرکت باربری/پستی -- تصمیم صریح کارفرما).
 */
@Entity
@Table(name = "shipping_method", schema = "shop")
public class ShippingMethod extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_rate", nullable = false, precision = 12, scale = 0)
    private BigDecimal baseRate;

    @Column(name = "per_kg_rate", nullable = false, precision = 12, scale = 0)
    private BigDecimal perKgRate = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected ShippingMethod() {
        // JPA
    }

    public ShippingMethod(String name, BigDecimal baseRate, BigDecimal perKgRate) {
        this.name = name;
        this.baseRate = baseRate;
        this.perKgRate = perKgRate;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public BigDecimal getPerKgRate() {
        return perKgRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
