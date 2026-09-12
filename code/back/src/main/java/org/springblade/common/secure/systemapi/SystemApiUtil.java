package org.springblade.common.secure.systemapi;

import org.springblade.common.secure.domain.ApiUserInfo;
import org.springblade.core.secure.exception.SecureException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 系统开放 API 请求头解析工具。
 */
public final class SystemApiUtil {

	public static final String USER_HEADER = "bladeSystemApiUser";

	private static final String USER_HEADER_PREFIX = "userInfo ";

	private SystemApiUtil() {
	}

	/**
	 * 从请求头解析调用方用户信息。
	 */
	public static ApiUserInfo parseUserInfo(HttpServletRequest request) {
		String header = request.getHeader(USER_HEADER);
		if (Func.isBlank(header)) {
			throw new SecureException("缺少请求头: " + USER_HEADER);
		}
		header = header.trim().replace("userInfo%20", USER_HEADER_PREFIX);
		if (!header.startsWith(USER_HEADER_PREFIX)) {
			throw new SecureException("请求头 " + USER_HEADER + " 格式不正确，应为: userInfo {base64-json}");
		}
		String encoded = header.substring(USER_HEADER_PREFIX.length()).trim();
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException ex) {
			throw new SecureException("客户端用户信息 Base64 解析失败");
		}
		String json = new String(decoded, StandardCharsets.UTF_8);
		ApiUserInfo userInfo = JsonUtil.parse(json, ApiUserInfo.class);
		if (userInfo == null || Func.isBlank(userInfo.getUserAccount())) {
			throw new SecureException("客户端用户信息不合法");
		}
		return userInfo;
	}

	/**
	 * 兼容旧代码：从当前线程请求中解析用户信息。
	 */
	public static ApiUserInfo userInfoAndDecodeHeader() {
		HttpServletRequest request = WebUtil.getRequest();
		if (request == null) {
			throw new SecureException("无法获取当前 HTTP 请求");
		}
		return parseUserInfo(request);
	}

}
