package ir.sepahan.app.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * پیاده‌سازی واقعی طبق مستندات رسمی Zibal (help.zibal.ir/IPG/API) که در Phase 9
 * بررسی و با درخواست واقعی به Sandbox عمومی (merchant: zibal) تأیید شد -- جزئیات
 * کامل در ADR-0011. هیچ فیلدی حدس زده نشده.
 */
@Component
@ConditionalOnProperty(name = "sepahan.payments.provider", havingValue = "zibal")
public class ZibalPaymentProvider implements PaymentProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String merchant;

    public ZibalPaymentProvider(
            @Value("${sepahan.payments.zibal.base-url}") String baseUrl,
            @Value("${sepahan.payments.zibal.merchant}") String merchant,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.merchant = merchant;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "zibal";
    }

    @Override
    @CircuitBreaker(name = "zibal")
    @Retry(name = "zibal")
    @SuppressWarnings("unchecked")
    public PaymentRequestResult createPayment(CreatePaymentCommand command) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchant", merchant);
        body.put("amount", command.amount().longValueExact());
        body.put("callbackUrl", command.callbackUrl());
        body.put("description", command.description() == null ? "" : command.description());
        if (command.orderId() != null) {
            body.put("orderId", command.orderId());
        }

        Map<String, Object> response = restClient.post().uri("/v1/request")
                .body(body)
                .retrieve()
                .body(Map.class);

        int result = asInt(response.get("result"));
        if (result != 100) {
            throw new PaymentProviderException("zibal", result, String.valueOf(response.get("message")));
        }
        String trackId = String.valueOf(response.get("trackId"));
        return new PaymentRequestResult(trackId, "https://gateway.zibal.ir/start/" + trackId);
    }

    @Override
    @CircuitBreaker(name = "zibal")
    @Retry(name = "zibal")
    @SuppressWarnings("unchecked")
    public PaymentVerificationResult verify(String trackId) {
        Map<String, Object> body = Map.of("merchant", merchant, "trackId", Long.parseLong(trackId));
        Map<String, Object> response = restClient.post().uri("/v1/verify")
                .body(body)
                .retrieve()
                .body(Map.class);

        int result = asInt(response.get("result"));
        // طبق ADR-0011 (جدول کدهای Verify رسمی): 100=تازه تأییدشد، 201=قبلاً تأیید شده -- هر دو یعنی واقعاً پرداخت‌شده
        boolean success = result == 100 || result == 201;
        BigDecimal amount = toAmount(response.get("amount"));
        return new PaymentVerificationResult(success, String.valueOf(response.get("message")), amount, toJson(response));
    }

    @Override
    @CircuitBreaker(name = "zibal")
    @Retry(name = "zibal")
    @SuppressWarnings("unchecked")
    public PaymentInquiryResult inquire(String trackId) {
        Map<String, Object> body = Map.of("merchant", merchant, "trackId", Long.parseLong(trackId));
        Map<String, Object> response = restClient.post().uri("/v1/inquiry")
                .body(body)
                .retrieve()
                .body(Map.class);

        int result = asInt(response.get("result"));
        int gatewayStatus = response.get("status") != null ? asInt(response.get("status")) : -1;
        BigDecimal amount = toAmount(response.get("amount"));
        return new PaymentInquiryResult(result == 100, String.valueOf(response.get("message")), gatewayStatus, amount, toJson(response));
    }

    private int asInt(Object value) {
        return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal toAmount(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
