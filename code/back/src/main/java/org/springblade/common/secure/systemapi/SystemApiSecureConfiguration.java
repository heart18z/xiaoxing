package org.springblade.common.secure.systemapi;

import org.springblade.core.tool.utils.Func;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Order(4)
@AutoConfiguration
public class SystemApiSecureConfiguration implements WebMvcConfigurer {

	private final SystemApiSecureRegistry systemApiSecureRegistry;

	private final SystemApiSecureProperties secureProperties;

	private final ISystemApiHandler systemApiHandlerHandler;


	public SystemApiSecureConfiguration(final SystemApiSecureRegistry systemApiSecureRegistry,
										final SystemApiSecureProperties secureProperties,
										final ISystemApiHandler systemApiHandlerHandler) {
		this.systemApiSecureRegistry = systemApiSecureRegistry;
		this.secureProperties = secureProperties;
		this.systemApiHandlerHandler = systemApiHandlerHandler;
	}


	public void addInterceptors(@NonNull InterceptorRegistry registry) {
		List<SystemApiSecure> apiSecures =this.secureProperties.getSystemApi();
		if (Func.isNotEmpty(apiSecures)) {
			List<SystemApiSecure>	signSecures = this.systemApiSecureRegistry.addSystemApiPatterns(apiSecures).getSystemApiSecures();
			registry.addInterceptor(this.systemApiHandlerHandler.systemApiInterceptor(signSecures));
		}
	}




}
