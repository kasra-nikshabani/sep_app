package ir.sepahan.app.ticketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * یک صندلی فیزیکی داخل یک Venue. عمداً از common.BaseEntity استفاده نمی‌کند
 * (طبق docs/database/erd-ticketing.md) — چون نه updated_at نه created_by/updated_by
 * برای این جدول معنا ندارد؛ چیدمان صندلی بعد از ساخت اولیه تغییر نمی‌کند.
 */
@Entity
@Table(name = "venue_seat", schema = "ticketing")
public class VenueSeat {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Venue venue;

    @Column(nullable = false, length = 100)
    private String section;

    @Column(name = "row_label", nullable = false, length = 20)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected VenueSeat() {
        // JPA
    }

    public VenueSeat(Venue venue, String section, String rowLabel, String seatNumber) {
        this.venue = venue;
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
    }

    public UUID getId() {
        return id;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getSection() {
        return section;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
