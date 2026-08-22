package ir.sepahan.app.shop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    List<ReturnRequest> findByUserId(UUID userId);

    List<ReturnRequest> findByOrderId(UUID orderId);

    Optional<ReturnRequest> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByOrderIdAndStatus(UUID orderId, ReturnStatus status);
}
