package ir.sepahan.app.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * دنبال‌کردن سبک یک درخواست در بین Logهای این Backend (بدون OpenTelemetry/Trace Backend
 * جدید -- طبق تصمیم صریح کارفرما در Phase 17، ADR-0019). یک {@code X-Request-Id} از
 * فراخوانی‌کننده می‌پذیرد (مثلاً Admin Panel/Mobile در آینده) یا خودش می‌سازد، در MDC
 * می‌گذارد (پس در هر خط Log ساختاریافته حاضر است -- {@code logging.structured.format.file})،
 * و در Header پاسخ هم برمی‌گرداند تا Client بتواند همان مقدار را برای گزارش خطا ذخیره کند.
 *
 * <p>{@code Order(HIGHEST_PRECEDENCE)} عمداً است: باید قبل از زنجیره‌ی امنیتی اسپرینگ اجرا
 * شود تا حتی درخواست‌های رد‌شده (۴۰۱/۴۰۳) هم Request Id داشته باشند.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_NAME);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // حیاتی: Tomcat Threadها را بین درخواست‌ها دوباره استفاده می‌کند -- بدون این پاک‌سازی،
            // Request Id یک درخواست قبلی می‌تواند در Logهای درخواست بعدی روی همان Thread درز کند.
            MDC.remove(MDC_KEY);
        }
    }
}
