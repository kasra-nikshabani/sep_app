package ir.sepahan.app.payments;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPurposeAndReferenceIdAndStatus(PaymentPurpose purpose, UUID referenceId, PaymentStatus status);

    Optional<Payment> findByTrackId(String trackId);
}
