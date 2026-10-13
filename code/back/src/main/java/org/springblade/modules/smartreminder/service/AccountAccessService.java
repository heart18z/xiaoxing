package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oauth2.handler.AuthorizationHandler;
import org.springblade.core.oauth2.provider.*;
import org.springblade.core.oauth2.service.*;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.jwt.JwtUtil;
import org.springblade.modules.auth.utils.OAuth2TokenIssueHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Email authentication uses the same client, tenant, status and single-login checks as password login. */
@Service
@RequiredArgsConstructor
public class AccountAccessService {
    private final JdbcTemplate jdbc;
    private final EmailVerificationService verification;
    private final OAuth2UserService users;
    private final OAuth2ClientService clients;
    private final AuthorizationHandler authorization;
    private final OAuth2TokenIssueHelper tokens;

    public Map<String,Object> account(String rawEmail) {
        String email=ContactIdentity.email(rawEmail);
        var rows=jdbc.queryForList("select u.id,u.account,u.email from blade_user u where u.tenant_id='000000' and u.app_unique_email=? and u.is_deleted=0 and u.status=1 and exists(select 1 from blade_role r where r.tenant_id=u.tenant_id and r.role_alias='app_user' and r.is_deleted=0 and find_in_set(r.id,u.role_id)>0)",email);
        if(rows.size()!=1)throw new ServiceException("邮箱未注册或账号不可用");
        return rows.get(0);
    }
    public void send(String email,String purpose,String ip) {
        account(email);
        verification.sendAction(email,ip,purpose);
    }
    @Transactional(rollbackFor=Exception.class,noRollbackFor=EmailVerificationService.InvalidCode.class)
    public Kv login(String email,String code) {
        var row=account(email);
        OAuth2Request request=OAuth2Request.create().buildArgs();
        // Never accept a tenant, grant, user type or user ID from the public JSON payload.
        request.setTenantId("000000");request.setUsername(row.get("account").toString());
        request.getParameterArgs().set("grant_type","password");
        request.getHeaderArgs().set("User-Type","web");
        var client=clients.loadByClientId(request.getClientId(),request);
        if(client==null || !"saber".equals(request.getClientId()) || !clients.validateClient(client,request.getClientId(),request.getClientSecret()) || !clients.validateGranter(client,"password"))throw new ServiceException("登录客户端无效");
        check(authorization.preValidation(request));
        verification.consumeAction(email,code,"LOGIN");
        var user=users.loadByUsername(request.getUsername(),request);
        if(user==null || !users.validateUser(user) || !user.getAuthorities().contains("app_user"))throw new ServiceException("账号不可用");
        // A confirmation challenge rolls back code consumption; confirmation can reuse that code.
        check(authorization.authValidation(user,request));
        Kv result=tokens.issueToken(user,request);
        authorization.authSuccessful(user,request);
        return result;
    }
    private void check(OAuth2Validation validation) {
        if(!validation.isSuccess())throw new ServiceException(validation.getMessage());
    }
    @Transactional(rollbackFor=Exception.class,noRollbackFor=EmailVerificationService.InvalidCode.class)
    public void reset(String email,String code,String password,String confirmation) {
        if(!Objects.equals(password,confirmation))throw new ServiceException("两次新密码不一致");
        var row=account(email);Long id=((Number)row.get("id")).longValue();
        jdbc.queryForList("select id from blade_user where id=? for update",id);
        verification.consumeAction(email,code,"RESET");
        if(jdbc.update("update blade_user set password=?,last_change_password_time=now(),update_time=now() where id=? and is_deleted=0 and status=1",DigestUtil.encrypt(password),id)!=1)throw new ServiceException("账号不可用");
        revoke(id);
    }
    @Transactional(rollbackFor=Exception.class)
    public void close(Long id,String password,boolean confirmed) {
        if(!confirmed)throw new ServiceException("请再次确认注销账号");
        var rows=jdbc.queryForList("select password from blade_user where id=? and tenant_id='000000' and is_deleted=0 and status=1 for update",id);
        if(rows.size()!=1 || !java.security.MessageDigest.isEqual(DigestUtil.encrypt(password).getBytes(java.nio.charset.StandardCharsets.UTF_8),rows.get(0).get("password").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ServiceException("密码不正确或账号不可用");
        if(jdbc.queryForObject("select count(*) from blade_app_chat_job where user_id=? and job_status in ('QUEUED','RUNNING') and created_at>?",Integer.class,id,new java.sql.Timestamp(System.currentTimeMillis()-900000))>0)throw new ServiceException("当前对话仍在处理，请完成或停止后再注销");
        // Keep shared event history for other participants; remove this account's identity and access.
        jdbc.update("update blade_smart_event_branch set branch_status='STOPPED',stop_time=now(),stop_reason='账号已注销',next_evaluate_time=null,evaluate_lock=0,evaluation_token=null,update_time=now() where branch_status='ACTIVE' and (recipient_user_id=? or event_id in(select id from blade_smart_event where creator_user_id=?))",id,id);
        jdbc.update("update blade_smart_event set event_status='STOPPED',stop_time=now(),stop_reason='账号已注销',update_time=now() where creator_user_id=? and event_status='ACTIVE'",id);
        jdbc.update("delete from blade_smart_push_device where user_id=?",id);
        jdbc.update("delete from blade_smart_push_delivery where user_id=?",id);
        jdbc.update("delete from blade_friendship where owner_user_id=? or friend_user_id=?",id,id);
        jdbc.update("delete from blade_friend_request where applicant_user_id=? or target_user_id=?",id,id);
        jdbc.update("delete from blade_smart_person_alias where owner_user_id=? or person_user_id=?",id,id);
        jdbc.update("delete from blade_smart_user_preference where user_id=?",id);
        jdbc.update("delete from blade_smart_chat_message where user_id=?",id);
        jdbc.update("delete from blade_smart_candidate where user_id=?",id);
        jdbc.update("update blade_user set is_deleted=1,status=0,email=null,phone=null,avatar='',name='已注销用户',real_name='已注销用户',password=?,update_time=now() where id=?",DigestUtil.encrypt(UUID.randomUUID().toString()),id);
        revoke(id);
    }
    private void revoke(Long id) {
        // Delete both user-level and client-level indices (deployments may use either).
        JwtUtil.removeAccessToken("000000",null,id.toString(),null);
        JwtUtil.removeRefreshToken("000000",null,id.toString(),null);
        for(String client:jdbc.queryForList("select client_id from blade_client",String.class)) {
            JwtUtil.removeAccessToken("000000",client,id.toString(),null);
            JwtUtil.removeRefreshToken("000000",client,id.toString(),null);
        }
    }
}
