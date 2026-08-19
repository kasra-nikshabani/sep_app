package ir.sepahan.app.ticketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateEventRequest(
        @NotNull UUID venueId,
        @NotBlank String title,
        String description,
        @NotNull OffsetDateTime startsAt,
        Integer durationMinutes,
        @NotNull @Positive BigDecimal basePrice
) {
}
