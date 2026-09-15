-- Preflight only: investigate duplicates manually; never delete or overwrite users.
-- Uniqueness is per tenant among non-deleted users, including disabled/admin users.
SELECT tenant_id, TRIM(phone) AS phone, COUNT(*) AS duplicates
FROM blade_user WHERE is_deleted=0 AND NULLIF(TRIM(phone),'') IS NOT NULL
GROUP BY tenant_id, TRIM(phone) HAVING COUNT(*)>1;
SELECT tenant_id, LOWER(TRIM(email)) AS email, COUNT(*) AS duplicates
FROM blade_user WHERE is_deleted=0 AND NULLIF(TRIM(email),'') IS NOT NULL
GROUP BY tenant_id, LOWER(TRIM(email)) HAVING COUNT(*)>1;

-- Run once, only after both preflights return no rows. MySQL 8 atomic ALTER:
-- a collision fails the entire ALTER; existing users and contacts are not changed.
-- Multiple blank contacts remain allowed. Older users need not be backfilled.
ALTER TABLE blade_user
  ADD COLUMN app_unique_phone VARCHAR(45) GENERATED ALWAYS AS
    (CASE WHEN is_deleted=0 THEN NULLIF(TRIM(phone),'') ELSE NULL END) STORED,
  ADD COLUMN app_unique_email VARCHAR(45) GENERATED ALWAYS AS
    (CASE WHEN is_deleted=0 THEN NULLIF(LOWER(TRIM(email)),'') ELSE NULL END) STORED,
  ADD UNIQUE INDEX uk_app_phone_tenant (tenant_id,app_unique_phone),
  ADD UNIQUE INDEX uk_app_email_tenant (tenant_id,app_unique_email);
