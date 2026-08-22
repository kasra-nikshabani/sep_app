package ir.sepahan.app.shop;

import java.math.BigDecimal;

/**
 * Provider Interface طبق ADR-0005 -- حتی وقتی فعلاً فقط یک پیاده‌سازی داخلی
 * (بدون اتصال واقعی به شرکت باربری/پستی) وجود دارد، طوری طراحی شده که افزودن یک
 * Provider واقعی در آینده (مثلاً پست ایران/تیپاکس) نیازمند تغییر در ShippingService
 * یا کد Order نباشد -- دقیقاً همان الگویی که برای Payments (ADR-0011) استفاده شد.
 */
public interface ShippingProvider {

    String name();

    /** هزینه‌ی ارسال بر اساس روش انتخابی و وزن کل بسته (گرم). */
    BigDecimal calculateCost(ShippingMethod method, int totalWeightGrams);

    /**
     * ثبت مرسوله نزد یک شرکت باربری/پستی واقعی. طبق تصمیم صریح کارفرما در Phase 10،
     * هنوز هیچ Provider واقعی متصل نیست -- نه حدس‌زدن یک API که مستندش دیده نشده.
     */
    default String dispatch(java.util.UUID orderId) {
        throw new UnsupportedOperationException(
                name() + " اتصال واقعی به شرکت باربری/پستی ندارد -- طبق ADR-0012 فقط محاسبه‌ی داخلی هزینه پیاده‌سازی شده");
    }
}
