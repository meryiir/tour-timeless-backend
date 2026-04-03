-- Convert contact messages to real threads by storing each admin/client message as an entry.

CREATE TABLE IF NOT EXISTS contact_message_entries (
  id BIGSERIAL PRIMARY KEY,
  contact_message_id BIGINT NOT NULL REFERENCES contact_messages(id) ON DELETE CASCADE,
  sender VARCHAR(20) NOT NULL, -- CLIENT / ADMIN
  body TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contact_message_entries_thread_created
  ON contact_message_entries(contact_message_id, created_at);

-- Backfill threads for existing rows (first client message + optional admin reply).
INSERT INTO contact_message_entries (contact_message_id, sender, body, created_at)
SELECT
  c.id,
  'CLIENT',
  c.message,
  COALESCE(c.created_at, NOW())
FROM contact_messages c
WHERE NOT EXISTS (
  SELECT 1 FROM contact_message_entries e WHERE e.contact_message_id = c.id
);

INSERT INTO contact_message_entries (contact_message_id, sender, body, created_at)
SELECT
  c.id,
  'ADMIN',
  c.admin_reply,
  COALESCE(c.replied_at, c.created_at, NOW())
FROM contact_messages c
WHERE c.admin_reply IS NOT NULL AND TRIM(c.admin_reply) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM contact_message_entries e
    WHERE e.contact_message_id = c.id AND e.sender = 'ADMIN'
  );

