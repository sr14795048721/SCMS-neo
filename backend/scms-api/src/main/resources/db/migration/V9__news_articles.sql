CREATE TABLE IF NOT EXISTS news_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL DEFAULT '',
    markdown_content TEXT NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL,
    cover_path VARCHAR(255),
    cover_content_type VARCHAR(128),
    cover_size_bytes BIGINT,
    cover_updated_at TIMESTAMPTZ,
    author_id BIGINT NOT NULL REFERENCES users(id),
    published_at TIMESTAMPTZ,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_news_articles_status_published ON news_articles(status, published_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_news_articles_updated ON news_articles(updated_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS news_assets (
    id BIGSERIAL PRIMARY KEY,
    news_id BIGINT NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    asset_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_news_assets_news_id ON news_assets(news_id, id);
