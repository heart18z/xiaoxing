package org.springblade.modules.auth.keycloak;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Keycloak OIDC 配置
 */
@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

	/**
	 * 是否启用 Keycloak 单点登录
	 */
	private boolean enable = false;

	private String clientId;

	private String clientSecret;

	private String scope = "openid profile email";

	private String authEndpoint;

	private String tokenEndpoint;

	private String discoveryEndpoint;

	private String redirectUri;

	private String logoutEndpoint;

}
