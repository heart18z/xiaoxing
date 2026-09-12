package org.springblade.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties("maitalk-config")
public class MaitalkConfig {
	/**
	 * 外部 maitalk 服务基础地址，例如：http://eastview.top:40021
	 */
	private String baseUrl;
}

