package org.springblade.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.OAuth2UserService;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.auth.provider.UserType;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IUserService;

import java.util.Optional;

/**
 * OAuth2 用户加载服务
 */
@RequiredArgsConstructor
public class BladeUserDetailService implements OAuth2UserService {

	private final IUserService userService;

	@Override
	public OAuth2User loadByUserId(String userId, OAuth2Request request) {
		String userType = resolveUserType(request);
		UserInfo userInfo = userService.userInfo(userId, UserType.of(userType).toUserEnum());
		return TokenUtil.convertUser(userInfo, request);
	}

	@Override
	public OAuth2User loadByUsername(String username, OAuth2Request request) {
		String userType = resolveUserType(request);
		String tenantId = request.getTenantId();
		User user = userService.userByAccount(tenantId, username);
		if (user == null) {
			return null;
		}
		UserInfo userInfo = userService.userInfo(String.valueOf(user.getId()), UserType.of(userType).toUserEnum());
		return TokenUtil.convertUser(userInfo, request);
	}

	@Override
	public OAuth2User loadByPhone(String phone, OAuth2Request request) {
		return null;
	}

	@Override
	public boolean validateUser(OAuth2User user) {
		return Optional.ofNullable(user)
			.filter(u -> u.getUserId() != null && !u.getUserId().isEmpty())
			.filter(u -> u.getAuthorities() != null && !u.getAuthorities().isEmpty())
			.isPresent();
	}

	private String resolveUserType(OAuth2Request request) {
		return Optional.ofNullable(request.getUserType())
			.filter(s -> !StringUtil.isBlank(s))
			.orElse(UserType.WEB.getName());
	}
}
