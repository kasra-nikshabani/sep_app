package ir.sepahan.app.ticketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * قفل موقت یک EventSeat تا زمانی که کاربر خرید را تکمیل کند. طبق ADR-0010،
 * منبع حقیقتِ اصلی قفل Redis است؛ این ردیف برای گزارش‌گیری و پاک‌سازی
 * دوره‌ای است، نه خودِ مکانیزم Race-safety.
 *
 * عمداً بدون common.BaseEntity — یک رکورد کاملاً موقت است، نه چیزی که
 * Soft-delete/Audit به آن معنا بدهد (وقتی منقضی شود، Hard-delete می‌شود).
 */
@Entity
@Table(name = "reservation", schema = "ticketing")
public class Reservation {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(optional = false)
    private EventSeat eventSeat;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Reservation() {
        // JPA
    }

    public Reservation(EventSeat eventSeat, UUID userId, OffsetDateTime expiresAt) {
        this.eventSeat = eventSeat;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public EventSeat getEventSeat() {
        return eventSeat;
    }

    public UUID getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
