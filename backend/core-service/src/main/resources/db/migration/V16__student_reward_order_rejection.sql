ALTER TABLE reward_orders
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT;

ALTER TABLE reward_orders
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ;
