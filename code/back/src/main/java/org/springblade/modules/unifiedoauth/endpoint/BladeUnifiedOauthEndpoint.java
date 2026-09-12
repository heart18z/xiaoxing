package org.springblade.modules.unifiedoauth.endpoint;

import com.alibaba.fastjson.JSONObject;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.dfyj.unifiedoauth.rpc.domain.Oauth2AccountUserDTO;
import org.dfyj.unifiedoauth.service.UnifiedOauthService;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.log.annotation.ApiLog;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.auth.constant.BladeOAuth2ErrorCode;
import org.springblade.modules.auth.utils.OAuth2TokenIssueHelper;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.constant.UnifiedOauthConstant;
import org.springblade.modules.unifiedoauth.enums.LoginCheckResult;
import org.springblade.modules.unifiedoauth.service.UnifiedOauthUserLoader;
import org.springblade.modules.unifiedoauth.util.SingleLoginUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Objects;

@NonDS
@ApiSort(2)
@RestController
@AllArgsConstructor
@Tag(name = "授权接口", description = "用户授权认证")
@RequestMapping("unified-oauth")
public class BladeUnifiedOauthEndpoint {

	private final IUserService userService;
	private final UnifiedOauthUserLoader unifiedOauthUserLoader;
	private final OAuth2TokenIssueHelper tokenIssueHelper;
	private final UnifiedOauthService myUnifiedOauthService;

	@ApiLog("统一授权系统登录用户验证")
	@PostMapping("/login")
	@Operation(summary = "获取认证令牌", description = "传入用户编码:userCode,编码:code")
	@Transactional
	public Kv authToken(@RequestParam("userCode") String userCode, @RequestParam("code") String code) {
		Kv authInfo = Kv.create();
		String username = myUnifiedOauthService.checkLoginCode(userCode, code);
		UserInfo userInfo = unifiedOauthUserLoader.loadUserInfo(username);

		if (userInfo == null || userInfo.getUser() == null) {
			return authInfo.set("error_code", HttpServletResponse.SC_BAD_REQUEST).set("error_description", "用户名或密码不正确");
		}
		if (Func.isEmpty(userInfo.getRoles())) {
			return authInfo.set("error_code", HttpServletResponse.SC_BAD_REQUEST).set("error_description", "未获得用户的角色信息");
		}
		String clientId = OAuth2Request.create().buildClientArgs().getClientId();
		LoginCheckResult checkResult = SingleLoginUtil.checkLogin(
			userInfo.getUser().getTenantId(),
			clientId,
			String.valueOf(userInfo.getUser().getId()),
			"true".equals(WebUtil.getRequest().getHeader(CommonConstant.HEADER_CONFIRM_KEY)));
		if (LoginCheckResult.NEED_CONFIRM == checkResult) {
			return authInfo
				.set("error", BladeOAuth2ErrorCode.NEED_CONFIRM_LOGIN)
				.set("error_description", BladeOAuth2ErrorCode.NEED_CONFIRM_LOGIN_MESSAGE);
		}

		User user = new User();
		user.setId(userInfo.getUser().getId());
		user.setLastLoginTime(new Date());
		user.setLastLoginCode(code);
		user.setLastLoginType(UnifiedOauthConstant.SSO_LOGIN_TYPE);
		userService.updateById(user);

		OAuth2Request request = OAuth2Request.create()
			.buildArgs()
			.buildHeaderArgs()
			.buildParameterArgs()
			.buildClientArgs();
		OAuth2User oauth2User = TokenUtil.convertUser(userInfo, request);
		return tokenIssueHelper.issueToken(oauth2User, request);
	}

	@PostMapping("/get-login-url")
	public String getLoginUrl() {
		return myUnifiedOauthService.getUnifiedOauthLoginUrl();
	}

	@PostMapping("/logout-all")
	public R logoutAll() {
		User user = userService.getById(AuthUtil.getUserId());
		if (!Objects.equals(user.getLastLoginType(), UnifiedOauthConstant.SSO_LOGIN_TYPE)) {
			throw new org.springblade.core.log.exception.ServiceException("当前登录状态不是统一授权登录，无法退出登录");
		}
		myUnifiedOauthService.logoutAll(user.getAccount(), ParamCache.getValue("system.id"));
		return R.success("操作成功");
	}

	@PostMapping("/user-oauth2-account")
	public String oauth2Account() {
		return myUnifiedOauthService.getOauthUserInfo(
			AuthUtil.getUserAccount(),
			ParamCache.getValue("system.id"));
	}

	@PostMapping("/user-oauth2-unbind")
	public String oauth2AccountUnbind() {
		String res = myUnifiedOauthService.unbindOauth(AuthUtil.getUserAccount(), ParamCache.getValue("system.id"));
		JSONObject o = JSONObject.parseObject(res);
		if (o != null) {
			Object codeObj = o.get("code");
			if (Func.notNull(codeObj) && codeObj.toString().equals("200")) {
				User user = new User();
				user.setId(AuthUtil.getUserId());
				user.setLoginType(UnifiedOauthConstant.ACCOUNT_LOGIN_TYPE);
				this.userService.updateById(user);
			}
		}
		return res;
	}

	@PostMapping("/user-oauth2-account-check")
	public String checkAuthUsername(String account) {
		return myUnifiedOauthService.checkAuthUsername(account, ParamCache.getValue("system.id"));
	}

	@PostMapping("/submit-oauth2-account")
	public R submitOauth2Account(@RequestBody Oauth2AccountUserDTO oauth2AccountUser) {
		String username = oauth2AccountUser.getUsername();
		String password = oauth2AccountUser.getPassword();
		String loginType = oauth2AccountUser.getLoginType();

		myUnifiedOauthService.submitOauth2Account(username, password, AuthUtil.getUserAccount(), loginType, ParamCache.getValue("system.id"));

		if (Func.isNull(loginType)) {
			loginType = UnifiedOauthConstant.ACCOUNT_LOGIN_TYPE;
		}
		String finalLoginType = loginType;
		userService.updateById(new User() {{
			setId(AuthUtil.getUserId());
			setLoginType(finalLoginType);
		}});
		return R.success("操作成功");
	}
}
