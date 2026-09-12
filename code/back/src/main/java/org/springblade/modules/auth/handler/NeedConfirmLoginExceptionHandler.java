package org.springblade.modules.auth.handler;

import org.springblade.core.tool.support.Kv;
import org.springblade.modules.auth.constant.BladeOAuth2ErrorCode;
import org.springblade.modules.auth.exception.NeedConfirmLoginException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将互踢确认场景转换为 OAuth2 标准错误响应
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class NeedConfirmLoginExceptionHandler {

	@ExceptionHandler(NeedConfirmLoginException.class)
	public ResponseEntity<Kv> handleNeedConfirmLogin(NeedConfirmLoginException exception) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(Kv.create()
				.set("error", BladeOAuth2ErrorCode.NEED_CONFIRM_LOGIN)
				.set("error_description", exception.getMessage()));
	}
}
