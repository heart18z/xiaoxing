package org.springblade.common.rpc.apiplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Data
@ConfigurationProperties("api-platform")
public class ApiPlatformConfig {
	private String appKey;
	private String appSecret;
	private String baseUrl;
}
