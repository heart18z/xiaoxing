package org.springblade.common.config;
import lombok.Data;
import org.dfyj.unifiedoauth.UnifiedOauthClient;
import org.dfyj.unifiedoauth.service.UnifiedOauthService;
import org.springblade.common.cache.ParamCache;
import org.springblade.core.tool.utils.Func;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("unified-oauth.client")
@Data
public class UnifiedOauthConfig {


	private String baseUrl;
	private String username;
	private String password;
	private String logOutUrl;
	private String systemId;
	private String systemName;
	private String loginUrl;



	@Bean
	public UnifiedOauthService initUnifiedOauthService() {
//		String systemParamId = ParamCache.getValue("system.id");
//		if (Func.isNotBlank(systemParamId)) {
//			systemId = systemParamId;
//		}
		return new UnifiedOauthClient(baseUrl,
			username, password,
			logOutUrl,
			systemId,
			systemName,
			loginUrl)
			.getUnifiedOauthService();

	}

}
