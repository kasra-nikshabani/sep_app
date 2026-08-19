package ir.sepahan.app.ticketing;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * رکورد دائمی بلیط بعد از تکمیل خرید. طبق docs/database/erd-ticketing.md،
 * qr_payload فعلاً یک مقدار Placeholder است -- امضای رمزنگاری‌شده‌ی واقعی
 * موضوع فاز بعدی (بعد از Payment Architecture واقعی، Phase 9) است.
 */
@Entity
@Table(name = "ticket", schema = "ticketing")
public class Ticket extends BaseEntity {

    @OneToOne(optional = false)
    private EventSeat eventSeat;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber;

    @Column(name = "qr_payload", nullable = false)
    private String qrPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.valid;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    protected Ticket() {
        // JPA
    }

    public Ticket(EventSeat eventSeat, UUID userId, String ticketNumber, String qrPayload) {
        this.eventSeat = eventSeat;
        this.userId = userId;
        this.ticketNumber = ticketNumber;
        this.qrPayload = qrPayload;
        this.issuedAt = OffsetDateTime.now();
    }

    public EventSeat getEventSeat() {
        return eventSeat;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }
}
