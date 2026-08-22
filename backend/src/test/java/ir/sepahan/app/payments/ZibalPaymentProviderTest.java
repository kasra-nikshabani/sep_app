package ir.sepahan.app.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * تست واحد ساده (بدون Context اسپرینگ) روی Sandbox واقعی و عمومی Zibal
 * (merchant: "zibal") -- طبق ADR-0011. نیاز به دسترسی اینترنت به gateway.zibal.ir
 * دارد؛ اگر آن Sandbox در دسترس نبود، این تست به‌طور طبیعی Fail می‌شود -- Mock
 * نشده چون هدف دقیقاً اثبات صحت پیاده‌سازی در برابر API واقعی است (نه حدسی).
 */
class ZibalPaymentProviderTest {

    private final ZibalPaymentProvider provider = new ZibalPaymentProvider(
            "https://gateway.zibal.ir", "zibal", new ObjectMapper());

    @Test
    void createPayment_thenInquire_reflectsPendingState() {
        PaymentRequestResult created = provider.createPayment(
                new CreatePaymentCommand(new BigDecimal("150000"), "https://example.com/callback", "تست Phase 9", null));

        assertThat(created.trackId()).isNotBlank();
        assertThat(created.redirectUrl()).contains(created.trackId());

        PaymentInquiryResult inquiry = provider.inquire(created.trackId());
        assertThat(inquiry.success()).isTrue();
        assertThat(inquiry.gatewayStatus()).isEqualTo(-1); // «در انتظار پرداخت» طبق جدول وضعیت‌های رسمی
        assertThat(inquiry.amount()).isEqualByComparingTo("150000");
    }

    @Test
    void verify_onUnpaidTransaction_isNotSuccessful() {
        PaymentRequestResult created = provider.createPayment(
                new CreatePaymentCommand(new BigDecimal("150000"), "https://example.com/callback", "تست Phase 9", null));

        PaymentVerificationResult verification = provider.verify(created.trackId());

        // طبق جدول کدهای Verify رسمی: 202 = «سفارش پرداخت نشده یا ناموفق بوده است»
        assertThat(verification.success()).isFalse();
    }

    @Test
    void createPayment_belowMinimumAmount_throwsWithDocumentedResultCode() {
        // طبق مستندات رسمی: کد 105 = amount باید بزرگ‌تر از ۱٬۰۰۰ ریال باشد
        assertThatThrownBy(() -> provider.createPayment(
                new CreatePaymentCommand(new BigDecimal("500"), "https://example.com/callback", "تست", null)))
                .isInstanceOf(PaymentProviderException.class)
                .hasMessageContaining("105");
    }
}
