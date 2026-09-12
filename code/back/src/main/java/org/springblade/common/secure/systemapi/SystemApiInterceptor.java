package org.springblade.common.secure.systemapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.secure.domain.ApiUserInfo;
import org.springblade.core.secure.provider.HttpMethod;
import org.springblade.core.secure.provider.ResponseProvider;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 系统开放 API 鉴权拦截器：匹配配置路径后校验 Basic 凭证与调用方用户头。
 */
@Slf4j
@RequiredArgsConstructor
public class SystemApiInterceptor implements HandlerInterceptor {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private static final String PARAM_ACCESS_KEY = "accessKey";

	private static final String PARAM_SECRET_KEY = "secretKey";

	private final List<SystemApiSecure> systemApiSecures;

	@Override
	public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
							 @NonNull Object handler) {
		boolean matched = systemApiSecures.stream().anyMatch(rule -> matchesRule(request, rule));
		if (!matched) {
			return true;
		}
		if (authenticate(request)) {
			return true;
		}
		log.warn("系统 API 鉴权失败，uri={}，ip={}", request.getRequestURI(), WebUtil.getIP(request));
		response.setHeader("WWW-Authenticate", "Basic realm=\"system-api\"");
		ResponseProvider.write(response);
		return false;
	}

	private boolean matchesRule(HttpServletRequest request, SystemApiSecure rule) {
		return matchesMethod(request, rule.getMethod()) && matchesPath(request, rule.getPattern());
	}

	private boolean matchesMethod(HttpServletRequest request, HttpMethod method) {
		return method == HttpMethod.ALL || method == HttpMethod.of(request.getMethod());
	}

	private boolean matchesPath(HttpServletRequest request, String pattern) {
		String servletPath = request.getServletPath();
		String pathInfo = request.getPathInfo();
		if (Func.isNotBlank(pathInfo)) {
			servletPath = servletPath + pathInfo;
		}
		return PATH_MATCHER.match(pattern, servletPath);
	}

	private boolean authenticate(HttpServletRequest request) {
		try {
			String accessKey = ParamCache.getValue(PARAM_ACCESS_KEY);
			String secretKey = ParamCache.getValue(PARAM_SECRET_KEY);
			if (Func.isBlank(accessKey) || Func.isBlank(secretKey)) {
				log.warn("系统 API 凭证未配置，请在参数表配置 {} / {}", PARAM_ACCESS_KEY, PARAM_SECRET_KEY);
				return false;
			}
			String[] credentials = SecureUtil.extractAndDecodeAuthorization();
			if (credentials == null || credentials.length < 2) {
				return false;
			}
			if (!accessKey.equals(credentials[0]) || !secretKey.equals(credentials[1])) {
				return false;
			}
			ApiUserInfo userInfo = SystemApiUtil.parseUserInfo(request);
			log.debug("系统 API 鉴权通过，account={}，uri={}", userInfo.getUserAccount(), request.getRequestURI());
			return true;
		} catch (Exception ex) {
			log.debug("系统 API 鉴权异常: {}", ex.getMessage());
			return false;
		}
	}

}
