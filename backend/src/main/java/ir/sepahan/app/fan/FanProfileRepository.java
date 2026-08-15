package ir.sepahan.app.fan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanProfileRepository extends JpaRepository<FanProfile, UUID> {

    Optional<FanProfile> findByUserIdAndDeletedAtIsNull(UUID userId);
}
