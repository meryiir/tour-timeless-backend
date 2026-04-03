-- After DB migration / restore, image URLs may still point at an old host
-- (e.g. http://old-server:8081/uploads/...). Rewrite to path-only /uploads/...
-- so the current backend serves them from app.upload.dir.
-- Also prefix bare filenames (uuid.ext) when the export dropped the path.

UPDATE destinations
SET image_url = substring(image_url FROM '/uploads/.*')
WHERE image_url IS NOT NULL
  AND image_url ~ '^https?://[^[:space:]]+/uploads/';

UPDATE activities
SET image_url = substring(image_url FROM '/uploads/.*')
WHERE image_url IS NOT NULL
  AND image_url ~ '^https?://[^[:space:]]+/uploads/';

UPDATE activity_gallery_images
SET image_url = substring(image_url FROM '/uploads/.*')
WHERE image_url IS NOT NULL
  AND image_url ~ '^https?://[^[:space:]]+/uploads/';

UPDATE destination_page_cards
SET image_url = substring(image_url FROM '/uploads/.*')
WHERE image_url IS NOT NULL
  AND image_url ~ '^https?://[^[:space:]]+/uploads/';

-- Bare storage filename only (no scheme, no slash — e.g. UUID from a bad export)
UPDATE destinations
SET image_url = '/uploads/' || image_url
WHERE image_url IS NOT NULL
  AND image_url !~ '^https?://'
  AND image_url !~ '^/'
  AND image_url !~ '/'
  AND image_url ~ '^[a-zA-Z0-9_.-]+\.(jpg|jpeg|png|gif|webp)$';

UPDATE activities
SET image_url = '/uploads/' || image_url
WHERE image_url IS NOT NULL
  AND image_url !~ '^https?://'
  AND image_url !~ '^/'
  AND image_url !~ '/'
  AND image_url ~ '^[a-zA-Z0-9_.-]+\.(jpg|jpeg|png|gif|webp)$';

UPDATE activity_gallery_images
SET image_url = '/uploads/' || image_url
WHERE image_url IS NOT NULL
  AND image_url !~ '^https?://'
  AND image_url !~ '^/'
  AND image_url !~ '/'
  AND image_url ~ '^[a-zA-Z0-9_.-]+\.(jpg|jpeg|png|gif|webp)$';

UPDATE destination_page_cards
SET image_url = '/uploads/' || image_url
WHERE image_url IS NOT NULL
  AND image_url !~ '^https?://'
  AND image_url !~ '^/'
  AND image_url !~ '/'
  AND image_url ~ '^[a-zA-Z0-9_.-]+\.(jpg|jpeg|png|gif|webp)$';
