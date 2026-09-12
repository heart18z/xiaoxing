package org.springblade.modules.auth.keycloak;

import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Keycloak 单点登录端点
 */
@NonDS
@ApiSort(2)
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(value = "keycloak.enable", havingValue = "true")
@Tag(name = "Keycloak 单点登录", description = "Keycloak OIDC 对接")
public class KeycloakEndpoint {

	private final KeycloakService keycloakService;

	@PostMapping("/oauth/keycloak/login-url")
	@Operation(summary = "获取 Keycloak 授权地址")
	public R<String> loginUrl(@RequestParam(required = false) String state) {
		return R.data(keycloakService.buildAuthorizeUrl(state));
	}

	@GetMapping("/oauth/keycloak/login")
	@Operation(summary = "跳转 Keycloak 授权页")
	public void login(@RequestParam(required = false) String state, HttpServletResponse response) throws IOException {
		response.sendRedirect(keycloakService.buildAuthorizeUrl(state));
	}

	@PostMapping("/oauth/keycloak/logout-url")
	@Operation(summary = "获取 Keycloak 登出地址")
	public R<String> logoutUrl(@RequestParam(required = false) String redirectUri) {
		return R.data(keycloakService.buildLogoutUrl(redirectUri));
	}

}
