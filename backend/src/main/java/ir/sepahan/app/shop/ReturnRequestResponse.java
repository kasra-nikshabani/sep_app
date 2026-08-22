package ir.sepahan.app.shop;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReturnRequestResponse(
        UUID id,
        UUID orderId,
        String reason,
        ReturnStatus status,
        String adminNote,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {
    public static ReturnRequestResponse of(ReturnRequest request) {
        return new ReturnRequestResponse(request.getId(), request.getOrder().getId(), request.getReason(),
                request.getStatus(), request.getAdminNote(), request.getCreatedAt(), request.getResolvedAt());
    }
}
