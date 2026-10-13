package org.springblade.modules.smartreminder.controller;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestController @RequiredArgsConstructor @RequestMapping("app/account")
public class AccountAccessController {
    private final AccountAccessService access;
    private final AppAccountService accounts;
    @PostMapping("access-code") public R<Object> send(@Valid @RequestBody Send input,HttpServletRequest request) {
        accounts.limit("access-code:"+request.getRemoteAddr(),30);
        access.send(input.getEmail(),input.getPurpose(),request.getRemoteAddr());
        return R.data(java.util.Map.of("retryAfter",60,"expiresIn",600));
    }
    @PostMapping("email-login") public R<Object> login(@Valid @RequestBody Login input,HttpServletRequest request) {
        accounts.limit("email-login:"+request.getRemoteAddr(),60);
        return R.data(access.login(input.getEmail(),input.getCode()));
    }
    @PostMapping("reset-password") public R<Object> reset(@Valid @RequestBody Reset input,HttpServletRequest request) {
        accounts.limit("reset-password:"+request.getRemoteAddr(),30);
        access.reset(input.getEmail(),input.getCode(),input.getPassword(),input.getConfirmation());return R.data(true);
    }
    @PostMapping("close") public R<Object> close(@Valid @RequestBody Close input) {
        AppRoleGuard.requireAppUser();accounts.limit("close:"+AuthUtil.getUserId(),8);
        access.close(AuthUtil.getUserId(),input.getPassword(),input.isConfirmed());return R.data(true);
    }
    @Data public static class Send {
        @NotBlank @Email @Size(max=45) private String email;
        @NotBlank @Pattern(regexp="LOGIN|RESET") private String purpose;
    }
    @Data public static class Login {
        @NotBlank @Email @Size(max=45) private String email;
        @ToString.Exclude @NotBlank @Pattern(regexp="[0-9]{6}") private String code;
    }
    @Data @EqualsAndHashCode(callSuper=true) public static class Reset extends Login {
        @ToString.Exclude @NotBlank @Size(min=8,max=64) private String password;
        @ToString.Exclude @NotBlank @Size(min=8,max=64) private String confirmation;
    }
    @Data public static class Close {
        @ToString.Exclude @NotBlank @Size(max=128) private String password;
        private boolean confirmed;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) public R<Object> invalid() { return R.fail("请填写正确的邮箱、6位验证码及8–64位密码"); }
    @ExceptionHandler(HttpMessageNotReadableException.class) public R<Object> unreadable() { return R.fail("请求格式有误"); }
}
