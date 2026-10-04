CREATE TABLE IF NOT EXISTS score_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    score_delta INTEGER NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    club_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_score_rules_status_created_at
    ON score_rules(status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_rules_club_id
    ON score_rules(club_id);

CREATE TABLE IF NOT EXISTS score_records (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rule_id BIGINT,
    score_delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_score_records_club_created_at
    ON score_records(club_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_records_user_created_at
    ON score_records(user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_records_rule_id
    ON score_records(rule_id);

CREATE TABLE IF NOT EXISTS reward_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    score_cost INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    image_path VARCHAR(255),
    image_content_type VARCHAR(120),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reward_items_status_created_at
    ON reward_items(status, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS reward_orders (
    id BIGSERIAL PRIMARY KEY,
    reward_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score_cost INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    completed_by BIGINT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reward_orders_reward_id
    ON reward_orders(reward_id);

CREATE INDEX IF NOT EXISTS idx_reward_orders_user_status
    ON reward_orders(user_id, status);

CREATE INDEX IF NOT EXISTS idx_reward_orders_created_at
    ON reward_orders(created_at DESC, id DESC);
