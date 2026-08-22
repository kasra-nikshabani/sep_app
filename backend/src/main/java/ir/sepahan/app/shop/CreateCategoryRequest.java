package ir.sepahan.app.shop;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCategoryRequest(@NotBlank String name, @NotBlank String slug, UUID parentId) {
}
