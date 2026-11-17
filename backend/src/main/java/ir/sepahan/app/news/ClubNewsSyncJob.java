package ir.sepahan.app.news;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * همگام‌سازی اخبار سایت اصلی باشگاه. اولین اجرا بعد از هر بالا آمدن «کامل» است (همه‌ی ماه‌های
 * آرشیو؛ فقط خبرهای ناموجود دانلود می‌شوند، پس اجرای دوباره ارزان است و شکاف ناشی از قطع شدن
 * اجرای قبلی را پر می‌کند)، اجراهای بعدی فقط خبرهای تازه. جدیدترین‌ها همیشه اول ذخیره می‌شوند.
 */
@Component
@ConditionalOnProperty(name = "sepahan.news.scraper.enabled", havingValue = "true")
public class ClubNewsSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ClubNewsSyncJob.class);
    /** در اجرای افزایشی، این تعداد ماه آخر آرشیو هم بررسی می‌شود (مرز ماه). */
    private static final int INCREMENTAL_ARCHIVE_MONTHS = 2;

    private final SepahanscClient client;
    private final ClubNewsImportService importService;
    private final ArticleRepository articleRepository;
    private final long requestDelayMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean fullSyncDone = false;

    public ClubNewsSyncJob(SepahanscClient client, ClubNewsImportService importService,
                            ArticleRepository articleRepository,
                            @Value("${sepahan.news.scraper.request-delay-ms:400}") long requestDelayMs) {
        this.client = client;
        this.importService = importService;
        this.articleRepository = articleRepository;
        this.requestDelayMs = requestDelayMs;
    }

    @Scheduled(initialDelay = 15_000, fixedDelayString = "${sepahan.news.scraper.interval-ms:900000}")
    public void sync() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            boolean full = !fullSyncDone;
            int imported = run(full);
            if (full) {
                fullSyncDone = true;
            }
            log.info("همگام‌سازی اخبار سایت باشگاه ({}) تمام شد -- {} خبر جدید", full ? "کامل" : "افزایشی", imported);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("همگام‌سازی اخبار سایت باشگاه ناموفق بود -- در دور بعدی دوباره تلاش می‌شود: {}", e.toString());
        } finally {
            running.set(false);
        }
    }

    private int run(boolean full) throws Exception {
        int imported = importNew(client.recentEntries());

        List<String> archive = new ArrayList<>(client.archiveSitemapUrls());
        Collections.reverse(archive); // جدیدترین ماه اول
        if (!full) {
            archive = archive.subList(0, Math.min(INCREMENTAL_ARCHIVE_MONTHS, archive.size()));
        }
        for (String sitemapUrl : archive) {
            pause();
            List<SepahanscClient.SitemapEntry> entries = new ArrayList<>(client.entriesOf(sitemapUrl));
            entries.sort((a, b) -> Long.compare(b.sourceId(), a.sourceId()));
            imported += importNew(entries);
        }
        return imported;
    }

    private int importNew(List<SepahanscClient.SitemapEntry> entries) throws InterruptedException {
        if (entries.isEmpty()) {
            return 0;
        }
        Set<Long> existing = new HashSet<>(articleRepository.findExistingSourceIds(
                entries.stream().map(SepahanscClient.SitemapEntry::sourceId).collect(Collectors.toSet())));
        int imported = 0;
        for (SepahanscClient.SitemapEntry entry : entries) {
            if (existing.contains(entry.sourceId())) {
                continue;
            }
            try {
                pause();
                importService.importArticle(client.fetchArticle(entry));
                imported++;
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                // یک خبر خراب نباید کل همگام‌سازی را متوقف کند؛ در اجرای کامل بعدی دوباره تلاش می‌شود.
                log.warn("ورود خبر {} ناموفق بود: {}", entry.sourceId(), e.toString());
            }
        }
        return imported;
    }

    private void pause() throws InterruptedException {
        if (requestDelayMs > 0) {
            Thread.sleep(requestDelayMs);
        }
    }
}
