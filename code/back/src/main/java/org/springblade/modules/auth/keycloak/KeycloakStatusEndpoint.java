package org.springblade.modules.auth.keycloak;

import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keycloak 状态查询（始终可用，供前端探测是否启用 SSO）
 */
@NonDS
@ApiSort(2)
@RestController
@RequiredArgsConstructor
@Tag(name = "Keycloak 单点登录", description = "Keycloak OIDC 对接")
public class KeycloakStatusEndpoint {

	private final KeycloakProperties keycloakProperties;

	@PostMapping("/oauth/keycloak/enabled")
	@Operation(summary = "是否启用 Keycloak")
	public R<Boolean> enabled() {
		return R.data(keycloakProperties.isEnable());
	}

}
