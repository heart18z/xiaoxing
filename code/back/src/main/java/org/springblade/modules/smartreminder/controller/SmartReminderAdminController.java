package org.springblade.modules.smartreminder.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.service.SmartReminderAdminService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("blade-smart/admin")
public class SmartReminderAdminController {

	private final SmartReminderAdminService adminService;

	@PostMapping("/dashboard")
	public R<Object> dashboard() {
		AppRoleGuard.requireAdmin();
		return R.data(adminService.dashboard());
	}

	@PostMapping("/events")
	public R<Object> events(@RequestParam(defaultValue = "1") int page,
							@RequestParam(defaultValue = "20") int size,
							@RequestParam(defaultValue = "") String keyword,
							@RequestParam(defaultValue = "") String status) {
		AppRoleGuard.requireAdmin();
		return R.data(adminService.events(page, size, keyword, status));
	}

	@PostMapping("/notifications")
	public R<Object> notifications(@RequestParam(defaultValue = "1") int page,
									@RequestParam(defaultValue = "20") int size,
									@RequestParam(defaultValue = "") String keyword,
									@RequestParam(defaultValue = "") String readStatus) {
		AppRoleGuard.requireAdmin();
		return R.data(adminService.notifications(page, size, keyword, readStatus));
	}

	@PostMapping("/evaluations")
	public R<Object> evaluations(@RequestParam(defaultValue = "1") int page,
								  @RequestParam(defaultValue = "20") int size,
								  @RequestParam(defaultValue = "") String action) {
		AppRoleGuard.requireAdmin();
		return R.data(adminService.evaluations(page, size, action));
	}
}
