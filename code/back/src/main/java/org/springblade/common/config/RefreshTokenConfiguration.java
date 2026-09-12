package org.springblade.common.config;

import org.dfyj.unifiedoauth.service.UnifiedOauthService;
import org.springblade.common.filter.RefreshTokenFilter;
import org.springblade.core.jwt.props.JwtProperties;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.modules.auth.utils.OAuth2TokenIssueHelper;
import org.springblade.modules.system.service.IUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * access_token 服务端静默续期 Filter 配置。
 * 由 blade.auth.refresh-filter.enabled 控制开关，不依赖 refresh_token。
 */
@Configuration
public class RefreshTokenConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "blade.auth.refresh-filter", name = "enabled", havingValue = "true")
	public FilterRegistrationBean<RefreshTokenFilter> refreshTokenFilter(JwtProperties jwtProperties,
																		  BladeRedis bladeRedis,
																		  IUserService userService,
																		  OAuth2TokenIssueHelper tokenIssueHelper,
																		  UnifiedOauthService unifiedOauthService) {
		FilterRegistrationBean<RefreshTokenFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new RefreshTokenFilter(jwtProperties, bladeRedis, userService, tokenIssueHelper, unifiedOauthService));
		registration.addUrlPatterns("/*");
		registration.setName("refreshTokenFilter");
		registration.setOrder(1);
		return registration;
	}
}
