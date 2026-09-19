CREATE TABLE detection_rule (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code     VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    risk_weight   INTEGER NOT NULL,
    parameters    JSONB NOT NULL,
    version       INTEGER NOT NULL DEFAULT 1,
    updated_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
