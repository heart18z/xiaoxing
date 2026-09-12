package org.springblade.modules.auth.utils;

import lombok.RequiredArgsConstructor;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.oauth2.handler.TokenHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.provider.OAuth2Token;
import org.springblade.core.oauth2.service.OAuth2Client;
import org.springblade.core.oauth2.service.OAuth2ClientService;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.utils.OAuth2Util;
import org.springblade.core.secure.TokenInfo;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springframework.stereotype.Component;

/**
 * 非标准登录场景下签发 OAuth2 令牌
 */
@Component
@RequiredArgsConstructor
public class OAuth2TokenIssueHelper {

	private final TokenHandler tokenHandler;
	private final OAuth2ClientService clientService;

	public Kv issueToken(OAuth2User user, OAuth2Request request) {
		OAuth2Client client = clientService.loadByClientId(request.getClientId(), request);
		user.setClient(client);
		TokenInfo accessToken = OAuth2Util.createAccessToken(user);
		TokenInfo refreshToken = OAuth2Util.createRefreshToken(user);
		OAuth2Token token = OAuth2Token.create();
		Kv args = token.getArgs();
		args.set(TokenConstant.TENANT_ID, user.getTenantId())
			.set(TokenConstant.USER_ID, user.getUserId())
			.set(TokenConstant.DEPT_ID, user.getDeptId())
			.set(TokenConstant.POST_ID, user.getPostId())
			.set(TokenConstant.ROLE_ID, user.getRoleId())
			.set(TokenConstant.OAUTH_ID, user.getOauthId())
			.set(TokenConstant.ACCOUNT, user.getAccount())
			.set(TokenConstant.USER_NAME, user.getAccount())
			.set(TokenConstant.NICK_NAME, user.getName())
			.set(TokenConstant.REAL_NAME, user.getRealName())
			.set(TokenConstant.ROLE_NAME, Func.join(user.getAuthorities()))
			.set(TokenConstant.AVATAR, Func.toStr(user.getAvatar(), TokenConstant.DEFAULT_AVATAR))
			.set(TokenConstant.ACCESS_TOKEN, accessToken.getToken())
			.set(TokenConstant.REFRESH_TOKEN, refreshToken.getToken())
			.set(TokenConstant.TOKEN_TYPE, TokenConstant.BEARER)
			.set(TokenConstant.EXPIRES_IN, accessToken.getExpire())
			.set(TokenConstant.DETAIL, user.getDetail())
			.set(TokenConstant.LICENSE, TokenConstant.LICENSE_NAME);
		token.setAccessToken(accessToken.getToken())
			.setAccessTokenExpire(accessToken.getExpire())
			.setRefreshToken(refreshToken.getToken())
			.setRefreshTokenExpire(refreshToken.getExpire());
		OAuth2Token enhanced = tokenHandler.enhance(user, token, request);
		return enhanced.getArgs();
	}
}
