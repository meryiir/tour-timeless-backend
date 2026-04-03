-- Public About/Contact CMS: map embed, phone list JSON, hours, optional about JSON (EN + per-language overrides).

ALTER TABLE settings ADD COLUMN IF NOT EXISTS map_embed_url TEXT;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS contact_phones_json TEXT;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS business_hours TEXT;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS about_content_json TEXT;

ALTER TABLE settings_translations ADD COLUMN IF NOT EXISTS business_hours TEXT;
ALTER TABLE settings_translations ADD COLUMN IF NOT EXISTS about_content_json TEXT;
