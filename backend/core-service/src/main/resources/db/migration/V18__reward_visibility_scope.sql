ALTER TABLE reward_items
    ADD COLUMN IF NOT EXISTS visibility_scope VARCHAR(32) NOT NULL DEFAULT 'UNASSIGNED';

CREATE TABLE IF NOT EXISTS reward_item_target_clubs (
    id BIGSERIAL PRIMARY KEY,
    reward_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reward_item_target_clubs_reward_club
    ON reward_item_target_clubs(reward_id, club_id);

CREATE INDEX IF NOT EXISTS idx_reward_item_target_clubs_reward_id
    ON reward_item_target_clubs(reward_id);

CREATE INDEX IF NOT EXISTS idx_reward_item_target_clubs_club_id
    ON reward_item_target_clubs(club_id);

UPDATE reward_items
SET visibility_scope = 'UNASSIGNED'
WHERE visibility_scope IS NULL
   OR visibility_scope <> 'UNASSIGNED';
