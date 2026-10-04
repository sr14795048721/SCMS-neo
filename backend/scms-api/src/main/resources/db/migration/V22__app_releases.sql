CREATE TABLE IF NOT EXISTS app_releases (
    id BIGSERIAL PRIMARY KEY,
    version_name VARCHAR(32) NOT NULL,
    build_number INTEGER NOT NULL,
    release_notes TEXT NOT NULL DEFAULT '',
    force_update BOOLEAN NOT NULL DEFAULT FALSE,
    android_url VARCHAR(500),
    harmony_url VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_releases_version_build
    ON app_releases(version_name, build_number);

CREATE INDEX IF NOT EXISTS idx_app_releases_status_published_at
    ON app_releases(status, published_at DESC, id DESC);
