CREATE TABLE IF NOT EXISTS manager_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(120),
    manager_no VARCHAR(64),
    phone VARCHAR(32),
    bio TEXT,
    avatar_path VARCHAR(255),
    avatar_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_manager_info_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_manager_info_user_id ON manager_info(user_id);
