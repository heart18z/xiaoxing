package org.springblade.modules.smartreminder.controller;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.smartreminder.service.AppAccountService;
import org.springblade.modules.smartreminder.service.RegistrationMailService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("blade-smart/registration-mail")
public class RegistrationMailController {
    private final RegistrationMailService mail;
    private final AppAccountService accounts;
    @PostMapping("detail") public R<Object> detail(){AppRoleGuard.requireAdmin();return R.data(mail.detail());}
    @PostMapping("save") public R<Boolean> save(@RequestBody RegistrationMailService.Settings input){AppRoleGuard.requireAdmin();mail.save(input);return R.data(true);}
    @PostMapping("test") public R<Boolean> test(@RequestBody Map<String,String> input){
        AppRoleGuard.requireAdmin();accounts.limit("smtp-test:"+AuthUtil.getUserId(),5);
        mail.test(input.get("email"));return R.data(true);
    }
}
