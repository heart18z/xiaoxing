package org.springblade.modules.smartreminder.push;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public final class PushOutbox {
    private PushOutbox() {}
    private static final Set<String> TYPES=Set.of("REMINDER","QUESTION","FEEDBACK","EVENT_ASSIGNED","CONFLICT");
    /** Called in the same JDBC transaction as the business message. No network/AI calls here. */
    public static void enqueue(JdbcTemplate jdbc,long messageId,long userId,String role,String type,Long eventId,boolean read) {
        if(read || eventId==null || !"assistant".equals(role) || !TYPES.contains(type)) return;
        var devices=jdbc.queryForList("select id,binding_id from blade_smart_push_device where user_id=? and enabled=1",userId);
        Instant now=Instant.now();
        for(var device:devices) jdbc.update("insert into blade_smart_push_delivery(id,message_id,event_id,user_id,device_id,binding_id,apns_id,status,attempts,next_attempt_at,expires_at,created_at) values(?,?,?,?,?,?,?,'PENDING',0,?,?,?)",IdWorker.getId(),messageId,eventId,userId,device.get("id"),device.get("binding_id"),UUID.randomUUID().toString(),Timestamp.from(now),Timestamp.from(now.plusSeconds(3600)),Timestamp.from(now));
    }
}
