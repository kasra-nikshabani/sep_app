package ir.sepahan.app.ticketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * وضعیت یک VenueSeat مشخص برای یک Event مشخص. طبق docs/database/erd-ticketing.md
 * عمداً از common.BaseEntity استفاده نمی‌کند (بدون Soft Delete/Audit — دلیل در ERD).
 *
 * status اینجا فقط برای Query/گزارش سریع است؛ منبع حقیقتِ قفل هم‌زمانی Redis است
 * (ADR-0010) نه این ستون به‌تنهایی.
 */
@Entity
@Table(name = "event_seat", schema = "ticketing")
public class EventSeat {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Event event;

    @ManyToOne(optional = false)
    private VenueSeat venueSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSeatStatus status = EventSeatStatus.available;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EventSeat() {
        // JPA
    }

    public EventSeat(Event event, VenueSeat venueSeat, BigDecimal price) {
        this.event = event;
        this.venueSeat = venueSeat;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public VenueSeat getVenueSeat() {
        return venueSeat;
    }

    public EventSeatStatus getStatus() {
        return status;
    }

    public void setStatus(EventSeatStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
