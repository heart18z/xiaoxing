package org.springblade.modules.auth.service;

import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2Client;
import org.springblade.core.oauth2.service.impl.OAuth2ClientDetail;
import org.springblade.core.oauth2.service.impl.OAuth2ClientDetailService;
import org.springblade.core.tool.utils.Func;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OAuth2 客户端加载，支持 ParamCache token_expire_time 覆盖 access_token 有效期
 */
public class BladeClientDetailService extends OAuth2ClientDetailService {

	public BladeClientDetailService(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	@Override
	public OAuth2Client loadByClientId(String clientId) {
		return applyTokenExpire(super.loadByClientId(clientId));
	}

	@Override
	public OAuth2Client loadByClientId(String clientId, OAuth2Request request) {
		return applyTokenExpire(super.loadByClientId(clientId, request));
	}

	private OAuth2Client applyTokenExpire(OAuth2Client client) {
		if (client instanceof OAuth2ClientDetail detail) {
			Integer expireSeconds = getTokenExpireSeconds();
			if (expireSeconds != null) {
				detail.setAccessTokenValidity(expireSeconds);
			}
		}
		return client;
	}

	private Integer getTokenExpireSeconds() {
		String time = ParamCache.getValue(ParamCacheConstant.TOKEN_EXPIRE_TIME);
		if (Func.isBlank(time)) {
			return null;
		}
		if ("-1".equals(time)) {
			return 315360000;
		}
		return Math.toIntExact(Long.parseLong(time) * 60);
	}
}
