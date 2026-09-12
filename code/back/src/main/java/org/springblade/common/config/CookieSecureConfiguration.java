package org.springblade.common.config;

import org.springblade.common.filter.CookieSecureFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CookieSecureConfiguration {

	/**
	 * CookieSecureFilter
	 */
	@Bean
	public CookieSecureFilter cookieSecureFilter() {
		return new CookieSecureFilter();
	}

}
