package ir.sepahan.app.ticketing;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** یک اجرای مشخص تئاتر در یک Venue، در تاریخ/ساعت معین. */
@Entity
@Table(name = "event", schema = "ticketing")
public class Event extends BaseEntity {

    @ManyToOne(optional = false)
    private Venue venue;

    @Column(nullable = false, length = 200)
    private String title;

    @Column
    private String description;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 120;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.draft;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal basePrice;

    protected Event() {
        // JPA
    }

    public Event(Venue venue, String title, OffsetDateTime startsAt, BigDecimal basePrice) {
        this.venue = venue;
        this.title = title;
        this.startsAt = startsAt;
        this.basePrice = basePrice;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
