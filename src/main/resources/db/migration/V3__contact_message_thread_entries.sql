-- Thread entries for contact messages. On databases where Hibernate already created
-- contact_messages (typical dev), we add entries + backfill. On a fresh DB, Flyway runs
-- before Hibernate, so contact_messages does not exist yet — skip here; Hibernate will
-- create both contact_messages and contact_message_entries from entities.

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'contact_messages'
  ) THEN
    CREATE TABLE IF NOT EXISTS contact_message_entries (
      id BIGSERIAL PRIMARY KEY,
      contact_message_id BIGINT NOT NULL REFERENCES contact_messages(id) ON DELETE CASCADE,
      sender VARCHAR(20) NOT NULL,
      body TEXT NOT NULL,
      created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_contact_message_entries_thread_created
      ON contact_message_entries(contact_message_id, created_at);

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
  END IF;
END
$migration$;
