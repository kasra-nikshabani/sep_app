package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShippingService {

    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingProvider provider;

    public ShippingService(ShippingMethodRepository shippingMethodRepository, ShippingProvider provider) {
        this.shippingMethodRepository = shippingMethodRepository;
        this.provider = provider;
    }

    public List<ShippingMethod> listActiveMethods() {
        return shippingMethodRepository.findByActiveTrueAndDeletedAtIsNull();
    }

    public ShippingMethod requireActiveMethod(UUID id) {
        return shippingMethodRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "روش ارسال یافت نشد"));
    }

    public BigDecimal calculateCost(ShippingMethod method, int totalWeightGrams) {
        return provider.calculateCost(method, totalWeightGrams);
    }
}
