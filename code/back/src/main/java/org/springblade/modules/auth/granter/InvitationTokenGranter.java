package org.springblade.modules.auth.granter;

import org.springblade.common.cache.CacheNames;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oauth2.granter.AbstractTokenGranter;
import org.springblade.core.oauth2.handler.PasswordHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2ClientService;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.OAuth2UserService;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.auth.enums.UserEnum;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Component;

/**
 * 邀请码登录
 */
@Component
public class InvitationTokenGranter extends AbstractTokenGranter {

	public static final String GRANT_TYPE = "invitation";

	private final IUserService userService;
	private final BladeRedis bladeRedis;

	public InvitationTokenGranter(OAuth2ClientService clientService, OAuth2UserService oAuth2UserService,
								  PasswordHandler passwordHandler, IUserService userService, BladeRedis bladeRedis) {
		super(clientService, oAuth2UserService, passwordHandler);
		this.userService = userService;
		this.bladeRedis = bladeRedis;
	}

	@Override
	public String type() {
		return GRANT_TYPE;
	}

	@Override
	public OAuth2User user(OAuth2Request request) {
		String invitationCode = request.getUsername();
		String userId = bladeRedis.get(CacheNames.cacheKey(CacheNames.INVITATION_KEY, invitationCode));
		if (Func.isBlank(userId)) {
			throw new ServiceException("邀请码错误！");
		}
		UserInfo userInfo = userService.userInfo(userId, UserEnum.WEB);
		if (userInfo == null || userInfo.getUser() == null) {
			throw new ServiceException("邀请码错误！");
		}
		OAuth2User user = TokenUtil.convertUser(userInfo, request);
		user.setClient(client(request));
		return user;
	}
}
