package ir.sepahan.app.shop;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Order> findByIdAndDeletedAtIsNull(UUID id);

    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, OffsetDateTime cutoff);

    List<Order> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
