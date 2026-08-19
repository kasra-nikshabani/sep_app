package ir.sepahan.app.ticketing;

import jakarta.validation.constraints.NotBlank;

public record CreateVenueRequest(@NotBlank String name, String city, String address) {
}
