package org.springblade.common.secure.systemapi;

import lombok.AllArgsConstructor;
import org.springblade.core.secure.registry.SecureRegistry;
import org.springblade.core.tool.utils.Func;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties("blade.secure")
@AllArgsConstructor
public class SystemApiSecureProperties {

	private final List<SystemApiSecure> systemApi = new ArrayList();

	public List<SystemApiSecure> getSystemApi() {
		return systemApi;
	}

	private final SecureRegistry secureRegistry;

	@Bean
	public SystemApiSecureRegistry systemApiSecureRegistry() {
		if (Func.isNotEmpty(systemApi)) {
			List<String> pathPatterns = systemApi.stream().map(SystemApiSecure::getPattern).filter(i->Func.isNotBlank(i)).collect(Collectors.toList());
			secureRegistry.excludePathPatterns(pathPatterns);
		}
		return new SystemApiSecureRegistry();
	}


}
