package org.springblade.modules.smartreminder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;

/**
 * Keeps long-running AI streams alive beyond Spring MVC's short default timeout.
 */
@Configuration
public class SmartReminderWebConfig implements WebMvcConfigurer {

	private static final long AI_STREAM_TIMEOUT_MILLIS = 10 * 60 * 1000L;

	/** Preflight must complete before OAuth interceptors. No wildcard web origins/cookies. */
	@Bean
	public FilterRegistrationBean<CorsFilter> nativeCorsFilter() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(List.of("capacitor://localhost"));
		cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Blade-Auth", "Blade-Requested-With", "Content-Type", "Accept", "Accept-Language", "Tenant-Id", "Dept-Id", "Role-Id", "Captcha-Key", "Captcha-Code", "confirm"));
		cors.setAllowCredentials(false);
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		for (String path : List.of("/app/**", "/blade-auth/**", "/blade-resource/**", "/blade-system/tenant/info")) source.registerCorsConfiguration(path, cors);
		FilterRegistrationBean<CorsFilter> filter = new FilterRegistrationBean<>(new CorsFilter(source) {
			@Override protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
				// Preserve existing browser/proxy CORS handling; this filter serves native origins only.
				String origin = request.getHeader("Origin");
				return origin == null || !origin.startsWith("capacitor:");
			}
		});
		filter.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return filter;
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setDefaultTimeout(AI_STREAM_TIMEOUT_MILLIS);
	}
}
