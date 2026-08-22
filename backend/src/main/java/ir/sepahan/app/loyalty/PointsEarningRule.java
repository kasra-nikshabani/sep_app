package ir.sepahan.app.loyalty;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** طبق بند ۱۶ بریف: منطق امتیازدهی Configurable است -- نه یک ضریب Hardcode در کد جاوا. */
@Entity
@Table(name = "earning_rule", schema = "loyalty")
public class PointsEarningRule extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private EarningSourceType sourceType;

    /** هر چند ریال/تومان معادل یک امتیاز است -- امتیاز = floor(amount / pointsPerAmount). */
    @Column(name = "points_per_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal pointsPerAmount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected PointsEarningRule() {
        // JPA
    }

    public PointsEarningRule(EarningSourceType sourceType, BigDecimal pointsPerAmount) {
        this.sourceType = sourceType;
        this.pointsPerAmount = pointsPerAmount;
    }

    public EarningSourceType getSourceType() {
        return sourceType;
    }

    public BigDecimal getPointsPerAmount() {
        return pointsPerAmount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
