-- طبق docs/database/erd-news.md و ADR-0013 (News/CMS، Phase 11)

CREATE TABLE news.category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID
);

CREATE UNIQUE INDEX uq_news_category_slug ON news.category (slug) WHERE deleted_at IS NULL;

CREATE TABLE news.tag (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID
);

CREATE UNIQUE INDEX uq_news_tag_slug ON news.tag (slug) WHERE deleted_at IS NULL;

-- author_id فقط یک UUID عمومی است (-> users.app_user.id)، بدون FK فیزیکی Cross-schema
-- (طبق docs/database/conventions.md) -- همان الگوی user_id در ticketing/shop/payments.
CREATE TABLE news.article (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id         UUID NOT NULL REFERENCES news.category (id),
    author_id           UUID NOT NULL,
    title               VARCHAR(250) NOT NULL,
    slug                VARCHAR(250) NOT NULL,
    summary             TEXT,
    content             TEXT NOT NULL,
    cover_image_url     VARCHAR(500),
    meta_title          VARCHAR(250),
    meta_description    VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'draft',
    published_at        TIMESTAMPTZ,
    scheduled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    created_by          UUID,
    updated_by          UUID,

    CONSTRAINT chk_article_status CHECK (status IN ('draft', 'scheduled', 'published', 'archived'))
);

CREATE UNIQUE INDEX uq_article_slug ON news.article (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_article_status_published_at ON news.article (status, published_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_article_scheduled_at ON news.article (scheduled_at) WHERE status = 'scheduled';
CREATE INDEX idx_article_category ON news.article (category_id) WHERE deleted_at IS NULL;

CREATE TABLE news.article_tag (
    article_id  UUID NOT NULL REFERENCES news.article (id),
    tag_id      UUID NOT NULL REFERENCES news.tag (id),
    PRIMARY KEY (article_id, tag_id)
);

-- Append-only: هر ویرایش، حالت *قبلی* خبر را این‌جا Snapshot می‌کند، پیش از اعمال
-- تغییر روی خود article -- بدون ستون‌های پایه‌ی Soft-delete/Audit (مثل ticketing.reservation).
CREATE TABLE news.article_revision (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id  UUID NOT NULL REFERENCES news.article (id),
    title       VARCHAR(250) NOT NULL,
    summary     TEXT,
    content     TEXT NOT NULL,
    edited_by   UUID NOT NULL,
    edited_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_article_revision_article ON news.article_revision (article_id, edited_at DESC);

-- ذخیره‌سازی روی دیسک محلی (تصمیم صریح کارفرما در همین فاز، ADR-0013) -- storage_path
-- مسیر فیزیکی روی دیسک است (هرگز مستقیم به کاربر نشان داده نمی‌شود)، url مسیر عمومی Serve.
CREATE TABLE news.media_asset (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name     VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    url           VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID
);
