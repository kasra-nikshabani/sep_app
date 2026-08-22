package ir.sepahan.app.shop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, UUID> {

    List<ShippingMethod> findByActiveTrueAndDeletedAtIsNull();

    Optional<ShippingMethod> findByIdAndActiveTrueAndDeletedAtIsNull(UUID id);
}
