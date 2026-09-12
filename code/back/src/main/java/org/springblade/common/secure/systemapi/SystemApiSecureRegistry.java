package org.springblade.common.secure.systemapi;

import org.springblade.core.secure.provider.HttpMethod;

import java.util.ArrayList;
import java.util.List;


public class SystemApiSecureRegistry {

	private final List<SystemApiSecure> systemApiSecures = new ArrayList();

	private final List<String> excludePatterns = new ArrayList();

	public SystemApiSecureRegistry addSystemApiPattern(HttpMethod method, String pattern) {
		this.systemApiSecures.add(new SystemApiSecure(method, pattern));
		return this;
	}

	public SystemApiSecureRegistry addSystemApiPatterns(List<SystemApiSecure> basicSecures) {
		this.systemApiSecures.addAll(basicSecures);
		return this;
	}

	public List<SystemApiSecure> getSystemApiSecures() {
		return this.systemApiSecures;
	}

}
