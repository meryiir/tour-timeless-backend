-- Seed uses a multi-number display string; column was historically VARCHAR(50).
DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'settings'
  ) THEN
    ALTER TABLE settings ALTER COLUMN contact_phone TYPE VARCHAR(255);
  END IF;
END
$migration$;
