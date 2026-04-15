-- Add admin-only soft hide flag for bookings.
-- Required by Booking.hidden + BookingRepository.findByHiddenFalse(...)

ALTER TABLE bookings
  ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Helps queries like: WHERE hidden = false ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_bookings_hidden_created_at
  ON bookings (hidden, created_at DESC);

