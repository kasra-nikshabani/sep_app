package ir.sepahan.app.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * شمارنده‌ی باخت مسابقه در الگوی JIT Provisioning (users/loyalty) -- کشف Phase 15/16.
 * افزایش این عدد به‌خودی‌خود نشانه‌ی خطا نیست (طبق طراحی، Recovery بدون خطا برای کاربر انجام
 * می‌شود)؛ فقط دید عملیاتی واقعی روی این‌که این هم‌زمانی چقدر واقعاً رخ می‌دهد -- که پیش از
 * این فاز اصلاً قابل مشاهده نبود (فقط از طریق ۵۰۰ واقعی کاربر کشف شد).
 */
@Component
public class JitRaceMetrics {

    private final MeterRegistry meterRegistry;

    public JitRaceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordConflict(String entity) {
        meterRegistry.counter("sepahan.jit.race.conflicts", "entity", entity).increment();
    }
}
