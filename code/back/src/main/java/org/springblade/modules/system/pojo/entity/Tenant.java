package org.springblade.modules.system.pojo.entity;

import org.apache.ibatis.type.Alias;

/**
 * BladeX 4.9 兼容类：Redis 缓存反序列化会引用此包路径，业务实体仍在 {@link org.springblade.modules.system.entity.Tenant}。
 * 使用独立别名，避免与 {@code org.springblade.**.entity} 扫描下的 Tenant 冲突。
 */
@Alias("PojoTenant")
public class Tenant extends org.springblade.modules.system.entity.Tenant {

}
