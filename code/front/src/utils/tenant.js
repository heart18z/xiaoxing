import website from '@/config/website';

/** 默认租户编号，关闭多租户 UI 时统一使用 */
export const DEFAULT_TENANT_ID = website.tenantId || '000000';

export function resolveTenantId(tenantId) {
  if (tenantId === null || tenantId === undefined || tenantId === '') {
    return DEFAULT_TENANT_ID;
  }
  return tenantId;
}
