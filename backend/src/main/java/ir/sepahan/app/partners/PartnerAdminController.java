package ir.sepahan.app.partners;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** مدیریت رکورد Partnerها -- فقط نقش admin (طبق rbac-matrix.md). پیاده‌سازی حداقلی؛ Admin Panel واقعی Phase 14 است. */
@RestController
@RequestMapping("/api/v1/partners/admin")
@PreAuthorize("hasRole('admin')")
public class PartnerAdminController {

    private final PartnerService partnerService;
    private final PartnerRepository partnerRepository;

    public PartnerAdminController(PartnerService partnerService, PartnerRepository partnerRepository) {
        this.partnerService = partnerService;
        this.partnerRepository = partnerRepository;
    }

    @PostMapping
    public ResponseEntity<PartnerCreatedResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        PartnerService.CreatedPartner created = partnerService.createPartner(
                request.name(), request.slug(), request.contactEmail(), request.contactPhone());
        return ResponseEntity.status(HttpStatus.CREATED).body(PartnerCreatedResponse.of(created));
    }

    @GetMapping
    public List<PartnerResponse> list() {
        return partnerRepository.findByDeletedAtIsNull().stream().map(PartnerResponse::of).toList();
    }
}
