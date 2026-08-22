package ir.sepahan.app.news;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(@NotBlank String name, @NotBlank String slug) {
}
