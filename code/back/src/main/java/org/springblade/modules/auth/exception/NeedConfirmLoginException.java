package org.springblade.modules.auth.exception;

import org.springblade.modules.auth.constant.BladeOAuth2ErrorCode;

/**
 * 账号已在其他终端登录，需用户确认后继续
 */
public class NeedConfirmLoginException extends RuntimeException {

	public NeedConfirmLoginException() {
		super(BladeOAuth2ErrorCode.NEED_CONFIRM_LOGIN_MESSAGE);
	}

	public NeedConfirmLoginException(String message) {
		super(message);
	}

	public String getError() {
		return BladeOAuth2ErrorCode.NEED_CONFIRM_LOGIN;
	}
}
