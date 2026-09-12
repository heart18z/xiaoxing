package org.springblade.common.filter;

import com.alibaba.fastjson.JSON;
import org.dfyj.unifiedoauth.service.UnifiedOauthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.CacheNames;
import org.springblade.common.cache.ParamCache;
import org.springblade.core.jwt.JwtUtil;
import org.springblade.core.jwt.props.JwtProperties;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.oauth2.provider.OAuth2Response;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.auth.utils.OAuth2TokenIssueHelper;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IUserService;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * access_token 自动续期：请求携带的 JWT 超过有效期 50% 时静默换发新 token。
 * 不依赖 refresh_token，与 token_expire_time 参数「有操作自动续期」语义一致。
 */
@AllArgsConstructor
public class RefreshTokenFilter implements Filter {

	private static final String ACCESS_TOKEN_COOKIE = "saber-access-token";
	private static final String AUTH_HEADER = "Blade-Auth";
	private static final Duration REFRESH_LOCK_DURATION = Duration.ofMinutes(5);

	private final JwtProperties jwtProperties;
	private final BladeRedis bladeRedis;
	private final IUserService userService;
	private final OAuth2TokenIssueHelper tokenIssueHelper;
	private final UnifiedOauthService unifiedOauthService;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
		throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
		HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(httpServletRequest);

		if (isLogoutRequest(httpServletRequest)) {
			handleLogout(httpServletRequest, httpServletResponse);
			return;
		}
		if (!shouldSkip(httpServletRequest)) {
			Claims claims = getClaims(httpServletRequest);
			if (claims != null && claims.getExpiration() != null) {
				long startTime = Long.parseLong(claims.get("nbf").toString());
				long endTime = Long.parseLong(claims.get("exp").toString());
				if (isPastFiftyPercent(startTime, endTime)) {
					String oldToken = getToken(httpServletRequest);
					String tenantId = Func.toStr(claims.get(TokenConstant.TENANT_ID), TokenUtil.DEFAULT_TENANT_ID);
					String refreshedToken = resolveRefreshedToken(oldToken, tenantId, claims);
					if (Func.isNotBlank(refreshedToken)) {
						requestWrapper.addHeader(AUTH_HEADER, TokenConstant.BEARER + " " + refreshedToken);
						setCookie(httpServletResponse, refreshedToken);
					}
				}
			}
		}

		filterChain.doFilter(requestWrapper, httpServletResponse);
	}

	/**
	 * 登录、验证码等接口不参与静默续期，避免干扰认证流程。
	 */
	private boolean shouldSkip(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (Func.isBlank(uri)) {
			return true;
		}
		return uri.contains("/blade-auth/oauth/token")
			|| uri.contains("/blade-auth/oauth/captcha")
			|| uri.contains("/blade-auth/oauth/logout")
			|| uri.contains("/unified-oauth/login");
	}

	private boolean isLogoutRequest(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.endsWith("/blade-auth/oauth/logout");
	}

	private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		BladeUser user = AuthUtil.getUser();
		if (user != null && Boolean.TRUE.equals(jwtProperties.getState())) {
			String token = getToken(request);
			JwtUtil.removeAccessToken(user.getTenantId(), user.getClientId(), String.valueOf(user.getUserId()), token);
			JwtUtil.removeRefreshToken(user.getTenantId(), user.getClientId(), String.valueOf(user.getUserId()), token);
			if ("enable".equals(ParamCache.getValue("oauth2"))) {
				unifiedOauthService.changeLoginStatus(user.getAccount(), ParamCache.getValue("system.id"));
			}
		}
		Kv body = OAuth2Response.create().ofSuccessful("退出登录成功");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(JSON.toJSONString(body));
	}

	private String resolveRefreshedToken(String oldToken, String tenantId, Claims claims) {
		if (Func.isBlank(oldToken)) {
			return null;
		}
		String cacheKey = CacheNames.tenantKey(tenantId, CacheNames.REFRESH_TOKEN_KEY, oldToken);
		synchronized (RefreshTokenFilter.class) {
			String cachedToken = bladeRedis.get(cacheKey);
			if (Func.isNotBlank(cachedToken)) {
				return cachedToken;
			}
			UserInfo userInfo = grant(claims);
			if (userInfo == null || userInfo.getUser() == null) {
				return null;
			}
			OAuth2Request request = OAuth2Request.create()
				.buildArgs()
				.buildHeaderArgs()
				.buildParameterArgs()
				.buildClientArgs();
			OAuth2User oauth2User = TokenUtil.convertUser(userInfo, request);
			String newToken = Objects.requireNonNull(
				tokenIssueHelper.issueToken(oauth2User, request).get(TokenConstant.ACCESS_TOKEN)
			).toString();
			bladeRedis.setEx(cacheKey, newToken, REFRESH_LOCK_DURATION);
			return newToken;
		}
	}

	private static void setCookie(HttpServletResponse response, String token) {
		Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
		cookie.setPath("/");
		cookie.setMaxAge(360000);
		response.addCookie(cookie);
	}

	public static Claims getClaims(HttpServletRequest request) {
		String token = getToken(request);
		if (StringUtil.isNotBlank(token)) {
			return AuthUtil.parseJWT(token);
		}
		return null;
	}

	public static String getToken(HttpServletRequest request) {
		String auth = request.getHeader(AUTH_HEADER);
		if (StringUtil.isNotBlank(auth)) {
			return JwtUtil.getToken(auth);
		}
		String paramAuth = request.getParameter(AUTH_HEADER);
		return JwtUtil.getToken(paramAuth);
	}

	private UserInfo grant(Claims claims) {
		UserInfo userInfo = userService.userInfo(Long.parseLong(claims.get(TokenConstant.USER_ID).toString()));
		if (userInfo == null || userInfo.getUser() == null) {
			return userInfo;
		}
		userInfo.getUser().setDeptId(Func.toStrWithEmpty(claims.get(TokenConstant.DEPT_ID), ""));
		userInfo.getUser().setRoleId(Func.toStrWithEmpty(claims.get(TokenConstant.ROLE_ID), ""));
		userInfo.setRoles(Arrays.asList(Func.split(
			Func.toStrWithEmpty(claims.get(TokenConstant.ROLE_NAME), ""), ",")));
		return userInfo;
	}

	private static boolean isPastFiftyPercent(long startTime, long endTime) {
		long currentTime = System.currentTimeMillis() / 1000;
		long totalDuration = endTime - startTime;
		if (totalDuration <= 0) {
			return false;
		}
		long fiftyPercentTime = startTime + totalDuration / 2;
		return currentTime > fiftyPercentTime;
	}

}
