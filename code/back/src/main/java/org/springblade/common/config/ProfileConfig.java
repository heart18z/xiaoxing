package org.springblade.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileConfig {

	@Autowired
	private ApplicationContext context;

	public String getActiveProfile() {
		return context.getEnvironment().getActiveProfiles()[0];
	}
}

