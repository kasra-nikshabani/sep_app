package ir.sepahan.app.ticketing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueSeatRepository extends JpaRepository<VenueSeat, UUID> {

    List<VenueSeat> findByVenueIdAndDeletedAtIsNull(UUID venueId);
}
