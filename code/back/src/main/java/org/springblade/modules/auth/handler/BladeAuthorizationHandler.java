package org.springblade.modules.auth.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.cache.SysCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.common.constant.TenantConstant;
import org.springblade.core.launch.props.BladeProperties;
import org.springblade.core.oauth2.exception.ExceptionCode;
import org.springblade.modules.auth.exception.NeedConfirmLoginException;
import org.springblade.core.oauth2.handler.AbstractAuthorizationHandler;
import org.springblade.core.oauth2.props.OAuth2Properties;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.provider.OAuth2Validation;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.tenant.BladeTenantProperties;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.DesUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.system.entity.Tenant;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.constant.UnifiedOauthConstant;
import org.springblade.modules.unifiedoauth.enums.LoginCheckResult;
import org.springblade.modules.unifiedoauth.util.SingleLoginUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 认证处理器：IP 限制、Redis 锁定、租户校验、SSO 互斥、单点登录确认
 */
@Slf4j
@RequiredArgsConstructor
public class BladeAuthorizationHandler extends AbstractAuthorizationHandler {

	private final BladeProperties bladeProperties;
	private final BladeTenantProperties tenantProperties;
	private final OAuth2Properties oAuth2Properties;
	private final BladeRedisLockHandler lockHandler;
	private final IUserService userService;

	@Override
	public OAuth2Validation preValidation(OAuth2Request request) {
		OAuth2Validation ipValidation = validateIp(request);
		if (!ipValidation.isSuccess()) {
			return ipValidation;
		}
		if (Boolean.TRUE.equals(request.isPassword()) || Boolean.TRUE.equals(request.isCaptchaCode())) {
			OAuth2Validation accountValidation = lockHandler.validateAccountLock(request.getTenantId(), request.getUsername());
			if (!accountValidation.isSuccess()) {
				return accountValidation;
			}
		}
		return super.preValidation(request);
	}

	@Override
	public void preFailure(OAuth2Request request, OAuth2Validation validation) {
		if (Boolean.TRUE.equals(request.isPassword()) || Boolean.TRUE.equals(request.isCaptchaCode())) {
			lockHandler.handleAuthFailure(request.getTenantId(), request.getUsername());
		}
		log.error("用户：{}，认证失败，失败原因：{}", request.getUsername(), validation.getMessage());
	}

	@Override
	public OAuth2Validation authValidation(OAuth2User user, OAuth2Request request) {
		if (Boolean.TRUE.equals(request.isPassword()) || Boolean.TRUE.equals(request.isRefreshToken()) || Boolean.TRUE.equals(request.isCaptchaCode())) {
			OAuth2Validation tenantValidation = validateTenant(user.getTenantId());
			if (!tenantValidation.isSuccess()) {
				return tenantValidation;
			}
		}
		if (Boolean.TRUE.equals(request.isPassword()) || Boolean.TRUE.equals(request.isCaptchaCode())) {
			User dbUser = userService.getById(Func.toLong(user.getUserId()));
			if (dbUser != null) {
				if (dbUser.getStatus() != null && dbUser.getStatus() != 1) {
					return buildValidationFailure(ExceptionCode.INVALID_USER);
				}
				if (Func.notNull(dbUser.getLoginType()) && UnifiedOauthConstant.SSO_LOGIN_TYPE.equals(dbUser.getLoginType())) {
					return failure("当前账号登录方式为仅单点登录", ExceptionCode.ACCESS_DENIED);
				}
			}
			HttpServletRequest httpRequest = WebUtil.getRequest();
			boolean confirm = httpRequest != null && "true".equals(httpRequest.getHeader(CommonConstant.HEADER_CONFIRM_KEY));
			LoginCheckResult checkResult = SingleLoginUtil.checkLogin(user.getTenantId(), request.getClientId(), user.getUserId(), confirm);
			if (LoginCheckResult.NEED_CONFIRM == checkResult) {
				throw new NeedConfirmLoginException();
			}
		}
		return super.authValidation(user, request);
	}

	@Override
	public void authSuccessful(OAuth2User user, OAuth2Request request) {
		lockHandler.handleAuthSuccess(user.getTenantId(), user.getAccount());
		if (Boolean.TRUE.equals(request.isPassword()) || Boolean.TRUE.equals(request.isCaptchaCode())) {
			User updateUser = new User();
			updateUser.setId(Func.toLong(user.getUserId()));
			updateUser.setLastLoginTime(new Date());
			updateUser.setLastLoginCode("");
			updateUser.setLastLoginType(UnifiedOauthConstant.ACCOUNT_LOGIN_TYPE);
			userService.updateById(updateUser);
		}
		log.info("用户：{}，认证成功", user.getAccount());
	}

	@Override
	public void authFailure(OAuth2User user, OAuth2Request request, OAuth2Validation validation) {
		// 预留扩展
	}

	private OAuth2Validation validateTenant(String tenantId) {
		// 单租户模式：未开启授权保护时跳过租户表校验，避免触发 4.9 增强多租户逻辑
		if (!Boolean.TRUE.equals(tenantProperties.getLicense())) {
			return new OAuth2Validation();
		}
		Tenant tenant = SysCache.getTenant(tenantId);
		if (tenant == null) {
			return buildValidationFailure(ExceptionCode.USER_TENANT_NOT_FOUND);
		}
		Date expireTime = tenant.getExpireTime();
		if (tenantProperties.getLicense()) {
			String licenseKey = tenant.getLicenseKey();
			String decrypt = DesUtil.decryptFormHex(licenseKey, TenantConstant.DES_KEY);
			Tenant license = JsonUtil.parse(decrypt, Tenant.class);
			if (license == null || !license.getId().equals(tenant.getId())) {
				return buildValidationFailure(ExceptionCode.UNAUTHORIZED_USER_TENANT);
			}
			expireTime = license.getExpireTime();
		}
		if (expireTime != null && expireTime.before(DateUtil.now())) {
			return buildValidationFailure(ExceptionCode.UNAUTHORIZED_USER_TENANT);
		}
		return new OAuth2Validation();
	}

	private OAuth2Validation validateIp(OAuth2Request request) {
		HttpServletRequest servletRequest = WebUtil.getRequest();
		if (servletRequest == null) {
			return new OAuth2Validation();
		}
		String white = ParamCache.getValue(ParamCacheConstant.VISIT_IP_WHITE_LIST);
		String black = ParamCache.getValue(ParamCacheConstant.VISIT_IP_BLACK_LIST);
		String ip = getIp(servletRequest);
		if (Func.isNotBlank(white) && !"-1".equals(white)) {
			boolean allowed = false;
			for (String wip : white.split(",")) {
				if (wip.equals(ip)) {
					allowed = true;
					break;
				}
			}
			if (!allowed) {
				return failure("IP 地址不在白名单内，请联系管理员", ExceptionCode.ACCESS_DENIED);
			}
		}
		if (Func.isNotBlank(black) && !"-1".equals(black)) {
			for (String bip : black.split(",")) {
				if (bip.equals(ip)) {
					return failure("IP 地址在黑名单内，请联系管理员", ExceptionCode.ACCESS_DENIED);
				}
			}
		}
		return new OAuth2Validation();
	}

	private String getIp(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-Forwarded-For");
		if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress.split(",")[0];
	}

	private OAuth2Validation failure(String message, ExceptionCode code) {
		OAuth2Validation validation = new OAuth2Validation();
		validation.setSuccess(false);
		validation.setCode(code.getCode());
		validation.setMessage(message);
		return validation;
	}
}
