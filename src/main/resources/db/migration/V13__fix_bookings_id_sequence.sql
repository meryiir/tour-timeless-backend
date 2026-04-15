-- After DB imports/restores, SERIAL/IDENTITY sequences can fall behind MAX(id),
-- causing "duplicate key value violates unique constraint ..._pkey" on insert.
-- This migration realigns the identity/sequence for bookings.id.

DO $migration$
DECLARE
  max_id BIGINT;
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_name = 'bookings'
  ) THEN
    EXECUTE 'SELECT COALESCE(MAX(id), 0) FROM bookings' INTO max_id;
    -- setval(sequence, value, is_called=true) so nextval() returns value+1.
    PERFORM setval(pg_get_serial_sequence('bookings', 'id'), max_id, true);
  END IF;
END
$migration$;

