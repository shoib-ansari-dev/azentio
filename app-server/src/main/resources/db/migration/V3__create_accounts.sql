CREATE TABLE account (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_ref               VARCHAR(20) NOT NULL UNIQUE,
    customer_id               UUID NOT NULL REFERENCES customer (id),
    account_type              VARCHAR(20),
    account_status            VARCHAR(20),
    currency                  CHAR(3),
    open_date                 DATE,
    close_date                DATE,
    branch_code               VARCHAR(20),
    branch_city               VARCHAR(100),
    current_balance           NUMERIC(18, 2),
    avg_monthly_balance_6m    NUMERIC(18, 2),
    credit_limit              NUMERIC(18, 2),
    credit_utilization_pct    NUMERIC(5, 2),
    overdraft_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    card_type                 VARCHAR(20),
    is_joint_account          BOOLEAN NOT NULL DEFAULT FALSE,
    num_linked_devices        INTEGER,
    mobile_banking_enrolled   BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_date           DATE,
    avg_monthly_txn_count     INTEGER,
    account_tier              VARCHAR(20),
    risk_rating               VARCHAR(10),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_customer ON account (customer_id);
