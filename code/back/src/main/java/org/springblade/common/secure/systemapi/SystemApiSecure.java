package org.springblade.common.secure.systemapi;

import lombok.NoArgsConstructor;
import org.springblade.core.secure.provider.HttpMethod;

@NoArgsConstructor
public class SystemApiSecure {

	private HttpMethod method;
	private String pattern;

	public SystemApiSecure(HttpMethod method, String pattern) {
		this.method = method;
		this.pattern = pattern;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
