-- Public signature-based attendance:
-- sessions get a share token for the public sign-in page,
-- records keep the handwritten signature image and who signed.

ALTER TABLE attendance_sessions
    ADD COLUMN IF NOT EXISTS share_token VARCHAR(64);

UPDATE attendance_sessions
SET share_token = md5(random()::text || clock_timestamp()::text || id::text)
WHERE share_token IS NULL;

ALTER TABLE attendance_sessions
    ALTER COLUMN share_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_sessions_share_token
    ON attendance_sessions(share_token);

ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS signed_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS signed_role VARCHAR(32),
    ADD COLUMN IF NOT EXISTS signature_path VARCHAR(255);
