package ir.sepahan.app.loyalty;

import ir.sepahan.app.users.AppUserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * منبع امتیاز خرید بلیط فوتبال از سیستم Django -- طبق ADR-0002/ADR-0014. طبق تصمیم صریح کارفرما،
 * Polling (نه Webhook) از یک Endpoint جدید و فقط-خواندنی در Django انتخاب شد: منطق خرید/پرداخت
 * Django عمداً کاملاً دست‌نخورده می‌ماند -- فقط یک مسیر گزارش‌گیری به آن اضافه شده.
 *
 * الگوی Cursor: هیچ‌وقت بر اساس ساعت خودمان جلو نمی‌رود -- فقط بر اساس بیشینه‌ی paid_at واقعی
 * دیده‌شده از Django (طبق پاسخ خودش)، به‌علاوه‌ی یک بازه‌ی همپوشانی ثابت در هر Query. این یعنی
 * حتی اگر یک سفارش کمی دیرتر از حد انتظار در Django Commit شود، Poll بعدی دوباره آن را می‌بیند --
 * و چون هر رویداد با (football_ticket_purchase, ارجاع بلیط) در سطح دیتابیس Idempotent است
 * (ADR-0014)، دیدن دوباره‌ی همان سفارش هرگز امتیاز تکراری نمی‌دهد.
 */
@Service
public class DjangoOrderPollingService {

    private static final Logger log = LoggerFactory.getLogger(DjangoOrderPollingService.class);
    private static final String SOURCE = "django_football_ticket";
    private static final Duration OVERLAP = Duration.ofMinutes(5);

    private final ExternalPollCursorRepository cursorRepository;
    private final LoyaltyAccountService loyaltyAccountService;
    private final AppUserRepository appUserRepository;
    private final RestClient restClient;
    private final String serviceToken;
    private final Duration initialLookback;

    public DjangoOrderPollingService(ExternalPollCursorRepository cursorRepository,
                                      LoyaltyAccountService loyaltyAccountService,
                                      AppUserRepository appUserRepository,
                                      @Value("${sepahan.loyalty.django.base-url}") String baseUrl,
                                      @Value("${sepahan.loyalty.django.service-token}") String serviceToken,
                                      @Value("${sepahan.loyalty.django.initial-lookback-days:1}") long initialLookbackDays) {
        this.cursorRepository = cursorRepository;
        this.loyaltyAccountService = loyaltyAccountService;
        this.appUserRepository = appUserRepository;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
        this.initialLookback = Duration.ofDays(initialLookbackDays);
    }

    @Scheduled(fixedDelayString = "${sepahan.loyalty.django.poll-interval-ms:300000}")
    @Transactional
    public void pollCompletedOrders() {
        OffsetDateTime cursor = cursorRepository.findById(SOURCE)
                .map(ExternalPollCursor::getLastCursorAt)
                .orElse(OffsetDateTime.now().minus(initialLookback));
        OffsetDateTime queryFrom = cursor.minus(OVERLAP);

        // toInstant().toString() عمداً به‌جای queryFrom.toString(): OffsetDateTime آفست غیر-UTC
        // را با "+" رندر می‌کند (مثلاً "+03:30")؛ "+" در Query String خام معادل Space دیکد
        // می‌شود (RFC 3986) و RestClient اینجا آن را Encode نمی‌کند -- باگ واقعی که مستقیماً
        // با اجرای این تست همین فاز (۴۰۰ از Django) کشف شد. فرمت Instant همیشه با "Z" است،
        // بدون "+"، پس این مشکل اصلاً پیش نمی‌آید.
        DjangoCompletedOrdersResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tickets/api/loyalty/completed-orders/")
                            .queryParam("since", queryFrom.toInstant().toString())
                            .build())
                    .header("Authorization", "Bearer " + serviceToken)
                    .retrieve()
                    .body(DjangoCompletedOrdersResponse.class);
        } catch (RestClientException e) {
            // شکست موقت (شبکه/Downtime Django) -- در Poll بعدی از همان Cursor دوباره تلاش می‌شود
            log.warn("Polling سفارش‌های فوتبال از Django ناموفق بود -- در دور بعدی دوباره تلاش می‌شود: {}", e.getMessage());
            return;
        }
        if (response == null || response.orders().isEmpty()) {
            return;
        }

        OffsetDateTime maxSeen = cursor;
        for (DjangoCompletedOrder order : response.orders()) {
            OffsetDateTime paidAt = OffsetDateTime.parse(order.paidAt());
            if (paidAt.isAfter(maxSeen)) {
                maxSeen = paidAt;
            }
            creditPointsIfMappable(order);
        }

        upsertCursor(maxSeen);
    }

    private void creditPointsIfMappable(DjangoCompletedOrder order) {
        if (order.fanIdSubject() == null || order.fanIdSubject().isBlank()) {
            // کاربر قدیمی بدون ورود از طریق Fan ID -- طبق ADR-0014 هنوز قابل تطبیق با AppUser نیست
            log.info("سفارش فوتبال {} بدون fan_id_subject -- امتیازی داده نشد", order.orderNumber());
            return;
        }
        UUID keycloakSubject;
        try {
            keycloakSubject = UUID.fromString(order.fanIdSubject());
        } catch (IllegalArgumentException invalid) {
            log.warn("سفارش فوتبال {} با fan_id_subject نامعتبر: {}", order.orderNumber(), order.fanIdSubject());
            return;
        }

        appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject).ifPresentOrElse(
                user -> loyaltyAccountService.earnPoints(user.getId(), EarningSourceType.football_ticket_purchase,
                        externalReferenceId(order.orderNumber()), BigDecimal.valueOf(order.amount())),
                () -> log.info("سفارش فوتبال {} به هیچ AppUser ای در sepapp نگاشت نشد (هنوز هرگز وارد این Backend نشده)",
                        order.orderNumber()));
    }

    /** UUID پایدار و قطعی از روی شناسه‌ی طبیعی سفارش Django (که خودش عدد صحیح است، نه UUID). */
    private UUID externalReferenceId(String orderNumber) {
        return UUID.nameUUIDFromBytes(("django_order:" + orderNumber).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void upsertCursor(OffsetDateTime newCursor) {
        ExternalPollCursor row = cursorRepository.findById(SOURCE).orElse(new ExternalPollCursor(SOURCE, newCursor));
        row.setLastCursorAt(newCursor);
        cursorRepository.save(row);
    }
}
