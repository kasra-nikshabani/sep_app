package ir.sepahan.app.notifications;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * پیاده‌سازی واقعی طبق مستندات رسمی SMS.ir (sms.ir/rest-api/، بررسی‌شده در همین فاز -- نه حدس)،
 * جزئیات کامل در ADR-0015. این همان Provider ای است که سیستم فوتبال Django از قبل واقعاً
 * استفاده می‌کند (SMS_IR_API_KEY در تنظیمات آن) -- یکسان‌سازی Provider بین دو سیستم.
 */
@Component
@ConditionalOnProperty(name = "sepahan.notifications.sms.provider", havingValue = "sms_ir")
public class SmsIrProvider implements SmsProvider {

    private final RestClient restClient;
    private final long lineNumber;

    public SmsIrProvider(@Value("${sepahan.notifications.sms.sms-ir.base-url}") String baseUrl,
                          @Value("${sepahan.notifications.sms.sms-ir.api-key}") String apiKey,
                          @Value("${sepahan.notifications.sms.sms-ir.line-number}") long lineNumber) {
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
        this.lineNumber = lineNumber;
    }

    @Override
    public String name() {
        return "sms_ir";
    }

    @Override
    @CircuitBreaker(name = "sms_ir")
    @Retry(name = "sms_ir")
    @SuppressWarnings("unchecked")
    public void send(String mobile, String message) {
        Map<String, Object> body = Map.of(
                "lineNumber", lineNumber,
                "messageText", message,
                "mobiles", List.of(mobile));

        Map<String, Object> response = restClient.post().uri("/send/bulk")
                .body(body)
                .retrieve()
                .body(Map.class);

        int status = response.get("status") instanceof Number n ? n.intValue() : -1;
        // طبق مستندات رسمی: ۱=موفق؛ سایر کدها خطا هستند (مثلاً ۱۰۲=اعتبار ناکافی، ۱۰۴=شماره نامعتبر)
        if (status != 1) {
            throw new NotificationProviderException(
                    "sms.ir با کد " + status + " رد کرد: " + response.get("message"));
        }
    }
}
