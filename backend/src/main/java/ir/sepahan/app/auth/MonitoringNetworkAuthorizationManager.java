package ir.sepahan.app.auth;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * فقط محدوده‌های شبکه‌ی خصوصی (RFC 1918) + Loopback را برای مسیرهایی مثل
 * {@code /actuator/prometheus} مجاز می‌کند (Phase 19، ADR-0021) -- دقیقاً همان بازه‌هایی که
 * Docker Bridge (Prometheus داخل Compose)، یک VPC داخلی، یا خودِ Host از آن استفاده می‌کنند.
 * یک درخواست از یک IP عمومی واقعی (اینترنت) با هیچ‌کدام مطابقت پیدا نمی‌کند.
 *
 * <p>در یک کلاس جدا (نه Lambda داخل SecurityConfig) عمداً قرار گرفته تا مستقیم و بدون بالا
 * آوردن یک Server واقعی تست شود ({@code MockHttpServletRequest.setRemoteAddr(...)}).
 *
 * <p>بدون Reverse Proxy جلوی این Backend، {@code getRemoteAddr()} دقیقاً همان IP واقعی TCP
 * Peer است -- اگر در Phase 20 (Deployment) یک Reverse Proxy اضافه شود، این بررسی باید با
 * اعتماد صریح به X-Forwarded-For (نه پیش‌فرض) بازنگری شود.
 */
public class MonitoringNetworkAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final List<IpAddressMatcher> ALLOWED_NETWORKS = List.of(
            new IpAddressMatcher("127.0.0.1/32"),
            new IpAddressMatcher("::1/128"), // "localhost" گاهی به IPv6 Resolve می‌شود، نه فقط IPv4
            new IpAddressMatcher("172.16.0.0/12"),
            new IpAddressMatcher("192.168.0.0/16"),
            new IpAddressMatcher("10.0.0.0/8"));

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        boolean allowed = ALLOWED_NETWORKS.stream().anyMatch(matcher -> matcher.matches(context.getRequest()));
        return new AuthorizationDecision(allowed);
    }
}
