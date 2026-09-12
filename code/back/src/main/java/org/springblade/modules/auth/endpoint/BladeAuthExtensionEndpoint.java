package org.springblade.modules.auth.endpoint;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.wf.captcha.SpecCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.oauth2.constant.OAuth2TokenConstant;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.springblade.common.constant.TenantConstant.DEFAULT_PASSWORD;
import static org.springblade.core.cache.constant.CacheConstant.MENU_CACHE;
import static org.springblade.core.cache.constant.CacheConstant.USER_CACHE;

/**
 * OAuth2 扩展端点（框架未覆盖的能力）
 */
@NonDS
@ApiSort(2)
@RestController
@AllArgsConstructor
@Tag(name = "授权扩展接口", description = "OAuth2 扩展能力")
public class BladeAuthExtensionEndpoint {

	private final IUserService userService;
	private final BladeRedis bladeRedis;

	@PostMapping("/oauth/user-info")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "获取用户信息")
	public R<Kv> userInfo() {
		BladeUser user = AuthUtil.getUser();
		if (user == null || user.getUserId() == null) {
			return R.fail("用户信息不存在");
		}
		return R.data(Kv.create()
			.set("userId", user.getUserId())
			.set("roleId", user.getRoleId())
			.set("deptId", user.getDeptId())
			.set("roles", Func.toStrList(user.getRoleName())));
	}

	@PostMapping("/oauth/captcha")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "获取验证码")
	public R<Kv> captcha() {
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
		String verCode = specCaptcha.text().toLowerCase();
		String key = UUID.randomUUID().toString();
		bladeRedis.setEx(OAuth2TokenConstant.CAPTCHA_CACHE_KEY + key, verCode, Duration.ofMinutes(30));
		return R.data(Kv.create().set("key", key).set("image", specCaptcha.toBase64()));
	}

	@PostMapping("/oauth/clear-cache")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "清除缓存")
	public R<Kv> clearCache() {
		BladeUser user = AuthUtil.getUser();
		if (user != null && Func.isNotBlank(user.getTenantId())) {
			CacheUtil.clear(MENU_CACHE, user.getTenantId());
			CacheUtil.clear(MENU_CACHE,  Boolean.FALSE);
			CacheUtil.clear(USER_CACHE, user.getTenantId());
		}
		return R.data(Kv.create().set("success", true).set("msg", "清除缓存成功"));
	}

	@PostMapping("/oauth/verify-set-password")
	@Operation(summary = "校验是否修改了初始密码/以及密码是否长时间没修改")
	public Kv verifySetPassword() {
		String isNecessary = ParamCache.getValue(ParamCacheConstant.IS_NEED_RESET_PASSWORD);
		String isUserResetPasswordStr = "false";
		BladeUser user = AuthUtil.getUser();
		if ("true".equals(isNecessary)) {
			String password = Func.toStr(ParamCache.getValue(CommonConstant.DEFAULT_PARAM_PASSWORD), DEFAULT_PASSWORD);
			Boolean isUserResetPassword = userService.isUserExists(user.getTenantId(), user.getAccount(), DigestUtil.encrypt(password));
			isUserResetPasswordStr = isUserResetPassword.toString();
		}

		String passwordExpireTime = ParamCache.getValue(ParamCacheConstant.PASSWORD_EXPIRE_TIME);
		String passwordExpireTimeStr = "false";
		if (Func.isNotBlank(passwordExpireTime) && Func.toInt(passwordExpireTime) > 0) {
			User userInfo = userService.getById(user.getUserId());
			if (userInfo.getLastChangePasswordTime() == null) {
				userInfo.setLastChangePasswordTime(userInfo.getCreateTime());
				if (userInfo.getLastChangePasswordTime() == null) {
					userInfo.setLastChangePasswordTime(new Date());
				}
				userService.updateById(userInfo);
			}
			if (userInfo.getLastChangePasswordTime() != null) {
				long timeDiff = System.currentTimeMillis() - userInfo.getLastChangePasswordTime().getTime();
				long daysDiff = timeDiff / (1000 * 60 * 60 * 24);
				if (daysDiff >= Func.toInt(passwordExpireTime)) {
					passwordExpireTimeStr = "true";
				}
			}
		}

		return Kv.create().set("success", "true").set("msg", "success")
			.set("isPasswordExpireTime", passwordExpireTimeStr)
			.set("isUserResetPassword", isUserResetPasswordStr);
	}
}
