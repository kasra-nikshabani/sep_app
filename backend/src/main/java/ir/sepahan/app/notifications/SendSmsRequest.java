package ir.sepahan.app.notifications;

import jakarta.validation.constraints.NotBlank;

public record SendSmsRequest(@NotBlank String mobile, @NotBlank String message) {
}
