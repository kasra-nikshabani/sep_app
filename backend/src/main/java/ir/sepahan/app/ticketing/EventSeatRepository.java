package ir.sepahan.app.ticketing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {

    List<EventSeat> findByEventId(UUID eventId);

    Optional<EventSeat> findByIdAndEventId(UUID id, UUID eventId);
}
