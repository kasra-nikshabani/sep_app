package ir.sepahan.app.loyalty;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * {@code lifetimePoints} هرگز با خرج‌کردن امتیاز کم نمی‌شود -- سطح عضویت بر همین مبنا
 * محاسبه می‌شود (نه روی {@code pointsBalance})، تا خرج‌کردن امتیاز باعث افت سطح نشود (ADR-0014).
 */
@Entity
@Table(name = "account", schema = "loyalty")
public class LoyaltyAccount extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance;

    @Column(name = "lifetime_points", nullable = false)
    private int lifetimePoints;

    @ManyToOne
    private LoyaltyLevel level;

    protected LoyaltyAccount() {
        // JPA
    }

    public LoyaltyAccount(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public int getLifetimePoints() {
        return lifetimePoints;
    }

    public LoyaltyLevel getLevel() {
        return level;
    }

    public void setLevel(LoyaltyLevel level) {
        this.level = level;
    }
}
