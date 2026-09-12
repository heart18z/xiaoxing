package org.springblade.modules.auth.handler;

import org.springblade.core.jwt.props.JwtProperties;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.oauth2.handler.OAuth2TokenHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.provider.OAuth2Token;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.tool.support.Kv;

/**
 * 令牌增强：补充 user_name，保留 ParamCache token_expire_time 语义
 */
public class BladeTokenHandler extends OAuth2TokenHandler {

	public BladeTokenHandler(JwtProperties properties) {
		super(properties);
	}

	@Override
	public OAuth2Token enhance(OAuth2User user, OAuth2Token token, OAuth2Request request) {
		Kv args = token.getArgs();
		args.set(TokenConstant.USER_NAME, user.getAccount());
		return super.enhance(user, token, request);
	}
}
