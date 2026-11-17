package ir.sepahan.app.news;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * خواندن اخبار سایت اصلی باشگاه (sepahansc.com). فهرست خبرها فقط از Sitemap رسمی سایت
 * (که در robots.txt معرفی شده) گرفته می‌شود -- نه از صفحه‌ی /fa/archive که robots.txt آن را
 * Disallow کرده -- و فقط صفحه‌ی خودِ خبر خوانده می‌شود. ADR-0025.
 */
@Component
@ConditionalOnProperty(name = "sepahan.news.scraper.enabled", havingValue = "true")
public class SepahanscClient {

    private static final Pattern NEWS_URL = Pattern.compile("/fa/news/(\\d+)");
    private static final int MAX_TAGS = 6;

    /** یک ورودی Sitemap: شناسه‌ی خبر در سایت باشگاه + آدرس. */
    public record SitemapEntry(long sourceId, String url) {
    }

    public record ScrapedArticle(long sourceId, String url, String title, String summary, String content,
                                  String coverImageUrl, OffsetDateTime publishedAt, List<String> tags) {
    }

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String baseUrl;

    public SepahanscClient(@Value("${sepahan.news.scraper.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** خبرهای اخیر (Sitemap «news»، حدود ۴۰ مورد، جدیدترین اول). */
    public List<SitemapEntry> recentEntries() throws IOException, InterruptedException {
        return parseNewsUrls(fetch(baseUrl + "/fa-sitemap-news"));
    }

    /** آدرس Sitemap ماهانه‌ی آرشیو، از قدیمی‌ترین تا جدیدترین (ترتیب خودِ سایت). */
    public List<String> archiveSitemapUrls() throws IOException, InterruptedException {
        Document index = Jsoup.parse(fetch(baseUrl + "/fa-sitemap-newsarchive"), "", Parser.xmlParser());
        List<String> urls = new ArrayList<>();
        for (Element loc : index.select("sitemap > loc")) {
            urls.add(loc.text().trim());
        }
        return urls;
    }

    public List<SitemapEntry> entriesOf(String sitemapUrl) throws IOException, InterruptedException {
        return parseNewsUrls(fetch(sitemapUrl));
    }

    public ScrapedArticle fetchArticle(SitemapEntry entry) throws IOException, InterruptedException {
        Document doc = Jsoup.parse(fetch(baseUrl + "/fa/news/" + entry.sourceId()), baseUrl);

        String title = meta(doc, "og:title");
        if (title == null || title.isBlank()) {
            title = doc.title();
        }
        if (title == null || title.isBlank()) {
            throw new IOException("عنوان خبر " + entry.sourceId() + " پیدا نشد");
        }

        String content = extractBody(doc);
        String summary = clean(meta(doc, "og:description"));
        if (summary == null || summary.isBlank()) {
            summary = content.length() > 220 ? content.substring(0, 220) + "…" : content;
        }
        if (content.isBlank()) {
            // گزارش‌های تصویری/ویدیویی گاهی متن ندارند؛ content ستون الزامی است.
            content = summary.isBlank() ? title : summary;
        }

        String cover = meta(doc, "og:image");
        if (cover != null && cover.length() > 500) {
            cover = null;
        }

        OffsetDateTime publishedAt = null;
        String published = meta(doc, "article:published_time");
        if (published != null) {
            try {
                publishedAt = OffsetDateTime.parse(published);
            } catch (RuntimeException ignored) {
                // تاریخ نامعتبر -> بعداً با زمان ورود جایگزین می‌شود
            }
        }

        Set<String> tags = new LinkedHashSet<>();
        for (Element tag : doc.select("meta[property=article:tag]")) {
            String name = clean(tag.attr("content"));
            if (name != null && !name.isBlank() && name.length() <= 100 && tags.size() < MAX_TAGS) {
                tags.add(name);
            }
        }

        return new ScrapedArticle(entry.sourceId(), entry.url(), clean(title), summary, content, cover, publishedAt,
                new ArrayList<>(tags));
    }

    private static String extractBody(Document doc) {
        Element body = doc.selectFirst("div.body");
        if (body == null) {
            return "";
        }
        body.select("script, style, iframe, .wrapper, .albumGridContainer").remove();
        List<String> blocks = new ArrayList<>();
        for (Element block : body.select("p, h2, h3, h4, li")) {
            String text = clean(block.text());
            if (text != null && !text.isBlank()) {
                blocks.add(text);
            }
        }
        if (blocks.isEmpty()) {
            String text = clean(body.text());
            return text == null ? "" : text;
        }
        return String.join("\n\n", blocks);
    }

    private static List<SitemapEntry> parseNewsUrls(String xml) {
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        List<SitemapEntry> entries = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Element loc : doc.select("url > loc")) {
            String url = loc.text().trim();
            Matcher m = NEWS_URL.matcher(url);
            if (m.find() && seen.add(Long.parseLong(m.group(1)))) {
                entries.add(new SitemapEntry(Long.parseLong(m.group(1)), url));
            }
        }
        return entries;
    }

    private static String meta(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        return el == null ? null : el.attr("content");
    }

    private static String clean(String text) {
        return text == null ? null : text.replace('\u00a0', ' ').replaceAll("[ \\t]+", " ").trim();
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "SEPahanApp-NewsSync/1.0 (+https://www.sepahansc.com)")
                .header("Accept-Language", "fa")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " برای " + url);
        }
        return response.body();
    }
}
