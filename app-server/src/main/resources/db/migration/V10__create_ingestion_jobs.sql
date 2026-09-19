CREATE TABLE ingestion_job (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type       VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    total_records  INTEGER NOT NULL DEFAULT 0,
    processed      INTEGER NOT NULL DEFAULT 0,
    failed         INTEGER NOT NULL DEFAULT 0,
    error_summary  TEXT,
    submitted_by   VARCHAR(100),
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ
);
