-- خبرهای واردشده‌ی خودکار از سایت اصلی باشگاه (sepahansc.com، ADR-0025).
-- source_id شناسه‌ی عددی خبر در سایت باشگاه است؛ یکتایی آن جلوی ورود تکراری را می‌گیرد.
ALTER TABLE news.article ADD COLUMN source_id BIGINT;
ALTER TABLE news.article ADD COLUMN source_url VARCHAR(600);

CREATE UNIQUE INDEX uq_article_source_id ON news.article (source_id) WHERE source_id IS NOT NULL;
