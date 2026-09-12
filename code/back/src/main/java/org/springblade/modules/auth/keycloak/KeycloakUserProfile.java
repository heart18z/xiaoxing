package org.springblade.modules.auth.keycloak;

import lombok.Data;

/**
 * Keycloak 解析出的用户资料
 */
@Data
public class KeycloakUserProfile {

	/**
	 * 登录账号（preferred_username / email / sub）
	 */
	private String username;

	/**
	 * 显示名称
	 */
	private String name;

	/**
	 * 邮箱
	 */
	private String email;

}
