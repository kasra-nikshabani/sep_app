package ir.sepahan.app.ticketing;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReserveSeatRequest(@NotNull UUID eventSeatId) {
}
