-- Replace all contact phone lines with US + Morocco numbers.

DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'settings'
  ) THEN
    UPDATE settings
    SET
      contact_phone = '+16086504232 | +212721104528',
      contact_phones_json = '[{"display":"+16086504232","tel":"+16086504232"},{"display":"+212721104528","tel":"+212721104528"}]'
    WHERE id IS NOT NULL;
  END IF;
END
$migration$;
