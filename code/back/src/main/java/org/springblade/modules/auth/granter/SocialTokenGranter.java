package org.springblade.modules.auth.granter;

import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.springblade.core.oauth2.exception.OAuth2ErrorCode;
import org.springblade.core.oauth2.granter.AbstractTokenGranter;
import org.springblade.core.oauth2.handler.PasswordHandler;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2ClientService;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.OAuth2UserService;
import org.springblade.core.oauth2.utils.OAuth2ExceptionUtil;
import org.springblade.core.social.props.SocialProperties;
import org.springblade.core.social.utils.SocialUtil;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.entity.UserOauth;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 第三方社交登录
 */
@Component
public class SocialTokenGranter extends AbstractTokenGranter {

	private static final Integer AUTH_SUCCESS_CODE = 2000;

	private final IUserService userService;
	private final SocialProperties socialProperties;

	public SocialTokenGranter(OAuth2ClientService clientService, OAuth2UserService oAuth2UserService,
							  PasswordHandler passwordHandler, IUserService userService, SocialProperties socialProperties) {
		super(clientService, oAuth2UserService, passwordHandler);
		this.userService = userService;
		this.socialProperties = socialProperties;
	}

	@Override
	public String type() {
		return SOCIAL;
	}

	@Override
	public OAuth2User user(OAuth2Request request) {
		String tenantId = request.getTenantId();
		String sourceParameter = request.getSource();
		String source = socialProperties.getAlias().getOrDefault(sourceParameter, sourceParameter);
		String code = request.getCode();
		String state = request.getState();

		AuthRequest authRequest = SocialUtil.getAuthRequest(source, socialProperties);
		AuthCallback authCallback = new AuthCallback();
		authCallback.setCode(code);
		authCallback.setState(state);
		AuthResponse<?> authResponse = authRequest.login(authCallback);
		AuthUser authUser;
		if (authResponse.getCode() == AUTH_SUCCESS_CODE) {
			authUser = (AuthUser) authResponse.getData();
		} else {
			OAuth2ExceptionUtil.throwFromCode(OAuth2ErrorCode.INVALID_USER);
			return null;
		}

		UserOauth userOauth = Objects.requireNonNull(BeanUtil.copyProperties(authUser, UserOauth.class));
		userOauth.setSource(authUser.getSource());
		userOauth.setTenantId(tenantId);
		userOauth.setUuid(authUser.getUuid());
		UserInfo userInfo = userService.userInfo(userOauth);

		OAuth2User user = TokenUtil.convertUser(userInfo, request);
		user.setClient(client(request));
		return user;
	}
}
