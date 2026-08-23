package ir.sepahan.app.notifications;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ارسال دستی/آزمایشی و مشاهده‌ی دفترکل Notification -- فقط نقش admin (طبق rbac-matrix.md:
 * «ارسال Notification/Broadcast» فقط 🛠 است). اتصال خودکار سایر ماژول‌ها به این سرویس
 * (مثلاً پیامک تأیید سفارش) موضوع فازهای بعدی است که آن رویداد را نیاز داشته باشند.
 */
@RestController
@RequestMapping("/api/v1/notifications/admin")
@PreAuthorize("hasRole('admin')")
public class NotificationAdminController {

    private final NotificationService notificationService;
    private final NotificationLogRepository logRepository;

    public NotificationAdminController(NotificationService notificationService, NotificationLogRepository logRepository) {
        this.notificationService = notificationService;
        this.logRepository = logRepository;
    }

    @PostMapping("/sms")
    public NotificationLogResponse sendSms(@Valid @RequestBody SendSmsRequest request) {
        return NotificationLogResponse.of(notificationService.sendSms(request.mobile(), request.message()));
    }

    @PostMapping("/email")
    public NotificationLogResponse sendEmail(@Valid @RequestBody SendEmailRequest request) {
        return NotificationLogResponse.of(notificationService.sendEmail(request.to(), request.subject(), request.body()));
    }

    @PostMapping("/push")
    public NotificationLogResponse sendPush(@Valid @RequestBody SendPushRequest request) {
        return NotificationLogResponse.of(notificationService.sendPush(request.deviceToken(), request.title(), request.body()));
    }

    @GetMapping("/logs")
    public List<NotificationLogResponse> logs() {
        return logRepository.findByOrderByCreatedAtDesc().stream().map(NotificationLogResponse::of).toList();
    }
}
