package org.springblade.modules.smartreminder.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.smartreminder.service.AppAccountService;
import org.springblade.modules.smartreminder.support.AppRoleGuard;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Public registration accepts identity fields only, never roles, tenant IDs or user IDs. */
@RestController
@RequiredArgsConstructor
@RequestMapping("app/account")
public class AppAccountController {
    private final AppAccountService accounts;

    // Never pass a validation exception containing rejected password values to generic logs.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Boolean> invalid(MethodArgumentNotValidException error) {
        return R.fail(error.getBindingResult().getAllErrors().stream()
            .map(item -> item.getDefaultMessage()).findFirst().orElse("请检查输入内容"));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Boolean> unreadable() { return R.fail("请求格式有误，请检查输入内容"); }

    @PostMapping("register")
    public R<Boolean> register(@Valid @RequestBody Registration input, HttpServletRequest request) {
        accounts.limit("signup:" + request.getRemoteAddr(), 30);
        accounts.register(input);
        return R.data(true);
    }

    @PostMapping("suggest-account")
    public R<String> suggestAccount(HttpServletRequest request) {
        accounts.limit("suggest:" + request.getRemoteAddr(), 30);
        return R.data(accounts.suggestAccount());
    }

    @PostMapping("password")
    public R<Boolean> password(@Valid @RequestBody PasswordChange input) {
        AppRoleGuard.requireAppUser();
        Long id = AuthUtil.getUserId();
        accounts.limit("password:" + id, 8);
        accounts.changePassword(id, input);
        return R.data(true);
    }

    @Data
    public static class Registration {
        @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_-]{3,31}", message="账号须为4–32位字母、数字、下划线或短横线，首位为字母或数字")
        private String account;
        @NotBlank @Size(max=20, message="姓名最多20个字符") private String name;
        @ToString.Exclude @NotBlank @Size(min=8,max=64, message="密码须为8–64个字符") private String password;
        @Pattern(regexp="^$|^1[3-9][0-9]{9}$", message="请输入正确的11位手机号") private String phone;
        @NotBlank(message="请填写邮箱") @Email(message="请输入正确的邮箱") @Size(max=45, message="邮箱最多45个字符") private String email;
        @AssertTrue(message="请填写邮箱")
        public boolean isContactProvided() {
            return email != null && !email.isBlank();
        }
    }

    @Data
    public static class PasswordChange {
        @ToString.Exclude @NotBlank @Size(max=128) private String oldPassword;
        @ToString.Exclude @NotBlank @Size(min=8,max=64, message="密码须为8–64个字符") private String password;
        @ToString.Exclude @NotBlank @Size(min=8,max=64) private String confirmation;
    }
}
