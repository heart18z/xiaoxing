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

    /** Persisted daily counter: suggestions are distinct across app instances/restarts.
     * The blade_user unique index remains the final arbiter if a user edits an account. */
    @Transactional(rollbackFor=Exception.class)
    public String suggestAccount() {
        String day = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        jdbc.update("insert into blade_app_account_sequence(account_day,next_value) values(?,0) on duplicate key update account_day=account_day", day);
        Long value = jdbc.queryForObject("select next_value from blade_app_account_sequence where account_day=? for update", Long.class, day);
        for (int i=0;i<1000;i++) {
            if (value == null || value >= 999999999L) throw new ServiceException("暂时无法生成账号，请手动填写");
            String account = day + String.format(Locale.ROOT, "%02d", ++value);
            if (jdbc.queryForObject("select count(*) from blade_user where tenant_id=? and account=?", Integer.class, TENANT, account) == 0) {
                jdbc.update("update blade_app_account_sequence set next_value=? where account_day=?", value, day);
                return account;
            }
        }
        throw new ServiceException("暂时无法生成账号，请手动填写");
    }

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
        input.setPhone(Objects.toString(input.getPhone(),"").trim());
        input.setEmail(Objects.toString(input.getEmail(),"").trim().toLowerCase(Locale.ROOT));
        if (!input.isContactProvided()) throw new ServiceException("请填写邮箱");
        // Fail closed until the additive migration is installed. The database constraint
        // arbitrates concurrent signups AND administrative account creation.
        Integer indexes = jdbc.queryForObject("select count(*) from information_schema.statistics where table_schema=database() and table_name='blade_user' and index_name='uk_app_account_tenant' and non_unique=0", Integer.class);
        if (indexes == null || indexes < 1) throw new ServiceException("注册服务尚未就绪，请联系管理员");
        Integer contactIndexes=jdbc.queryForObject("select count(distinct index_name) from information_schema.statistics where table_schema=database() and table_name='blade_user' and index_name in ('uk_app_phone_tenant','uk_app_email_tenant') and non_unique=0",Integer.class);
        if (contactIndexes == null || contactIndexes != 2) throw new ServiceException("联系方式唯一性保护尚未安装，请联系管理员");
        if (jdbc.queryForObject("select count(*) from blade_user where tenant_id=? and account=?", Integer.class, TENANT, input.getAccount()) > 0)
            throw new ServiceException("账号已存在，请更换账号");
        if (!input.getPhone().isEmpty() && jdbc.queryForObject("select count(*) from blade_user where tenant_id=? and is_deleted=0 and trim(phone)=?",Integer.class,TENANT,input.getPhone())>0)
            throw new ServiceException("手机号已被使用，请更换手机号");
        if (!input.getEmail().isEmpty() && jdbc.queryForObject("select count(*) from blade_user where tenant_id=? and is_deleted=0 and lower(trim(email))=?",Integer.class,TENANT,input.getEmail())>0)
            throw new ServiceException("邮箱已被使用，请更换邮箱");
        List<Long> roles = jdbc.queryForList("select id from blade_role where tenant_id=? and role_alias='app_user' and is_deleted=0", Long.class, TENANT);
        if (roles.size() != 1) throw new ServiceException("APP使用人员角色尚未配置，请联系管理员");
        User user = new User();
        user.setTenantId(TENANT); user.setAccount(input.getAccount());
        user.setAvatar("/avatars/user/B" + java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 14) + ".png");
        user.setName(input.getName().trim()); user.setRealName(input.getName().trim());
        user.setPassword(input.getPassword()); user.setPhone(input.getPhone()); user.setEmail(input.getEmail());
        user.setRoleId(String.valueOf(roles.get(0))); user.setUserType("1");
        user.setStatus(1); user.setIsDeleted(0); user.setCreateTime(new Date());
        try { if (!users.submit(user)) throw new ServiceException("注册失败，请稍后重试"); }
        catch (DuplicateKeyException duplicate) { throw new ServiceException("账号、手机号或邮箱已被使用，请检查后重试"); }
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
