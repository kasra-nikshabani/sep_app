package ir.sepahan.app.news;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ScheduleArticleRequest(@NotNull OffsetDateTime scheduledAt) {
}
