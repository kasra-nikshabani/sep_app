package ir.sepahan.app.news;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** سرو استاتیک تصاویر آپلودشده از دیسک محلی زیر مسیر عمومی {@code /media/**} (ADR-0013). */
@Configuration
public class NewsWebConfig implements WebMvcConfigurer {

    private final String storagePath;

    public NewsWebConfig(@Value("${sepahan.news.media.storage-path}") String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + java.nio.file.Path.of(storagePath).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
