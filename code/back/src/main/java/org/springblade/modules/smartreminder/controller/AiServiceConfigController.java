package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.AiConfigRequest;
import org.springblade.modules.smartreminder.service.AiConfigService;
import org.springblade.modules.smartreminder.service.NewApiClient;
import org.springblade.modules.smartreminder.service.NewApiClient.AiAnswer;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("blade-smart/ai-config")
public class AiServiceConfigController {

	private final AiConfigService configService;
	private final NewApiClient aiClient;

	@PostMapping("/detail")
	public R<Object> detail(@org.springframework.web.bind.annotation.RequestParam(defaultValue="LLM") String configType, @org.springframework.web.bind.annotation.RequestParam(required=false) Long id) {
		AppRoleGuard.requireAdmin();
		return R.data(configService.currentMasked(configType,id));
	}

	@PostMapping("/list")
	public R<Object> list() { AppRoleGuard.requireAdmin();return R.data(configService.choices(false)); }

	@PostMapping("/submit")
	public R<Object> submit(@RequestBody AiConfigRequest request) {
		AppRoleGuard.requireAdmin();
		return R.data(configService.save(request));
	}

	@PostMapping("/activate")
	public R<Object> activate(@org.springframework.web.bind.annotation.RequestParam String configType,@org.springframework.web.bind.annotation.RequestParam Long id) {
		AppRoleGuard.requireAdmin(); configService.activate(configType,id); return R.data(true);
	}
	@PostMapping("/default")
	public R<Object> setDefault(@org.springframework.web.bind.annotation.RequestParam String configType,@org.springframework.web.bind.annotation.RequestParam Long id) {
		AppRoleGuard.requireAdmin();configService.setDefault(configType,id);return R.data(true);
	}
	@PostMapping("/prompts")
	public R<Object> prompts() { AppRoleGuard.requireAdmin();return R.data(configService.prompts()); }
	@PostMapping("/prompts/save")
	public R<Object> savePrompts(@RequestBody Map<String,String> request) { AppRoleGuard.requireAdmin();configService.savePrompts(request);return R.data(true); }

	@PostMapping("/test")
	public R<Object> test(@org.springframework.web.bind.annotation.RequestParam(required=false) Long id) {
		AppRoleGuard.requireAdmin();
		AiAnswer answer = aiClient.chat(configService.testLlmConfig(id),"你是连接测试助手，只需简短回复。", "请回复：连接正常");
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("model", answer.model());
		result.put("content", answer.content());
		return R.data(result);
	}
}
