package org.springblade.modules.auth.constant;

/**
 * 项目扩展 OAuth2 错误码
 */
public interface BladeOAuth2ErrorCode {

	String NEED_CONFIRM_LOGIN = "need_confirm_login";

	String NEED_CONFIRM_LOGIN_MESSAGE = "您的账号已在其他地址/浏览器登录，继续登录将会导致其他地址/浏览器的登录退出，是否继续！";
}
