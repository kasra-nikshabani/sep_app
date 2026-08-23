package ir.sepahan.app.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * تست واحد ساده (بدون Spring Context) -- NotificationService فقط به سه Interface و دو
 * Repository نیاز دارد، همه Mockable؛ هدف اصلی اثبات این است که شکست یک Provider هرگز به
 * فراخوانی‌کننده پرتاب نمی‌شود (Best-effort، ADR-0015).
 */
class NotificationServiceTest {

    private final SmsProvider smsProvider = mock(SmsProvider.class);
    private final EmailProvider emailProvider = mock(EmailProvider.class);
    private final PushProvider pushProvider = mock(PushProvider.class);
    private final NotificationLogRepository logRepository = mock(NotificationLogRepository.class);
    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);

    private final NotificationService service =
            new NotificationService(smsProvider, emailProvider, pushProvider, logRepository, deviceTokenRepository);

    @Test
    void sendSms_success_recordsSentLog() {
        when(smsProvider.name()).thenReturn("fake");
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationLog log = service.sendSms("09120000000", "سلام");

        assertThat(log.getStatus()).isEqualTo(NotificationStatus.sent);
        assertThat(log.getChannel()).isEqualTo(NotificationChannel.sms);
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void sendSms_providerThrows_neverPropagates_recordsFailedLogInstead() {
        when(smsProvider.name()).thenReturn("sms_ir");
        doThrow(new NotificationProviderException("sms.ir با کد ۱۰۲ رد کرد"))
                .when(smsProvider).send(any(), any());
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationLog log = service.sendSms("09120000000", "سلام");

        assertThat(log.getStatus()).isEqualTo(NotificationStatus.failed);
        assertThat(log.getErrorMessage()).contains("۱۰۲");
    }

    @Test
    void sendEmail_success_recordsSentLogWithSubject() {
        when(emailProvider.name()).thenReturn("fake");
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationLog log = service.sendEmail("fan@example.com", "خوش‌آمدید", "متن ایمیل");

        assertThat(log.getStatus()).isEqualTo(NotificationStatus.sent);
        assertThat(log.getSubject()).isEqualTo("خوش‌آمدید");
    }

    @Test
    void sendPushToUser_fansOutToAllActiveDeviceTokens() {
        UUID userId = UUID.randomUUID();
        when(pushProvider.name()).thenReturn("fake");
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deviceTokenRepository.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId)).thenReturn(List.of(
                new DeviceToken(userId, "token-a", DevicePlatform.android),
                new DeviceToken(userId, "token-b", DevicePlatform.ios)));

        List<NotificationLog> logs = service.sendPushToUser(userId, "عنوان", "متن");

        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.getStatus() == NotificationStatus.sent);
    }

    @Test
    void sendPushToUser_noDeviceTokens_returnsEmptyList_doesNotFail() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId)).thenReturn(List.of());

        List<NotificationLog> logs = service.sendPushToUser(userId, "عنوان", "متن");

        assertThat(logs).isEmpty();
    }
}
