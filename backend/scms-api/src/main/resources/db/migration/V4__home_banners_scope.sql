ALTER TABLE home_banners
    ADD COLUMN IF NOT EXISTS scope VARCHAR(16) NOT NULL DEFAULT 'HOME';

UPDATE home_banners
SET scope = 'HOME'
WHERE scope IS NULL
   OR scope = '';

CREATE INDEX IF NOT EXISTS idx_home_banners_scope_sort_order_id
    ON home_banners(scope, sort_order, id);
