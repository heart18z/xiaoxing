package org.springblade.common.secure.systemapi;

import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public interface ISystemApiHandler {

	HandlerInterceptor systemApiInterceptor(List<SystemApiSecure> systemApiSecures);
}
