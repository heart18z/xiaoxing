-- Run this read-only preflight first. Resolve duplicates manually; never delete users automatically.
SELECT tenant_id, account, COUNT(*) AS duplicates
FROM blade_user WHERE account IS NOT NULL
GROUP BY tenant_id, account HAVING COUNT(*) > 1;

-- Run once only after preflight returns no rows. Includes deleted accounts to prevent identity reuse.
-- If this ALTER fails, registration remains disabled; do not remove the safety check in the service.
ALTER TABLE blade_user ADD UNIQUE INDEX uk_app_account_tenant (tenant_id, account);
