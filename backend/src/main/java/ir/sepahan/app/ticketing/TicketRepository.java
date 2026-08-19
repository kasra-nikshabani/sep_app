package ir.sepahan.app.ticketing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByUserIdAndDeletedAtIsNull(UUID userId);
}
