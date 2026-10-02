ALTER TABLE activities
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE activities
SET updated_at = COALESCE(updated_at, created_at, NOW());
