package ir.sepahan.app.loyalty;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** طبق بند ۱۶ بریف: سطوح عضویت Configurable هستند -- یک جدول عادی که ادمین مدیریت می‌کند، نه Enum ثابت. */
@Entity
@Table(name = "level", schema = "loyalty")
public class LoyaltyLevel extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_points", nullable = false)
    private int minPoints;

    @Column
    private String benefits;

    protected LoyaltyLevel() {
        // JPA
    }

    public LoyaltyLevel(String name, int minPoints, String benefits) {
        this.name = name;
        this.minPoints = minPoints;
        this.benefits = benefits;
    }

    public String getName() {
        return name;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public String getBenefits() {
        return benefits;
    }
}
