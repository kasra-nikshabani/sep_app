package ir.sepahan.app.ticketing;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(UUID id, String ticketNumber, TicketStatus status, OffsetDateTime issuedAt) {
    public static TicketResponse of(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTicketNumber(), ticket.getStatus(), ticket.getIssuedAt());
    }
}
