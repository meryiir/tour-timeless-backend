-- Long map URLs; skip if activities does not exist yet (Flyway before Hibernate on fresh DB).

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'activities'
  ) THEN
    ALTER TABLE activities ALTER COLUMN map_url TYPE TEXT;
  END IF;
END
$migration$;
