package ir.sepahan.app.ticketing;

import java.math.BigDecimal;
import java.util.UUID;

public record EventSeatResponse(
        UUID id, String section, String rowLabel, String seatNumber, EventSeatStatus status, BigDecimal price
) {
    public static EventSeatResponse of(EventSeat seat) {
        VenueSeat vs = seat.getVenueSeat();
        return new EventSeatResponse(seat.getId(), vs.getSection(), vs.getRowLabel(), vs.getSeatNumber(),
                seat.getStatus(), seat.getPrice());
    }
}
