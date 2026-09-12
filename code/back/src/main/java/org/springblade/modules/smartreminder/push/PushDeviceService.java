package org.springblade.modules.smartreminder.push;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PushDeviceService {
    public record Registration(String installationId,String installationSecret,String token,String environment,String bundleId,String language,String appVersion) {}
    public record Revocation(String installationId,String installationSecret,String bindingId) {}
    private final JdbcTemplate jdbc;
    private final SecretCodec secrets;
    private final PushProperties properties;
    private final ApnsClient apns;
    public PushDeviceService(JdbcTemplate jdbc,SecretCodec secrets,PushProperties properties,ApnsClient apns) {this.jdbc=jdbc;this.secrets=secrets;this.properties=properties;this.apns=apns;}
    static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void validateProof(String id,String secret) {
        if(id==null || !id.matches("[a-fA-F0-9-]{36}") || secret==null || !secret.matches("[a-fA-F0-9]{64}")) throw new ServiceException("无效的设备凭据");
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> register(long userId, Registration request) {
        validateProof(request.installationId(),request.installationSecret());
        if(!properties.getBundleId().equals(request.bundleId()) || !Set.of("production","sandbox").contains(Objects.toString(request.environment(),""))) throw new ServiceException("应用标识或推送环境不匹配");
        String token=Objects.toString(request.token(),"").toLowerCase(Locale.ROOT);
        if(!token.matches("[a-f0-9]{32,512}") || token.length()%2!=0) throw new ServiceException("无效的推送设备标识");
        var rows=jdbc.queryForList("select * from blade_smart_push_device where id=? for update",request.installationId());
        String proof=hash(request.installationSecret()), tokenHash=hash(token);
        if(!rows.isEmpty() && !MessageDigest.isEqual(proof.getBytes(StandardCharsets.US_ASCII),rows.get(0).get("secret_hash").toString().getBytes(StandardCharsets.US_ASCII))) throw new ServiceException("设备凭据不匹配");
        // Reinstall/token reuse: revoke the obsolete installation without moving its queued notifications.
        jdbc.update("update blade_smart_push_device set enabled=0,token_hash=null,device_token='' where environment=? and token_hash=? and id<>?",request.environment(),tokenHash,request.installationId());
        String binding=UUID.randomUUID().toString();
        if(!rows.isEmpty()) {
            var old=rows.get(0);
            if(((Number)old.get("user_id")).longValue()==userId && tokenHash.equals(old.get("token_hash")) && request.environment().equals(old.get("environment")) && ((Number)old.get("enabled")).intValue()==1) binding=old.get("binding_id").toString();
        }
        String language="en-us".equalsIgnoreCase(request.language())?"en-us":"zh-cn";
        String version=Objects.toString(request.appVersion(),""); if(version.length()>40) version=version.substring(0,40);
        if(rows.isEmpty()) jdbc.update("insert into blade_smart_push_device(id,user_id,secret_hash,token_hash,device_token,environment,binding_id,enabled,language,app_version,updated_at) values(?,?,?,?,?,?,?,1,?,?,now())",request.installationId(),userId,proof,tokenHash,secrets.encrypt(token),request.environment(),binding,language,version);
        else jdbc.update("update blade_smart_push_device set user_id=?,token_hash=?,device_token=?,environment=?,binding_id=?,enabled=1,language=?,app_version=?,updated_at=now() where id=?",userId,tokenHash,secrets.encrypt(token),request.environment(),binding,language,version,request.installationId());
        return Map.of("bindingId",binding,"userId",String.valueOf(userId),"backendReady",apns.ready(request.environment()));
    }
    /** Installation-secret proof permits logout retries after the OAuth session expires.
     * Binding ID makes delayed revocation harmless after another account signs in. */
    @Transactional(rollbackFor=Exception.class)
    public void revoke(Revocation request) {
        validateProof(request.installationId(),request.installationSecret());
        if(request.bindingId()==null || !request.bindingId().matches("[a-fA-F0-9-]{36}")) throw new ServiceException("无效的设备绑定");
        jdbc.update("update blade_smart_push_device set enabled=0,token_hash=null,device_token='',updated_at=now() where id=? and secret_hash=? and binding_id=?",request.installationId(),hash(request.installationSecret()),request.bindingId());
    }
}
