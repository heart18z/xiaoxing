package org.springblade.modules.smartreminder.service;

import org.springblade.core.log.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Service
public class EmailVerificationService {
    private final JdbcTemplate jdbc;
    private final RegistrationMailService mail;
    private final TransactionTemplate transactions;
    private final byte[] key;
    private final SecureRandom random=new SecureRandom();
    public EmailVerificationService(JdbcTemplate jdbc,RegistrationMailService mail,PlatformTransactionManager manager,@Value("${blade.token.sign-key}") String key) {
        this.jdbc=jdbc;this.mail=mail;this.transactions=new TransactionTemplate(manager);this.key=key.getBytes(StandardCharsets.UTF_8);
    }
    public Map<String,Object> availability(String field,String value) {
        if(!Set.of("email","phone").contains(Objects.toString(field,"")))throw new ServiceException("仅支持检测邮箱或手机号");
        String normalized="email".equals(field)?ContactIdentity.email(value):ContactIdentity.phone(value);
        if(normalized.isEmpty())return Map.of("available",true,"value",normalized);
        String column="email".equals(field)?"app_unique_email":"app_unique_phone";
        boolean available=jdbc.queryForObject("select count(*) from blade_user where tenant_id='000000' and "+column+"=?",Integer.class,normalized)==0;
        return Map.of("available",available,"value",normalized);
    }
    public void send(String rawEmail,String ip) {
        String email=ContactIdentity.email(rawEmail);mail.requireEnabled();
        if(!Boolean.TRUE.equals(availability("email",email).get("available")))throw new ServiceException("邮箱已被使用，请更换邮箱");
        sendScoped(email,ip,"REGISTER");
    }
    public void sendAction(String rawEmail,String ip,String purpose) {
        action(purpose);
        String email=ContactIdentity.email(rawEmail);mail.requireEnabled();
        sendScoped(email,ip,purpose);
    }
    private static void action(String purpose) {
        if(!Set.of("LOGIN","RESET").contains(purpose))throw new ServiceException("验证码用途无效");
    }
    private void sendScoped(String email,String ip,String purpose) {
        String table="REGISTER".equals(purpose)?"blade_registration_code":"blade_account_code";
        String identity="REGISTER".equals(purpose)?email:purpose+":"+email;
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000)),nonce=UUID.randomUUID().toString();
        long now=System.currentTimeMillis();
        transactions.executeWithoutResult(status->{
            // Fixed order serializes sends across instances, then applies mailbox and network limits.
            limit("global",now,3600_000,300);
            limit("ip:"+ip,now,3600_000,30);
            limit("email-hour:"+email,now,3600_000,5);
            limit("email-minute:"+email,now,60_000,1);
            jdbc.update("delete from "+table+" where expires_at<?",now-86400_000);
            jdbc.update("insert into "+table+"(email,nonce,code_hash,expires_at,attempts,delivered,used) values(?,?,?,?,0,0,0) on duplicate key update nonce=values(nonce),code_hash=values(code_hash),expires_at=values(expires_at),attempts=0,delivered=0,used=0",identity,nonce,hash(identity+":"+nonce+":"+code),now+600_000);
        });
        try {
            if("REGISTER".equals(purpose))mail.sendCode(email,code); else mail.sendActionCode(email,code,purpose);
            jdbc.update("update "+table+" set delivered=1 where email=? and nonce=?",identity,nonce);
        } catch(RuntimeException error) {
            jdbc.update("delete from "+table+" where email=? and nonce=?",identity,nonce);
            throw error;
        }
    }
    private void limit(String name,long now,long duration,int maximum) {
        String id=hash("rate:"+name);
        jdbc.update("insert into blade_registration_rate(bucket,started_at,attempts) values(?,?,0) on duplicate key update bucket=bucket",id,now);
        var row=jdbc.queryForMap("select started_at,attempts from blade_registration_rate where bucket=? for update",id);
        long start=((Number)row.get("started_at")).longValue();int attempts=((Number)row.get("attempts")).intValue();
        if(now-start>=duration){start=now;attempts=0;}
        if(attempts>=maximum)throw new ServiceException(duration==60_000?"请等待60秒后再发送验证码":"验证码发送过于频繁，请一小时后再试");
        jdbc.update("update blade_registration_rate set started_at=?,attempts=? where bucket=?",start,attempts+1,id);
        // Retain recent buckets across app restarts, discard expired ones to bound storage.
        jdbc.update("delete from blade_registration_rate where started_at<?",now-86400_000);
    }
    /** Must run inside the registration transaction. Invalid attempts commit; successful
     * consumption rolls back if inserting the account fails, so the user can correct it. */
    public void consume(String email,String code) { consumeScoped(email,code,"blade_registration_code"); }
    public void consumeAction(String email,String code,String purpose) {
        action(purpose);consumeScoped(purpose+":"+ContactIdentity.email(email),code,"blade_account_code");
    }
    private void consumeScoped(String email,String code,String table) {
        if(code==null || !code.matches("[0-9]{6}"))throw new InvalidCode("请输入6位邮箱验证码");
        var rows=jdbc.queryForList("select * from "+table+" where email=? for update",email);
        if(rows.isEmpty())throw new InvalidCode("请先获取当前邮箱的验证码");
        var row=rows.get(0);
        if(((Number)row.get("used")).intValue()!=0 || ((Number)row.get("delivered")).intValue()!=1 || ((Number)row.get("expires_at")).longValue()<=System.currentTimeMillis())throw new InvalidCode("验证码已失效，请重新获取");
        int attempts=((Number)row.get("attempts")).intValue();
        if(attempts>=5)throw new InvalidCode("验证码错误次数过多，请重新获取");
        if(!MessageDigest.isEqual(hash(email+":"+row.get("nonce")+":"+code).getBytes(StandardCharsets.US_ASCII),((String)row.get("code_hash")).getBytes(StandardCharsets.US_ASCII))) {
            jdbc.update("update "+table+" set attempts=attempts+1 where email=?",email);
            throw new InvalidCode(attempts==4?"验证码错误次数过多，请重新获取":"验证码不正确，请检查邮箱");
        }
        jdbc.update("update "+table+" set used=1 where email=?",email);
    }
    private String hash(String text) {
        try { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception error){throw new IllegalStateException("验证码服务初始化失败");}
    }
    public static class InvalidCode extends ServiceException { public InvalidCode(String message){super(message);} }
}
