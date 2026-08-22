package ir.sepahan.app.payments;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * طبق ADR-0011: مستقل از هر Order خاص -- {@code referenceId} فقط یک UUID عمومی
 * است (مثلاً شناسه‌ی یک ticketing.Reservation)، بدون FK فیزیکی Cross-schema
 * (طبق docs/database/conventions.md).
 */
@Entity
@Table(name = "payment", schema = "payments")
public class Payment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentPurpose purpose;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "track_id", length = 100)
    private String trackId;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.pending;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response")
    private String rawResponse;

    protected Payment() {
        // JPA
    }

    public Payment(PaymentPurpose purpose, UUID referenceId, UUID userId, String provider, BigDecimal amount) {
        this.purpose = purpose;
        this.referenceId = referenceId;
        this.userId = userId;
        this.provider = provider;
        this.amount = amount;
    }

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
