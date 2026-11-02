package org.springblade.modules.smartreminder.push;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("app/push/live")
public class LiveActivityController {
    private final LiveActivityService live;
    @PostMapping("snapshot")
    public R<Object> snapshot() { AppRoleGuard.requireAppUser();return R.data(live.snapshot(AuthUtil.getUserId(),Instant.now())); }
    @PostMapping("token")
    public R<Object> token(@RequestBody LiveActivityService.TokenRequest request) { live.token(request);return R.success("已同步活动状态"); }
}
