CREATE TABLE transaction (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_ref           VARCHAR(30) NOT NULL UNIQUE,
    account_id                UUID NOT NULL REFERENCES account (id),
    amount                    NUMERIC(18, 2) NOT NULL,
    currency                  CHAR(3) NOT NULL,
    amount_inr                NUMERIC(18, 2),
    exchange_rate_used        NUMERIC(18, 6),
    transaction_type          VARCHAR(20),
    channel                   VARCHAR(30),
    counterparty_account      VARCHAR(50),
    counterparty_bank         VARCHAR(100),
    counterparty_jurisdiction CHAR(2),
    description               VARCHAR(500),
    transaction_timestamp     TIMESTAMPTZ NOT NULL,
    ingested_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    job_id                    UUID
);

CREATE INDEX idx_transaction_account_time ON transaction (account_id, transaction_timestamp);
