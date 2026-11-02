package org.springblade.modules.smartreminder.push;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class LiveActivityService {
    public record TokenRequest(String installationId,String installationSecret,String bindingId,String owner,
        String kind,String token,String activityKey,Boolean enabled) {}
    private final JdbcTemplate jdbc; private final SecretCodec secrets; private final LiveActivityPlans plans;
    public LiveActivityService(JdbcTemplate jdbc,SecretCodec secrets,LiveActivityPlans plans) {this.jdbc=jdbc;this.secrets=secrets;this.plans=plans;}

    public Map<String,Object> snapshot(long userId,Instant now) {
        return Map.of("owner",Long.toString(userId),"serverTime",now.getEpochSecond(),
            "items",plans.candidates(userId,now).stream().map(p->p.content("zh-cn",now)).toList());
    }
    /** Native ActivityKit can wake us after OAuth expires. A current installation proof replaces OAuth ONLY here. */
    @Transactional(rollbackFor=Exception.class)
    public void token(TokenRequest r) {
        if(r==null || r.installationId()==null || !r.installationId().matches("[a-fA-F0-9-]{36}") ||
            r.installationSecret()==null || !r.installationSecret().matches("[a-fA-F0-9]{64}")) throw new ServiceException("无效的设备凭据");
        var devices=jdbc.queryForList("""
            select d.* from blade_smart_push_device d join blade_user u on u.id=d.user_id and u.is_deleted=0 and u.status=1
            where d.id=? and d.binding_id=? and d.enabled=1 for update
            """,r.installationId(),r.bindingId());
        if(devices.isEmpty()) throw new ServiceException("设备绑定已失效");
        var device=devices.get(0);
        if(!Objects.equals(r.owner(),device.get("user_id").toString()) || !MessageDigest.isEqual(
            PushDeviceService.hash(r.installationSecret()).getBytes(StandardCharsets.US_ASCII),
            device.get("secret_hash").toString().getBytes(StandardCharsets.US_ASCII))) throw new ServiceException("设备凭据不匹配");
        String kind=Objects.toString(r.kind(),"");
        if(!Set.of("START","UPDATE","DISMISSED","ENABLED").contains(kind)) throw new ServiceException("不支持的活动状态");
        String token=Objects.toString(r.token(),"").toLowerCase(Locale.ROOT);
        if(Set.of("START","UPDATE").contains(kind) && (!token.matches("[a-f0-9]{32,1024}") || token.length()%2!=0)) throw new ServiceException("无效的活动标识");
        if("START".equals(kind) || "ENABLED".equals(kind)) {
            var rows=jdbc.queryForList("select * from blade_smart_live_device where device_id=?",r.installationId());
            boolean same=!rows.isEmpty() && r.bindingId().equals(rows.get(0).get("binding_id"));
            String encrypted="START".equals(kind)?secrets.encrypt(token):same?Objects.toString(rows.get(0).get("start_token"),""):"";
            int enabled="START".equals(kind) && same ? ((Number)rows.get(0).get("enabled")).intValue() : Boolean.FALSE.equals(r.enabled())?0:1;
            if(rows.isEmpty()) jdbc.update("insert into blade_smart_live_device(device_id,binding_id,start_token,enabled,updated_at) values(?,?,?,?,now())",r.installationId(),r.bindingId(),encrypted,enabled);
            else jdbc.update("update blade_smart_live_device set binding_id=?,start_token=?,enabled=?,updated_at=now() where device_id=?",r.bindingId(),encrypted,enabled,r.installationId());
        } else {
            if(r.activityKey()==null || !r.activityKey().matches("[a-f0-9]{40}")) throw new ServiceException("无效的活动窗口");
            // A callback can only attach to a window issued by this server for this device + binding + user.
            var rows=jdbc.queryForList("select id,status from blade_smart_live_activity where device_id=? and binding_id=? and user_id=? and activity_key=?",r.installationId(),r.bindingId(),device.get("user_id"),r.activityKey());
            if(rows.isEmpty()) throw new ServiceException("活动不存在或已失效");
            var row=rows.get(0);
            if("DISMISSED".equals(kind)) jdbc.update("update blade_smart_live_activity set status='DISMISSED',activity_token='',updated_at=now() where id=?",row.get("id"));
            else if(!Set.of("DISMISSED","ENDED").contains(row.get("status")))
                jdbc.update("update blade_smart_live_activity set activity_token=?,updated_at=now() where id=?",secrets.encrypt(token),row.get("id"));
        }
    }
}
