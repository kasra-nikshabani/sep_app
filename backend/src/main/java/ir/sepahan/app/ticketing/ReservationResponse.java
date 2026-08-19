package ir.sepahan.app.ticketing;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(UUID id, UUID eventSeatId, OffsetDateTime expiresAt) {
    public static ReservationResponse of(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getEventSeat().getId(), reservation.getExpiresAt());
    }
}
