package ir.sepahan.app.partners;

import static org.assertj.core.api.Assertions.assertThat;

import ir.sepahan.app.TestcontainersConfig;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * روی Postgres واقعی (طبق backend/README.md) تا جست‌وجوی واقعی بر اساس Hash کلید
 * تأیید شود -- Mock کردن این منطق شکننده می‌بود (باید Hash را در تست هم بازتولید می‌کردیم).
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class PartnerServiceTest {

    @Autowired
    private PartnerService partnerService;
    @Autowired
    private PartnerRepository partnerRepository;

    @AfterEach
    void tearDown() {
        partnerRepository.deleteAll();
    }

    @Test
    void createPartner_rawKeyIsNeverStored_onlyItsHash() {
        PartnerService.CreatedPartner created = partnerService.createPartner(
                "باشگاه همکار تست", "partner-test", "contact@example.com", "09120000000");

        assertThat(created.rawApiKey()).isNotBlank();
        assertThat(created.partner().getApiKeyHash()).isNotEqualTo(created.rawApiKey());
        assertThat(created.partner().getApiKeyHash()).hasSize(64); // SHA-256 hex
    }

    @Test
    void authenticate_withCorrectRawKey_returnsThePartner() {
        PartnerService.CreatedPartner created = partnerService.createPartner(
                "باشگاه همکار تست", "partner-test", null, null);

        Optional<Partner> found = partnerService.authenticate(created.rawApiKey());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.partner().getId());
    }

    @Test
    void authenticate_withWrongKey_returnsEmpty() {
        partnerService.createPartner("باشگاه همکار تست", "partner-test", null, null);

        assertThat(partnerService.authenticate("pk_definitely-wrong-key")).isEmpty();
    }

    @Test
    void authenticate_withBlankKey_returnsEmpty_doesNotThrow() {
        assertThat(partnerService.authenticate("")).isEmpty();
        assertThat(partnerService.authenticate(null)).isEmpty();
    }

    @Test
    void authenticate_forInactivePartner_returnsEmpty() {
        PartnerService.CreatedPartner created = partnerService.createPartner(
                "باشگاه غیرفعال", "inactive-partner", null, null);
        Partner partner = created.partner();
        partner.setActive(false);
        partnerRepository.save(partner);

        assertThat(partnerService.authenticate(created.rawApiKey())).isEmpty();
    }
}
