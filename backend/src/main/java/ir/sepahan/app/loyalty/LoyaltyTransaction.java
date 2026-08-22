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
import org.hibernate.annotations.UuidGenerator;

/**
 * دفترکل امتیاز -- Append-only، عمداً بدون common.BaseEntity (مثل OrderItem/ArticleRevision).
 * {@code sourceType}/{@code sourceReferenceId} فقط برای رویدادهای earn پر می‌شوند (Idempotency
 * رویدادهای بیرونی -- خرید Shop/تئاتر/فوتبال)؛ برای redeem/adjust همیشه NULL می‌مانند.
 */
@Entity
@Table(name = "transaction", schema = "loyalty")
public class LoyaltyTransaction {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private LoyaltyAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoyaltyTransactionType type;

    @Column(nullable = false)
    private int points;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LoyaltyTransaction() {
        // JPA
    }

    public LoyaltyTransaction(LoyaltyAccount account, LoyaltyTransactionType type, int points,
                               String sourceType, UUID sourceReferenceId, String description) {
        this.account = account;
        this.type = type;
        this.points = points;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public LoyaltyAccount getAccount() {
        return account;
    }

    public LoyaltyTransactionType getType() {
        return type;
    }

    public int getPoints() {
        return points;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceReferenceId() {
        return sourceReferenceId;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
