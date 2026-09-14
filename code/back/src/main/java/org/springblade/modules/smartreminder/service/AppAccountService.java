package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.modules.smartreminder.controller.AppAccountController.Registration;
import org.springblade.modules.smartreminder.controller.AppAccountController.PasswordChange;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppAccountService {
    private final JdbcTemplate jdbc;
    private final IUserService users;
    private static final String TENANT = "000000";
    private final Map<String, long[]> attempts = new HashMap<>();

    // Bounded per-instance abuse protection; deployments should also rate-limit at the gateway.
    public synchronized void limit(String key, int maximum) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now - entry.getValue()[0] >= 600_000);
        if (!attempts.containsKey(key) && attempts.size() >= 2048) throw new ServiceException("请求较多，请稍后再试");
        long[] counter = attempts.computeIfAbsent(key, ignored -> new long[]{now, 0});
        if (++counter[1] > maximum) throw new ServiceException("操作过于频繁，请10分钟后再试");
    }

    @Transactional(rollbackFor=Exception.class)
    public void register(Registration input) {
        // Fail closed until the additive migration is installed. The database constraint
        // arbitrates concurrent signups AND administrative account creation.
        Integer indexes = jdbc.queryForObject("select count(*) from information_schema.statistics where table_schema=database() and table_name='blade_user' and index_name='uk_app_account_tenant' and non_unique=0", Integer.class);
        if (indexes == null || indexes < 1) throw new ServiceException("注册服务尚未就绪，请联系管理员");
        if (jdbc.queryForObject("select count(*) from blade_user where tenant_id=? and account=?", Integer.class, TENANT, input.getAccount()) > 0)
            throw new ServiceException("账号已存在，请更换人员号");
        List<Long> roles = jdbc.queryForList("select id from blade_role where tenant_id=? and role_alias='app_user' and is_deleted=0", Long.class, TENANT);
        if (roles.size() != 1) throw new ServiceException("APP使用人员角色尚未配置，请联系管理员");
        User user = new User();
        user.setTenantId(TENANT); user.setAccount(input.getAccount());
        user.setName(input.getName().trim()); user.setRealName(input.getName().trim());
        user.setPassword(input.getPassword()); user.setPhone(input.getPhone()); user.setEmail(input.getEmail());
        user.setRoleId(String.valueOf(roles.get(0))); user.setUserType("1");
        user.setStatus(1); user.setIsDeleted(0); user.setCreateTime(new Date());
        try { if (!users.submit(user)) throw new ServiceException("注册失败，请稍后重试"); }
        catch (DuplicateKeyException duplicate) { throw new ServiceException("账号已存在，请更换人员号"); }
    }

    @Transactional(rollbackFor=Exception.class)
    public void changePassword(Long id, PasswordChange input) {
        if (!input.getPassword().equals(input.getConfirmation())) throw new ServiceException("两次新密码不一致");
        // Use the same hashing function as BladePasswordHandler (not the legacy hex-only updater).
        int changed = jdbc.update("update blade_user set password=?,last_change_password_time=now(),update_time=now() where id=? and password=? and is_deleted=0 and status=1",
            DigestUtil.encrypt(input.getPassword()), id, DigestUtil.encrypt(input.getOldPassword()));
        if (changed != 1) throw new ServiceException("原密码不正确或账号不可用");
    }
}
