CREATE TABLE IF NOT EXISTS home_club_recommendations (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    slot_no SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_home_club_recommendations_club UNIQUE (club_id),
    CONSTRAINT uk_home_club_recommendations_slot UNIQUE (slot_no),
    CONSTRAINT chk_home_club_recommendations_slot CHECK (slot_no BETWEEN 1 AND 4)
);

CREATE INDEX IF NOT EXISTS idx_home_club_recommendations_slot ON home_club_recommendations(slot_no);
CREATE INDEX IF NOT EXISTS idx_home_club_recommendations_club ON home_club_recommendations(club_id);
