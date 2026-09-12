package org.springblade.modules.smartreminder.push;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class PushDeliveryWorker {
    private final JdbcTemplate jdbc; private final SecretCodec secrets; private final ApnsClient apns; private final PushProperties properties;
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"apns-delivery");t.setDaemon(true);return t;});
    public PushDeliveryWorker(JdbcTemplate jdbc,SecretCodec secrets,ApnsClient apns,PushProperties properties){this.jdbc=jdbc;this.secrets=secrets;this.apns=apns;this.properties=properties;}
    @EventListener(ApplicationReadyEvent.class)
    public void start(){executor.scheduleWithFixedDelay(()->{try{runBatch();}catch(Exception e){log.warn("APNs 队列暂不可用，稍后重试（{}）",e.getClass().getSimpleName());}},10,5,TimeUnit.SECONDS);}
    @PreDestroy public void close(){executor.shutdownNow();}
    public void runBatch() {
        if(!properties.isEnabled()) return;
        Timestamp now=Timestamp.from(Instant.now());
        jdbc.update("update blade_smart_push_delivery set status='EXPIRED',lease_token=null where status in ('PENDING','SENDING') and expires_at<=?",now);
        var jobs=jdbc.queryForList("select id from blade_smart_push_delivery where expires_at>? and next_attempt_at<=? and (status='PENDING' or (status='SENDING' and lease_until<?)) order by next_attempt_at,id limit 20",now,now,now);
        for(var job:jobs){if(Thread.currentThread().isInterrupted())return;deliver(((Number)job.get("id")).longValue());}
    }
    public void deliver(long id) {
        String lease=UUID.randomUUID().toString(); Instant now=Instant.now();
        if(jdbc.update("update blade_smart_push_delivery set status='SENDING',lease_token=?,lease_until=? where id=? and expires_at>? and next_attempt_at<=? and (status='PENDING' or (status='SENDING' and lease_until<?))",lease,Timestamp.from(now.plusSeconds(60)),id,Timestamp.from(now),Timestamp.from(now),Timestamp.from(now))!=1)return;
        var rows=jdbc.queryForList("""
            select o.*,d.device_token,d.environment,d.language,m.message_type
            from blade_smart_push_delivery o
            join blade_smart_push_device d on d.id=o.device_id and d.user_id=o.user_id and d.binding_id=o.binding_id and d.enabled=1
            join blade_smart_chat_message m on m.id=o.message_id and m.user_id=o.user_id and m.event_id=o.event_id and m.is_read=0
            join blade_user u on u.id=o.user_id and u.is_deleted=0 and u.status=1
            where o.id=? and o.lease_token=?
            """,id,lease);
        if(rows.isEmpty()){finish(id,lease,"CANCELLED","ReadOrUnbound");return;}
        var row=rows.get(0);String environment=row.get("environment").toString();
        if(!apns.ready(environment)) {
            jdbc.update("update blade_smart_push_delivery set status='PENDING',next_attempt_at=?,lease_token=null,last_error='NotConfigured' where id=? and lease_token=?",Timestamp.from(now.plusSeconds(60)),id,lease);return;
        }
        ApnsClient.Result result;
        try{result=apns.send(environment,secrets.decrypt(row.get("device_token").toString()),row.get("apns_id").toString(),row.get("message_id").toString(),expiryInstant(row.get("expires_at")),payload(row));}
        catch(Exception e){result=new ApnsClient.Result(0,"DeliveryPreparationFailed");}
        int attempts=((Number)row.get("attempts")).intValue()+1;
        if(result.accepted()) {
            jdbc.update("update blade_smart_push_delivery set status='SENT',attempts=?,sent_at=now(),lease_token=null,last_error=null where id=? and lease_token=?",attempts,id,lease);
        } else {
            if(result.invalidDevice()) jdbc.update("update blade_smart_push_device set enabled=0,token_hash=null,device_token='' where id=? and binding_id=?",row.get("device_id"),row.get("binding_id"));
            boolean retry=result.retryable() && attempts<6;
            jdbc.update("update blade_smart_push_delivery set status=?,attempts=?,next_attempt_at=?,lease_token=null,last_error=? where id=? and lease_token=?",retry?"PENDING":"FAILED",attempts,Timestamp.from(now.plusSeconds(Math.min(900,30L<<Math.min(attempts-1,5)))),result.reason(),id,lease);
            log.warn("APNs 投递 {}：{}，重试={}",id,result.reason(),retry);
        }
    }
    static Instant expiryInstant(Object value) {
        if(value instanceof Timestamp timestamp) return timestamp.toInstant();
        // MySQL Connector/J may expose DATETIME as LocalDateTime rather than Timestamp.
        if(value instanceof java.time.LocalDateTime local) return local.atZone(java.time.ZoneId.systemDefault()).toInstant();
        if(value instanceof Instant instant) return instant;
        throw new IllegalArgumentException("Unsupported delivery expiry type");
    }
    private void finish(long id,String lease,String status,String reason){jdbc.update("update blade_smart_push_delivery set status=?,last_error=?,lease_token=null where id=? and lease_token=?",status,reason,id,lease);}
    static Map<String,Object> payload(Map<String,Object> row) {
        boolean en="en-us".equals(row.get("language"));
        // Lock-screen preview intentionally contains no task text, names or other participants' details.
        String body=switch(Objects.toString(row.get("message_type"),"")) {
            case "FEEDBACK" -> en?"You have new event feedback. Tap to view.":"你有新的事件反馈，点击查看。";
            case "QUESTION","CONFLICT" -> en?"An event needs your attention. Tap to view.":"有一项事件需要你确认，点击查看。";
            default -> en?"You have a new event reminder. Tap to view.":"你有一条新的事件提醒，点击查看。";
        };
        return Map.of("aps",Map.of("alert",Map.of("title",en?"AI Xiaoxing":"AI小醒","body",body),"sound","default"),"eventId",row.get("event_id").toString(),"messageId",row.get("message_id").toString(),"recipientUserId",row.get("user_id").toString());
    }
}
