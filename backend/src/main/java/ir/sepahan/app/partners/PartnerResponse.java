package ir.sepahan.app.partners;

import java.util.UUID;

public record PartnerResponse(UUID id, String name, String slug, String contactEmail, String contactPhone, boolean active) {
    public static PartnerResponse of(Partner partner) {
        return new PartnerResponse(partner.getId(), partner.getName(), partner.getSlug(),
                partner.getContactEmail(), partner.getContactPhone(), partner.isActive());
    }
}
