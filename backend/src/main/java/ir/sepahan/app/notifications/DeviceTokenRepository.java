package ir.sepahan.app.notifications;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserIdAndActiveTrueAndDeletedAtIsNull(UUID userId);

    Optional<DeviceToken> findByTokenAndDeletedAtIsNull(String token);
}
