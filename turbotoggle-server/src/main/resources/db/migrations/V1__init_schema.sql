-- 1. Environment table
CREATE TABLE IF NOT EXISTS environments (
    id BIGSERIAL PRIMARY KEY,
    env_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sdk_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_environments_sdk_key ON environments(sdk_key);

-- 2. Feature Flags meta table
CREATE TABLE IF NOT EXISTS feature_flags(
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    flag_type VARCHAR(32) NOT NULL DEFAULT 'BOOLEAN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feature_flags_flag_key ON feature_flags(flag_key);

-- 3. Flag Environment Config & Targeting Rules

CREATE TABLE IF NOT EXISTS flag_environment_configs(
    id BIGSERIAL PRIMARY KEY,
    flag_id BIGINT NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    environment_id BIGINT NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_variation TEXT NOT NULL,
    rules JSONB DEFAULT '[]'::jsonb NOT NULL,
    individual_targets JSONB DEFAULT '[]'::jsonb NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_flag_env UNIQUE (flag_id, environment_id)
);

CREATE INDEX IF NOT EXISTS idx_flag_env_rules ON flag_environment_configs USING gin (rules);
CREATE INDEX IF NOT EXISTS idx_env_configs_flag_env ON environment_configs(flag_id, environment_id);

-- 4. Audit Log Table
CREATE TABLE IF NOT EXISTS audit_logs(
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(128) NOT NULL,
    env_key VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    delta_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_flag_env on audit_logs(flag_key, env_key);