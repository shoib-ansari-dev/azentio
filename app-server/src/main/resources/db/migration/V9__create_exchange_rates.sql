CREATE TABLE exchange_rate (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency       CHAR(3) NOT NULL UNIQUE,
    rate_to_inr    NUMERIC(18, 6) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     VARCHAR(100)
);
