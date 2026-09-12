package org.springblade.modules.auth.keycloak;

import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oauth2.granter.AbstractTokenGranter;
import org.springblade.core.oauth2.handler.PasswordHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2Client;
import org.springblade.core.oauth2.service.OAuth2ClientService;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.OAuth2UserService;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.auth.enums.UserEnum;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.constant.UnifiedOauthConstant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Keycloak 授权码登录
 */
@Component
@ConditionalOnProperty(value = "keycloak.enable", havingValue = "true")
public class KeycloakTokenGranter extends AbstractTokenGranter {

	public static final String GRANT_TYPE = "keycloak";

	/**
	 * Keycloak 自动注册默认角色别名
	 */
	public static final String COMMON_USER_ROLE_ALIAS = "common_user";

	private final KeycloakService keycloakService;
	private final IUserService userService;
	private final IRoleService roleService;
	private final OAuth2ClientService clientService;

	public KeycloakTokenGranter(OAuth2ClientService clientService, OAuth2UserService oAuth2UserService,
								PasswordHandler passwordHandler, KeycloakService keycloakService,
								IUserService userService, IRoleService roleService) {
		super(clientService, oAuth2UserService, passwordHandler);
		this.clientService = clientService;
		this.keycloakService = keycloakService;
		this.userService = userService;
		this.roleService = roleService;
	}

	@Override
	public String type() {
		return GRANT_TYPE;
	}

	@Override
	public OAuth2User user(OAuth2Request request) {
		String code = request.getCode();
		if (Func.isBlank(code)) {
			throw new ServiceException("Keycloak 授权码不能为空");
		}
		KeycloakUserProfile profile = keycloakService.resolveUserProfile(code);
		String username = profile.getUsername();
		String tenantId = Func.toStr(request.getTenantId(), TokenUtil.DEFAULT_TENANT_ID);
		User user = userService.userByAccount(tenantId, username);
		if (user == null) {
			user = autoRegister(tenantId, profile);
		}
		if (user.getStatus() != null && user.getStatus() != 1) {
			throw new ServiceException("用户已被禁用");
		}
		UserInfo userInfo = userService.userInfo(String.valueOf(user.getId()), UserEnum.WEB);
		if (userInfo == null || userInfo.getUser() == null) {
			throw new ServiceException("用户信息加载失败");
		}
		if (Func.isEmpty(userInfo.getRoles())) {
			throw new ServiceException("未获得用户的角色信息");
		}

		User updateUser = new User();
		updateUser.setId(user.getId());
		updateUser.setLastLoginTime(new Date());
		updateUser.setLastLoginType(UnifiedOauthConstant.SSO_LOGIN_TYPE);
		userService.updateById(updateUser);

		OAuth2User oauth2User = TokenUtil.convertUser(userInfo, request);
		oauth2User.setClient(keycloakClient(request));
		return oauth2User;
	}

	/**
	 * Keycloak 登录成功但本系统无对应用户时自动注册
	 */
	private User autoRegister(String tenantId, KeycloakUserProfile profile) {
		String roleId = roleService.getRoleIdByAlias(tenantId, COMMON_USER_ROLE_ALIAS);
		if (Func.isBlank(roleId)) {
			throw new ServiceException("未配置角色 [" + COMMON_USER_ROLE_ALIAS + "]，无法自动注册用户，请联系管理员");
		}
		User user = new User();
		user.setTenantId(tenantId);
		user.setAccount(profile.getUsername());
		String displayName = Func.toStr(profile.getName(), profile.getUsername());
		user.setName(displayName);
		user.setRealName(displayName);
		user.setEmail(profile.getEmail());
		user.setRoleId(roleId);
		user.setUserType(String.valueOf(UserEnum.WEB.getCategory()));
		user.setStatus(CommonConstant.DB_STATUS_NORMAL);
		String password = Func.toStr(ParamCache.getValue(CommonConstant.DEFAULT_PARAM_PASSWORD), CommonConstant.DEFAULT_PASSWORD);
		user.setPassword(password);
		try {
			userService.submit(user);
		} catch (ServiceException ex) {
			User existing = userService.userByAccount(tenantId, profile.getUsername());
			if (existing != null) {
				return existing;
			}
			throw ex;
		}
		return user;
	}

	private OAuth2Client keycloakClient(OAuth2Request request) {
		OAuth2Client client = clientService.loadByClientId(request.getClientId());
		if (client == null || !Func.equals(client.getClientSecret(), request.getClientSecret())) {
			throw new ServiceException("客户端认证失败");
		}
		return client;
	}

}
