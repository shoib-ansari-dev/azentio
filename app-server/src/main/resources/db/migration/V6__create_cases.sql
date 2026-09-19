CREATE TABLE aml_case (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_ref            VARCHAR(30) NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    assigned_to         VARCHAR(100),
    notes               TEXT,
    disposition         VARCHAR(50),
    disposition_reason  TEXT,
    created_by          VARCHAR(100),
    closed_by           VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ
);
