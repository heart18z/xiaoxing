package org.springblade.modules.auth.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Keycloak OIDC 交互服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "keycloak.enable", havingValue = "true")
public class KeycloakService {

	private final KeycloakProperties properties;

	/**
	 * 构建授权跳转地址
	 */
	public String buildAuthorizeUrl(String state) {
		String tenantId = Func.toStr(state, TokenUtil.DEFAULT_TENANT_ID);
		String nonce = UUID.randomUUID().toString().replace("-", "");
		StringBuilder url = new StringBuilder(properties.getAuthEndpoint());
		url.append(properties.getAuthEndpoint().contains("?") ? "&" : "?");
		url.append("client_id=").append(encode(properties.getClientId()));
		url.append("&redirect_uri=").append(encode(properties.getRedirectUri()));
		url.append("&response_type=code");
		url.append("&scope=").append(encode(properties.getScope()));
		url.append("&state=").append(encode(tenantId));
		url.append("&nonce=").append(encode(nonce));
		return url.toString();
	}

	/**
	 * 使用授权码换取令牌并解析用户名
	 */
	public String resolveUsername(String code) {
		return resolveUserProfile(code).getUsername();
	}

	/**
	 * 使用授权码换取令牌并解析用户资料
	 */
	public KeycloakUserProfile resolveUserProfile(String code) {
		String response = HttpRequest.post(properties.getTokenEndpoint())
			.formBuilder()
			.add("grant_type", "authorization_code")
			.add("client_id", properties.getClientId())
			.add("client_secret", properties.getClientSecret())
			.add("code", code)
			.add("redirect_uri", properties.getRedirectUri())
			.execute()
			.onSuccess(spec -> spec.asString());
		if (Func.isBlank(response)) {
			throw new ServiceException("Keycloak 令牌交换失败");
		}
		Map<String, Object> tokenMap = JsonUtil.parse(response, Map.class);
		if (tokenMap == null || tokenMap.containsKey("error")) {
			log.error("Keycloak token error: {}", response);
			throw new ServiceException("Keycloak 认证失败：" + tokenMap);
		}
		String idToken = Func.toStr(tokenMap.get("id_token"));
		if (Func.isNotBlank(idToken)) {
			return parseUserProfileFromJwt(idToken);
		}
		String accessToken = Func.toStr(tokenMap.get("access_token"));
		if (Func.isNotBlank(accessToken)) {
			return parseUserProfileFromJwt(accessToken);
		}
		throw new ServiceException("Keycloak 响应中未包含有效令牌");
	}

	/**
	 * 构建登出地址
	 */
	public String buildLogoutUrl(String redirectUri) {
		String target = Func.toStr(redirectUri, properties.getRedirectUri());
		return properties.getLogoutEndpoint()
			+ (properties.getLogoutEndpoint().contains("?") ? "&" : "?")
			+ "client_id=" + encode(properties.getClientId())
			+ "&post_logout_redirect_uri=" + encode(target);
	}

	private KeycloakUserProfile parseUserProfileFromJwt(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length < 2) {
			throw new ServiceException("Keycloak 令牌格式无效");
		}
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		Map<String, Object> claims = JsonUtil.parse(payload, Map.class);
		if (claims == null) {
			throw new ServiceException("Keycloak 令牌解析失败");
		}
		String username = Func.toStr(claims.get("preferred_username"));
		if (Func.isBlank(username)) {
			username = Func.toStr(claims.get("email"));
		}
		if (Func.isBlank(username)) {
			username = Func.toStr(claims.get("sub"));
		}
		if (StringUtil.isBlank(username)) {
			throw new ServiceException("Keycloak 用户标识为空");
		}
		KeycloakUserProfile profile = new KeycloakUserProfile();
		profile.setUsername(username);
		profile.setEmail(Func.toStr(claims.get("email")));
		String name = Func.toStr(claims.get("name"));
		if (Func.isBlank(name)) {
			String givenName = Func.toStr(claims.get("given_name"));
			String familyName = Func.toStr(claims.get("family_name"));
			name = (Func.toStr(familyName) + Func.toStr(givenName)).trim();
		}
		if (Func.isBlank(name)) {
			name = username;
		}
		profile.setName(name);
		return profile;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

}
