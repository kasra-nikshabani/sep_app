package ir.sepahan.app.partners;

import java.util.UUID;

/** apiKey فقط همین یک‌بار، در پاسخ ساخت، نمایش داده می‌شود -- هرگز بعداً قابل بازیابی نیست. */
public record PartnerCreatedResponse(UUID id, String name, String slug, String apiKey) {
    public static PartnerCreatedResponse of(PartnerService.CreatedPartner created) {
        return new PartnerCreatedResponse(created.partner().getId(), created.partner().getName(),
                created.partner().getSlug(), created.rawApiKey());
    }
}
