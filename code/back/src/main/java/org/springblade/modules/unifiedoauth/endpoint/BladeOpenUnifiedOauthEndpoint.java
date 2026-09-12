package org.springblade.modules.unifiedoauth.endpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.dfyj.unifiedoauth.domain.UnifiedOauthLogoutData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.jwt.JwtUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.constant.UnifiedOauthConstant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@NonDS
@ApiSort(3)
@RestController
@AllArgsConstructor
@Tag(name = "授权接口", description = "用户授权认证")
public class BladeOpenUnifiedOauthEndpoint {

	private final Logger logger = LoggerFactory.getLogger(BladeOpenUnifiedOauthEndpoint.class);

	private IUserService userService;
	// 所开放退出登录系统
	@PostMapping("/open/unified-oauth/logout")
	@Operation(summary = "退出登录")
	public Kv logout(@RequestBody UnifiedOauthLogoutData params) {
		logger.info("统一授权系统退出登录参数======>:{}",params);
		if (Func.isEmpty(params) ||
			Func.isBlank(params.getCode()) ||
			Func.isEmpty(params.getTimestamp()) ||
			Func.isBlank(params.getSystem_username())){
			Kv.create().set("fail", "true").set("msg", "必要参数为空");
		}
		User user = userService.getOne(new
			LambdaQueryWrapper<User>().eq(User::getAccount,params.getSystem_username())
			.eq(User::getLastLoginType, UnifiedOauthConstant.SSO_LOGIN_TYPE)
		);
		if (user != null) {
			JwtUtil.removeAccessToken(user.getTenantId(),  String.valueOf(user.getId()));
			JwtUtil.removeRefreshToken(user.getTenantId(), String.valueOf(user.getId()));
		}
		return Kv.create().set("success", "true").set("msg", "success");
	}
}
