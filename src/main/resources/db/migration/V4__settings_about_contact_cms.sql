-- Public About/Contact CMS columns. On a fresh DB, Flyway runs before Hibernate,
-- so settings tables may not exist — skip; Hibernate + ddl-auto will create the schema.

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'settings'
  ) THEN
    ALTER TABLE settings ADD COLUMN IF NOT EXISTS map_embed_url TEXT;
    ALTER TABLE settings ADD COLUMN IF NOT EXISTS contact_phones_json TEXT;
    ALTER TABLE settings ADD COLUMN IF NOT EXISTS business_hours TEXT;
    ALTER TABLE settings ADD COLUMN IF NOT EXISTS about_content_json TEXT;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'settings_translations'
  ) THEN
    ALTER TABLE settings_translations ADD COLUMN IF NOT EXISTS business_hours TEXT;
    ALTER TABLE settings_translations ADD COLUMN IF NOT EXISTS about_content_json TEXT;
  END IF;
END
$migration$;
