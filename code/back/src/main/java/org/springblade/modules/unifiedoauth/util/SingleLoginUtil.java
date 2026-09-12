package org.springblade.modules.unifiedoauth.util;

import org.springblade.core.jwt.JwtUtil;
import org.springblade.core.jwt.enums.SingleLevel;
import org.springblade.core.jwt.props.JwtProperties;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.enums.LoginCheckResult;

public class SingleLoginUtil {

	private static JwtProperties jwtProperties;

	private static IUserService userService;

	static {
		jwtProperties = SpringUtil.getBean(JwtProperties.class);
		userService = SpringUtil.getBean(IUserService.class);
	}

	public static LoginCheckResult checkLogin(String tenantId, String userId, Boolean confirm) {
		return checkLogin(tenantId, null, userId, confirm);
	}

	public static LoginCheckResult checkLogin(String tenantId, String clientId, String userId, Boolean confirm) {
		if (Func.notNull(confirm) && confirm) {
			return LoginCheckResult.OK;
		}
		if (jwtProperties.getState() && jwtProperties.getSingle()) {
			String effectiveTenantId = Func.isNotBlank(tenantId) ? tenantId : TokenUtil.DEFAULT_TENANT_ID;
			String accessToken = getStoredAccessToken(effectiveTenantId, clientId, userId);
			if (Func.isNotBlank(accessToken) && !"null".equals(accessToken)) {
				User user = userService.getById(Func.toLong(userId));
				if (Func.notNull(user)) {
					return LoginCheckResult.NEED_CONFIRM;
				}
			}
		}
		return LoginCheckResult.OK;
	}

	/**
	 * 按用户维度从 Redis 获取已存储的 accessToken（登录前互踢检查用）。
	 * 4.9 需使用四参 API，第 4 参传 null 表示按用户索引查询，而非校验指定 token。
	 */
	private static String getStoredAccessToken(String tenantId, String clientId, String userId) {
		String effectiveClientId = null;
		if (Boolean.TRUE.equals(jwtProperties.getSingle())
			&& SingleLevel.CLIENT == jwtProperties.getSingleLevel()
			&& Func.isNotBlank(clientId)) {
			effectiveClientId = clientId;
		}
		String key = JwtUtil.getAccessTokenKey(tenantId, effectiveClientId, userId, null);
		Object value = JwtUtil.getRedisTemplate().opsForValue().get(key);
		return Func.toStr(value);
	}
}
