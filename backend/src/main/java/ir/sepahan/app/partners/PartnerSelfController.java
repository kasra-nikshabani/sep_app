package ir.sepahan.app.partners;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * تنها مسیری که خودِ Partner (نه ادمین) می‌بیند -- طبق rbac-matrix.md، «از طریق API/Webhook
 * مجزا، نه Admin Panel». احراز هویت با کلید API خودِ Partner است، نه Keycloak (یک Partner
 * خارجی کاربر Fan ID نیست) -- به همین دلیل در SecurityConfig این مسیر permitAll است و
 * احراز هویت این‌جا دستی انجام می‌شود، هم‌الگوی Callback پرداخت (ADR-0011).
 */
@RestController
@RequestMapping("/api/v1/partners")
public class PartnerSelfController {

    private final PartnerService partnerService;

    public PartnerSelfController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/me")
    public PartnerResponse me(@RequestHeader("X-Partner-Api-Key") String apiKey) {
        Partner partner = partnerService.authenticate(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "کلید API نامعتبر است"));
        return PartnerResponse.of(partner);
    }
}
