package org.springblade.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可动态追加请求头的 HttpServletRequest 包装类。
 */
public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {

	private final Map<String, String> headerMap = new HashMap<>();

	public HeaderMapRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	public void addHeader(String name, String value) {
		headerMap.put(name, value);
	}

	@Override
	public String getHeader(String name) {
		if (headerMap.containsKey(name)) {
			return headerMap.get(name);
		}
		return super.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		List<String> names = Collections.list(super.getHeaderNames());
		names.addAll(headerMap.keySet());
		return Collections.enumeration(names);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> values = Collections.list(super.getHeaders(name));
		if (headerMap.containsKey(name)) {
			values.add(headerMap.get(name));
		}
		return Collections.enumeration(values);
	}

}
