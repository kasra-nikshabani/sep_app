package ir.sepahan.app.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * تست واحد ساده (بدون Spring Context) -- MediaService فقط به یک Repository و چند
 * مقدار پیکربندی نیاز دارد، پس نیازی به بالا آوردن کل برنامه/Postgres نیست.
 */
class MediaServiceTest {

    @TempDir
    Path tempDir;

    private MediaService newService() {
        MediaAssetRepository repository = mock(MediaAssetRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new MediaService(repository, tempDir.toString(), "http://localhost:8081/media",
                List.of("image/jpeg", "image/png"));
    }

    @Test
    void store_rejectsDisallowedContentType() {
        MediaService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void store_savesFileToDiskWithGeneratedName_andReturnsPublicUrl() throws IOException {
        MediaService service = newService();
        byte[] content = "fake-image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.png", "image/png", content);

        MediaAsset asset = service.store(file);

        assertThat(asset.getUrl()).startsWith("http://localhost:8081/media/");
        assertThat(asset.getFileName()).isEqualTo("../../etc/passwd.png"); // فقط برای نمایش، هرگز برای مسیر ذخیره استفاده نمی‌شود
        assertThat(asset.getContentType()).isEqualTo("image/png");
        assertThat(Path.of(asset.getStoragePath())).startsWith(tempDir);
        assertThat(Files.readAllBytes(Path.of(asset.getStoragePath()))).isEqualTo(content);
    }
}
