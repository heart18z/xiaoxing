package org.springblade.modules.unifiedoauth.constant;

import org.springblade.common.constant.CommonConstant;

public interface UnifiedOauthConstant {
	/**
	 * 登录类型 ；
	 *  1 本系统账号登录
	 *  2 单点登录
	 *  3 选择登录
	 */
	static String ACCOUNT_LOGIN_TYPE = "user_loginMethod_account";

	static String SSO_LOGIN_TYPE = "user_loginMethod_auth";

	static String SELECT_LOGIN_TYPE = "user_loginMethod_both";

}
