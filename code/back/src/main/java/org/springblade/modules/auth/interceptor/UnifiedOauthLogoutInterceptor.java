package org.springblade.modules.auth.interceptor;

import lombok.RequiredArgsConstructor;
import org.dfyj.unifiedoauth.service.UnifiedOauthService;
import org.springblade.common.cache.ParamCache;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 标准登出后同步统一授权登录状态
 */
@Component
@RequiredArgsConstructor
public class UnifiedOauthLogoutInterceptor implements HandlerInterceptor {

	private static final String LOGOUT_USER_ATTR = "blade.logout.user";

	private final UnifiedOauthService unifiedOauthService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isLogoutRequest(request)) {
			BladeUser user = AuthUtil.getUser();
			if (user != null) {
				request.setAttribute(LOGOUT_USER_ATTR, user);
			}
		}
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if (!isLogoutRequest(request)) {
			return;
		}
		Object attr = request.getAttribute(LOGOUT_USER_ATTR);
		if (!(attr instanceof BladeUser user)) {
			return;
		}
		if ("enable".equals(ParamCache.getValue("oauth2"))) {
			unifiedOauthService.changeLoginStatus(user.getAccount(), ParamCache.getValue("system.id"));
		}
	}

	private boolean isLogoutRequest(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.endsWith("/blade-auth/oauth/logout");
	}
}
