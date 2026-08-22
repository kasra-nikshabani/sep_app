package ir.sepahan.app.loyalty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/** یک رکورد گردش‌کار (هم‌الگوی shop.ReturnRequest)، نه داده‌ی کاتالوگ -- عمداً بدون common.BaseEntity. */
@Entity
@Table(name = "reward_redemption", schema = "loyalty")
public class RewardRedemption {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private LoyaltyAccount account;

    @ManyToOne(optional = false)
    private LoyaltyReward reward;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    @Column(name = "redemption_code", nullable = false, length = 40)
    private String redemptionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RedemptionStatus status = RedemptionStatus.requested;

    @Column(name = "fulfilled_by")
    private UUID fulfilledBy;

    @Column(name = "fulfilled_at")
    private OffsetDateTime fulfilledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RewardRedemption() {
        // JPA
    }

    public RewardRedemption(LoyaltyAccount account, LoyaltyReward reward, int pointsSpent, String redemptionCode) {
        this.account = account;
        this.reward = reward;
        this.pointsSpent = pointsSpent;
        this.redemptionCode = redemptionCode;
    }

    public UUID getId() {
        return id;
    }

    public LoyaltyAccount getAccount() {
        return account;
    }

    public LoyaltyReward getReward() {
        return reward;
    }

    public int getPointsSpent() {
        return pointsSpent;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public RedemptionStatus getStatus() {
        return status;
    }

    public void setStatus(RedemptionStatus status) {
        this.status = status;
    }

    public UUID getFulfilledBy() {
        return fulfilledBy;
    }

    public void setFulfilledBy(UUID fulfilledBy) {
        this.fulfilledBy = fulfilledBy;
    }

    public OffsetDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(OffsetDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
