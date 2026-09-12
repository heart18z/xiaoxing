package org.springblade.modules.smartreminder.push;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("app/push")
public class PushController {
    private final PushDeviceService devices;
    @PostMapping("register")
    public R<Object> register(@RequestBody PushDeviceService.Registration request) {
        AppRoleGuard.requireAppUser(); return R.data(devices.register(AuthUtil.getUserId(),request));
    }
    @PostMapping("revoke")
    public R<Object> revoke(@RequestBody PushDeviceService.Revocation request) {
        devices.revoke(request); return R.success("设备推送绑定已解除");
    }
}
