-- Auth: application users for JWT + RBAC
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed one user per role (username = role, password = role name, bcrypt-hashed)
INSERT INTO app_user (username, password_hash, role) VALUES
    ('admin',      '$2a$10$cDzBEIcY57JwUoDDNJpM.OzcR3m7HqLlxzcT0SjaBMTGfNe6xcf.y', 'ADMIN'),
    ('supervisor', '$2a$10$fXKZ.1O0XO0UyvHDMwd7LOzTZmBVHJhMuzFTsUyZB0YPPuO7g02EW', 'SUPERVISOR'),
    ('analyst',    '$2a$10$Cj31PuWNe1PToJU1Fax9q.o1T3atMDYzb.1KNeCb7v3REqZ966fM6', 'ANALYST');
