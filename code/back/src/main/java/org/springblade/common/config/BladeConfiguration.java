package org.springblade.common.config;

import org.springblade.common.annotation.interceptor.RepeatSubmitInterceptor;
import org.springblade.common.secure.systemapi.SystemApiSecureRegistry;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.oauth2.endpoint.OAuth2SocialEndpoint;
import org.springblade.core.oauth2.endpoint.OAuth2TokenEndPoint;
import org.springblade.core.secure.registry.SecureRegistry;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.modules.auth.endpoint.BladeAuthExtensionEndpoint;
import org.springblade.modules.auth.keycloak.KeycloakEndpoint;
import org.springblade.modules.auth.keycloak.KeycloakStatusEndpoint;
import org.springblade.modules.auth.interceptor.UnifiedOauthLogoutInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Blade配置
 */
@Configuration(proxyBeanMethods = false)
public class BladeConfiguration implements WebMvcConfigurer {

	@Autowired
	private RepeatSubmitInterceptor repeatSubmitInterceptor;

	@Autowired
	private UnifiedOauthLogoutInterceptor unifiedOauthLogoutInterceptor;

	@Bean
	public SecureRegistry secureRegistry() {
		SecureRegistry secureRegistry = new SecureRegistry();
		secureRegistry.setEnabled(true);
		// 4.9 默认开启严格 Token/请求头校验；历史用户 dept_id 可能为空，关闭后与升级前行为一致
		secureRegistry.strictToken(false);
		secureRegistry.strictHeader(false);
		secureRegistry.excludePathPatterns("/blade-auth/**");
		// This single endpoint uses a random installation secret + versioned binding proof,
		// allowing logout cleanup after OAuth expires. Registration remains authenticated.
		secureRegistry.excludePathPatterns("/app/push/revoke");
		secureRegistry.excludePathPatterns("/unified-oauth/**");
		secureRegistry.excludePathPatterns("/open/unified-oauth/**");
		secureRegistry.excludePathPatterns("/blade-system/menu/routes");
		secureRegistry.excludePathPatterns("/blade-system/menu/auth-routes");
		secureRegistry.excludePathPatterns("/blade-system/menu/top-menu");
		secureRegistry.excludePathPatterns("/blade-system/tenant/info");
		secureRegistry.excludePathPatterns("/blade-flow/process/resource-view");
		secureRegistry.excludePathPatterns("/blade-flow/process/diagram-view");
		secureRegistry.excludePathPatterns("/blade-flow/manager/check-upload");
		secureRegistry.excludePathPatterns("/doc.html");
		secureRegistry.excludePathPatterns("/js/**");
		secureRegistry.excludePathPatterns("/webjars/**");
		secureRegistry.excludePathPatterns("/swagger-resources/**");
		secureRegistry.excludePathPatterns("/druid/**");
		return secureRegistry;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/cors/**")
			.allowedOriginPatterns("*")
			.allowedHeaders("*")
			.allowedMethods("*")
			.maxAge(3600)
			.allowCredentials(true);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
		registry.addInterceptor(unifiedOauthLogoutInterceptor).addPathPatterns("/blade-auth/oauth/logout");
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(StringPool.SLASH + AppConstant.APPLICATION_AUTH_NAME,
			c -> c.isAnnotationPresent(RestController.class) && (
				OAuth2TokenEndPoint.class.equals(c)
					|| OAuth2SocialEndpoint.class.equals(c)
					|| BladeAuthExtensionEndpoint.class.equals(c)
					|| KeycloakEndpoint.class.equals(c)
					|| KeycloakStatusEndpoint.class.equals(c)
			));
	}
}
