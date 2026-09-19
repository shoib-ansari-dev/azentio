CREATE TABLE alert (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_ref           VARCHAR(30) NOT NULL UNIQUE,
    account_id          UUID NOT NULL REFERENCES account (id),
    customer_id         UUID NOT NULL REFERENCES customer (id),
    rule_code           VARCHAR(50) NOT NULL REFERENCES detection_rule (rule_code),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    risk_score          INTEGER NOT NULL,
    explanation         TEXT,
    evidence_txn_ids    UUID[],
    disposition_reason  TEXT,
    assigned_to         VARCHAR(100),
    case_id             UUID REFERENCES aml_case (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_alert_open_dedup ON alert (account_id, rule_code) WHERE status = 'OPEN';
