-- Google Maps embed URLs for multi-stop routes are often 1500+ characters; VARCHAR(500) truncated them.
ALTER TABLE activities ALTER COLUMN map_url TYPE TEXT;
