package org.springblade.common.filter;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.api.ResultCode;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.Func;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class CookieSecureFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(CookieSecureFilter.class);
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Filter.super.init(filterConfig);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		Cookie[] cookies = httpServletRequest.getCookies();

		BladeUser bladeUser = AuthUtil.getUser();
//		AuthUtil.
		 if (bladeUser!=null && Func.notNull(bladeUser.getUserId()) && cookies != null) {
			 String cookieToken = Arrays.stream(cookies).filter(i-> "saber-access-token".equals(i.getName())).map(Cookie::getValue).findFirst().orElse(null);
			 if (Func.isNotBlank(cookieToken)) {
				 Claims claims = AuthUtil.parseJWT(cookieToken);
				 Long cookieUserId = Long.valueOf((String) claims.get(TokenConstant.USER_ID));
				 if (!bladeUser.getUserId().equals(cookieUserId))  {
					 write((HttpServletResponse)servletResponse);
					 return;
				 }
			 }
        }
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		Filter.super.destroy();
	}

	private void write(HttpServletResponse response) {
		R result = R.fail(ResultCode.UN_AUTHORIZED);
		result.setMsg("cookie设置和密钥不一致，请退出登录清空缓存之后重新登录！");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Content-type", "application/json");
		response.setStatus(401);

		try {
			response.getWriter().write((String) Objects.requireNonNull(JsonUtil.toJson(result)));
		} catch (IOException e) {
			log.error(e.getMessage());
		}

	}
}
