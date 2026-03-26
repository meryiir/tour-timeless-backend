-- CONTACT_REPLY rows omit booking_id / booking_reference. Hibernate ddl-auto:update does not reliably
-- drop NOT NULL on existing PostgreSQL columns, so we fix the live schema here.

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'user_notifications'
  ) THEN
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'user_notifications'
        AND column_name = 'booking_id' AND is_nullable = 'NO'
    ) THEN
      ALTER TABLE user_notifications ALTER COLUMN booking_id DROP NOT NULL;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'user_notifications'
        AND column_name = 'booking_reference' AND is_nullable = 'NO'
    ) THEN
      ALTER TABLE user_notifications ALTER COLUMN booking_reference DROP NOT NULL;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'user_notifications'
        AND column_name = 'activity_title' AND is_nullable = 'NO'
    ) THEN
      ALTER TABLE user_notifications ALTER COLUMN activity_title DROP NOT NULL;
    END IF;

    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'user_notifications'
        AND column_name = 'status' AND is_nullable = 'NO'
    ) THEN
      ALTER TABLE user_notifications ALTER COLUMN status DROP NOT NULL;
    END IF;
  END IF;
END
$migration$;
