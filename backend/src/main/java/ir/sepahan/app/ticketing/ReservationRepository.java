package ir.sepahan.app.ticketing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByEventSeatId(UUID eventSeatId);

    List<Reservation> findByExpiresAtBefore(OffsetDateTime cutoff);
}
