-- Pin order for public activity listings (lower = earlier). Default keeps existing rows after migration.

ALTER TABLE activities
  ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 1000;

CREATE INDEX IF NOT EXISTS idx_activities_display_order_created
  ON activities (display_order ASC, created_at DESC);
