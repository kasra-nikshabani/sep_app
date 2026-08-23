package ir.sepahan.app.partners;

import jakarta.validation.constraints.NotBlank;

public record CreatePartnerRequest(@NotBlank String name, @NotBlank String slug, String contactEmail, String contactPhone) {
}
