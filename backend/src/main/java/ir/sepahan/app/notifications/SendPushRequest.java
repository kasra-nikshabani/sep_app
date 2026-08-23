package ir.sepahan.app.notifications;

import jakarta.validation.constraints.NotBlank;

public record SendPushRequest(@NotBlank String deviceToken, @NotBlank String title, @NotBlank String body) {
}
