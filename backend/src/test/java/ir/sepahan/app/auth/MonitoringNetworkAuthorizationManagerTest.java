package ir.sepahan.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * تست واحد ساده (بدون Spring Context/Server واقعی) -- هدف اثبات هر دو جهت این کنترل امنیتی:
 * هم مسیر مجاز (Loopback/شبکه‌ی خصوصی) هم مسیر رد‌شده (IP عمومی واقعی). Phase 18's
 * SecurityRbacIntegrationTest فقط مسیر مجاز را از طریق TestRestTemplate واقعی اثبات می‌کند
 * (چون آن درخواست همیشه از Loopback می‌آید) -- این‌جا مسیر رد نیز مستقیم و بدون بالا آوردن
 * یک Server واقعی تست می‌شود (Phase 19، ADR-0021).
 */
class MonitoringNetworkAuthorizationManagerTest {

    private final MonitoringNetworkAuthorizationManager manager = new MonitoringNetworkAuthorizationManager();

    @Test
    void loopbackIpv4_isAllowed() {
        assertThat(decisionFor("127.0.0.1").isGranted()).isTrue();
    }

    @Test
    void loopbackIpv6_isAllowed() {
        assertThat(decisionFor("0:0:0:0:0:0:0:1").isGranted()).isTrue();
    }

    @Test
    void dockerBridgeNetwork_isAllowed() {
        assertThat(decisionFor("172.17.0.5").isGranted()).isTrue();
    }

    @Test
    void privateLanNetwork_isAllowed() {
        assertThat(decisionFor("192.168.1.50").isGranted()).isTrue();
        assertThat(decisionFor("10.0.0.5").isGranted()).isTrue();
    }

    @Test
    void realPublicIp_isRejected() {
        // 203.0.113.0/24 -- TEST-NET-3 طبق RFC 5737، عمداً برای مستندسازی/تست رزرو شده،
        // هرگز روی اینترنت واقعی Route نمی‌شود -- پس یک انتخاب امن برای «یک IP عمومی واقعی» در تست.
        assertThat(decisionFor("203.0.113.5").isGranted()).isFalse();
    }

    private AuthorizationDecision decisionFor(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        RequestAuthorizationContext context = new RequestAuthorizationContext(request);
        return manager.check(() -> null, context);
    }
}
