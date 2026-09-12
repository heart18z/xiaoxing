package org.springblade.common.secure.systemapi;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
@Component
public class SystemApiHandlerHandler implements ISystemApiHandler {


	public HandlerInterceptor systemApiInterceptor(List<SystemApiSecure> systemApiSecures) {
		return new SystemApiInterceptor(systemApiSecures);
	}


}
