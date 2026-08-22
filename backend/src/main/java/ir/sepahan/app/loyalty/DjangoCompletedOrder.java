package ir.sepahan.app.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;

/** طبق ADR-0014 -- شکل دقیق پاسخ Endpoint جدید Django (tickets/api_loyalty.py، تأییدشده با تست واقعی). */
public record DjangoCompletedOrder(
        @JsonProperty("order_id") long orderId,
        @JsonProperty("order_number") String orderNumber,
        @JsonProperty("fan_id_subject") String fanIdSubject,
        @JsonProperty("amount") long amount,
        @JsonProperty("paid_at") String paidAt
) {
}
