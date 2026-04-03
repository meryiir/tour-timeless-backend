-- Newsletter sign-ups from the public home page (additive; does not modify existing tables).

CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_newsletter_subscribers_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_newsletter_subscribers_created_at ON newsletter_subscribers (created_at);
