package org.springblade.common.secure.systemapi;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@Order(2)
@AutoConfiguration(
	before = {SystemApiSecureConfiguration.class}
)
public class SystemApiRegistryConfiguration {

	@Bean
	@ConditionalOnMissingBean({ISystemApiHandler.class})
	public ISystemApiHandler secureHandler() {
		return new SystemApiHandlerHandler();
	}
}
