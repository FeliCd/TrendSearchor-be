-- V22: Freemium subscription model + AI usage tracking.
--
-- Background: V11 first added subscription/payment tables; V12 dropped them.
-- This migration restores them (extended with quota columns) and adds an
-- ai_usage_log table used to enforce a rolling 24h AI prompt quota per user.
--
-- Tiers (seeded below):
--   FREE  — 3 AI prompts / 24h  (default for every user, no purchase)
--   PRO   — 50 AI prompts / 24h, 199,000 VND / 30 days

CREATE TABLE subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,          -- FREE / PRO
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    duration_days INT NOT NULL,                -- 0 for FREE, 30 for PRO
    daily_prompt_limit INT NOT NULL,           -- rolling-24h AI prompt cap
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,               -- PENDING / ACTIVE / EXPIRED / CANCELLED
    start_date DATETIME(6) NULL,
    end_date DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    CONSTRAINT fk_usub_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_usub_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_usub_user ON user_subscriptions (user_id);
CREATE INDEX idx_usub_status ON user_subscriptions (status);

CREATE TABLE payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,               -- PENDING / SUCCESS / FAILED
    created_at DATETIME(6) NULL,
    CONSTRAINT fk_ptx_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ptx_sub FOREIGN KEY (subscription_id) REFERENCES user_subscriptions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ptx_user ON payment_transactions (user_id);
CREATE INDEX idx_ptx_status ON payment_transactions (status);

CREATE TABLE ai_usage_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    feature VARCHAR(50) NOT NULL,              -- SEARCH / TREND_QA / SUMMARIZE / RERANK / ABSTRACT / RECOMMENDATIONS
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rolling-window quota query filters by (user_id, created_at)
CREATE INDEX idx_ai_usage_user_time ON ai_usage_log (user_id, created_at);

-- ─── Seed default tiers ─────────────────────────────────────────────────────
INSERT INTO subscription_plans (code, name, description, price, duration_days, daily_prompt_limit, active, created_at)
VALUES
 ('FREE', 'Free', 'Basic access with a daily AI trial quota.', 0.00, 0, 3, TRUE, NOW(6)),
 ('PRO',  'Pro Researcher', 'Unlock full AI features with a higher daily quota.', 199000.00, 30, 50, TRUE, NOW(6));
