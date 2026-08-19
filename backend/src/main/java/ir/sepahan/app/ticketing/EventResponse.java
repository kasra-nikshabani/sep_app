package ir.sepahan.app.ticketing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String venueName,
        OffsetDateTime startsAt,
        Integer durationMinutes,
        EventStatus status,
        BigDecimal basePrice
) {
    public static EventResponse of(Event event) {
        return new EventResponse(
                event.getId(), event.getTitle(), event.getDescription(), event.getVenue().getName(),
                event.getStartsAt(), event.getDurationMinutes(), event.getStatus(), event.getBasePrice());
    }
}
