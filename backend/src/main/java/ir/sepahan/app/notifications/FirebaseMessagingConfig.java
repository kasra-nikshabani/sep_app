package ir.sepahan.app.notifications;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * فقط وقتی Push واقعاً روی fcm تنظیم شده باشد بالا می‌آید -- بدون این شرط، نبود فایل Service
 * Account (که در توسعه‌ی محلی طبیعی است) باعث شکست کل Startup برنامه می‌شد، نه فقط غیرفعال‌ماندن
 * یک Provider (ADR-0018).
 */
@Configuration
@ConditionalOnProperty(name = "sepahan.notifications.push.provider", havingValue = "fcm")
public class FirebaseMessagingConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${sepahan.notifications.push.fcm.service-account-path}") String serviceAccountPath) throws IOException {
        try (FileInputStream credentialsStream = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        }
    }
}
