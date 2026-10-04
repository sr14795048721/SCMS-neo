CREATE TABLE IF NOT EXISTS home_banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    media_type VARCHAR(16) NOT NULL,
    media_path VARCHAR(255),
    media_content_type VARCHAR(128),
    media_size_bytes BIGINT,
    poster_path VARCHAR(255),
    poster_content_type VARCHAR(128),
    sort_order INTEGER NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_home_banners_sort_order ON home_banners(sort_order);
