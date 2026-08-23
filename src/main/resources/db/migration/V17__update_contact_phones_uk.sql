-- Replace US contact line with UK number (+447445473022).

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'settings'
  ) THEN
    UPDATE settings
    SET
      contact_phone = '+447445473022 | +212721104528',
      contact_phones_json = '[{"display":"+447445473022","tel":"+447445473022"},{"display":"+212721104528","tel":"+212721104528"}]'
    WHERE id IS NOT NULL;
  END IF;
END
$migration$;
