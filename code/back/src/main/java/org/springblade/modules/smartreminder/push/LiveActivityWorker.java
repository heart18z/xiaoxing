package org.springblade.modules.smartreminder.push;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** One activity per device, persisted windows prevent restart loops after dismissal or an uncertain start. */
@Slf4j
@Component
public class LiveActivityWorker {
    private final JdbcTemplate jdbc; private final LiveActivityPlans plans; private final SecretCodec secrets;
    private final ApnsClient apns; private final ObjectMapper json;
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"live-activities");t.setDaemon(true);return t;});
    public LiveActivityWorker(JdbcTemplate jdbc,LiveActivityPlans plans,SecretCodec secrets,ApnsClient apns,ObjectMapper json) {
        this.jdbc=jdbc;this.plans=plans;this.secrets=secrets;this.apns=apns;this.json=json;
    }
    @EventListener(ApplicationReadyEvent.class) public void start() {
        executor.scheduleWithFixedDelay(()->{try{runBatch(Instant.now());}catch(Exception e){log.warn("实时活动稍后重试（{}）",e.getClass().getSimpleName());}},20,15,TimeUnit.SECONDS);
    }
    @PreDestroy public void close() {executor.shutdownNow();}
    public void runBatch(Instant now) {
        for(var d:jdbc.queryForList("select device_id from blade_smart_live_device order by device_id")) {
            if(Thread.currentThread().isInterrupted()) return;
            process(d.get("device_id").toString(),now);
        }
        jdbc.update("delete from blade_smart_live_activity where expires_at<?",Timestamp.from(now.minusSeconds(7*86400)));
    }
    public void process(String deviceId,Instant now) {
        String lease=UUID.randomUUID().toString();
        if(jdbc.update("update blade_smart_live_device set lease_token=?,lease_until=? where device_id=? and (lease_until is null or lease_until<?)",lease,Timestamp.from(now.plusSeconds(180)),deviceId,Timestamp.from(now))!=1) return;
        try { reconcile(deviceId,now); }
        catch(Exception e) {log.warn("实时活动设备处理稍后重试（{}）",e.getClass().getSimpleName());}
        finally {jdbc.update("update blade_smart_live_device set lease_until=null,lease_token=null where device_id=? and lease_token=?",deviceId,lease);}
    }
    private void reconcile(String deviceId,Instant now) throws Exception {
        var devices=jdbc.queryForList("""
            select d.*,l.start_token,l.enabled as live_enabled,l.binding_id as live_binding
            from blade_smart_live_device l left join blade_smart_push_device d on d.id=l.device_id
            where l.device_id=?
            """,deviceId);
        if(devices.isEmpty()) return;
        var d=devices.get(0);
        boolean bound=Objects.equals(d.get("binding_id"),d.get("live_binding")) && number(d.get("enabled"))==1 && number(d.get("live_enabled"))==1;
        long userId=number(d.get("user_id"));
        if(bound) bound=jdbc.queryForObject("select count(*) from blade_user where id=? and is_deleted=0 and status=1",Integer.class,userId)==1;
        var candidates=bound?plans.candidates(userId,now):List.<LiveActivityPlans.Plan>of();
        var active=jdbc.queryForList("select * from blade_smart_live_activity where device_id=? and status in ('STARTING','ACTIVE','ENDING','FAILED') order by updated_at",deviceId);
        // Retain a still-eligible current activity: no flip-flopping between simultaneous tasks.
        Map<String,Object> retained=null;
        for(var a:active) {
            var plan=candidates.stream().filter(p->p.key().equals(a.get("activity_key"))).findFirst().orElse(null);
            boolean keep=retained==null && bound && Objects.equals(a.get("binding_id"),d.get("binding_id")) && plan!=null && Set.of("STARTING","ACTIVE").contains(a.get("status"));
            if(keep && a.get("activity_token").toString().isEmpty() && expiry(a.get("updated_at")).isBefore(now.minusSeconds(120))) keep=false;
            if(!keep) {end(a,now);continue;}
            retained=a;
            String language=Objects.toString(d.get("language"),"zh-cn"), body=json.writeValueAsString(plan.content(language,now)),hash=PushDeviceService.hash(body);
            if(!a.get("activity_token").toString().isEmpty() && (!hash.equals(a.get("content_hash")) || "STARTING".equals(a.get("status")))) {
                if(!current(deviceId,d.get("binding_id").toString(),userId)) {end(a,now);return;}
                var result=send(a,"update",plan.content(language,now),now);
                if(result.accepted()) jdbc.update("update blade_smart_live_activity set status='ACTIVE',content_json=?,content_hash=?,last_error=null,updated_at=now() where id=? and status in ('STARTING','ACTIVE')",body,hash,a.get("id"));
                else failure(a,result);
            }
        }
        if(retained!=null || !bound || Objects.toString(d.get("start_token"),"").isEmpty() || !apns.ready(d.get("environment").toString())) return;
        // Do not overlap a previous card while its end is awaiting a token/provider retry.
        if(jdbc.queryForObject("select count(*) from blade_smart_live_activity where device_id=? and status='ENDING' and expires_at>?",Integer.class,deviceId,Timestamp.from(now))>0) return;
        for(var plan:candidates) {
            if(jdbc.queryForObject("select count(*) from blade_smart_live_activity where device_id=? and binding_id=? and activity_key=?",Integer.class,deviceId,d.get("binding_id"),plan.key())>0) continue;
            String id=UUID.randomUUID().toString(),body=json.writeValueAsString(plan.content(Objects.toString(d.get("language"),"zh-cn"),now));
            jdbc.update("insert into blade_smart_live_activity(id,device_id,binding_id,activity_key,user_id,environment,activity_token,status,content_json,content_hash,expires_at,updated_at) values(?,?,?,?,?,?,'','STARTING',?,?,?,?)",
                id,deviceId,d.get("binding_id"),plan.key(),userId,d.get("environment"),body,PushDeviceService.hash(body),Timestamp.from(Instant.ofEpochSecond(plan.endAt())),Timestamp.from(now));
            // Persist before send: timeout/crash must never generate a duplicate start.
            var content=plan.content(Objects.toString(d.get("language"),"zh-cn"),now);
            var payload=payload("start",content,now);
            if(!current(deviceId,d.get("binding_id").toString(),userId)) {
                jdbc.update("update blade_smart_live_activity set status='ENDED' where id=?",id);return;
            }
            var result=apns.sendLive(d.get("environment").toString(),secrets.decrypt(d.get("start_token").toString()),id,Instant.ofEpochSecond(plan.endAt()),payload);
            if(!result.accepted()) jdbc.update("update blade_smart_live_activity set status='FAILED',last_error=? where id=? and status='STARTING'",result.reason(),id);
            if(result.invalidDevice()) jdbc.update("update blade_smart_live_device set start_token='' where device_id=? and binding_id=? and start_token=?",deviceId,d.get("binding_id"),d.get("start_token"));
            break;
        }
    }
    private boolean current(String deviceId,String bindingId,long owner) {
        return jdbc.queryForObject("""
            select count(*) from blade_smart_push_device d join blade_smart_live_device l on l.device_id=d.id and l.binding_id=d.binding_id
            join blade_user u on u.id=d.user_id and u.is_deleted=0 and u.status=1
            where d.id=? and d.binding_id=? and d.user_id=? and d.enabled=1 and l.enabled=1
            """,Integer.class,deviceId,bindingId,owner)==1;
    }
    private void end(Map<String,Object> a,Instant now) throws Exception {
        if(expiry(a.get("expires_at")).isBefore(now.minusSeconds(3600))) {jdbc.update("update blade_smart_live_activity set status='ENDED',activity_token='' where id=?",a.get("id"));return;}
        jdbc.update("update blade_smart_live_activity set status='ENDING' where id=? and status not in ('ENDED','DISMISSED')",a.get("id"));
        if(a.get("activity_token").toString().isEmpty()) return;
        Map<String,Object> content=json.readValue(a.get("content_json").toString(),new TypeReference<>(){});
        var result=send(a,"end",content,now);
        if(result.accepted() || result.invalidDevice()) jdbc.update("update blade_smart_live_activity set status='ENDED',activity_token='',updated_at=now() where id=?",a.get("id"));
        else failure(a,result);
    }
    private ApnsClient.Result send(Map<String,Object> a,String event,Map<String,Object> content,Instant now) {
        return apns.sendLive(a.get("environment").toString(),secrets.decrypt(a.get("activity_token").toString()),a.get("id").toString(),
            "end".equals(event)?now.plusSeconds(300):expiry(a.get("expires_at")),payload(event,content,now));
    }
    private void failure(Map<String,Object> a,ApnsClient.Result result) {
        if(result.invalidDevice()) jdbc.update("update blade_smart_live_activity set status='DISMISSED',activity_token='',last_error=? where id=?",result.reason(),a.get("id"));
        else jdbc.update("update blade_smart_live_activity set last_error=? where id=?",result.reason(),a.get("id"));
    }
    public static Map<String,Object> payload(String event,Map<String,Object> content,Instant now) {
        var aps=new LinkedHashMap<String,Object>();aps.put("timestamp",now.getEpochSecond());aps.put("event",event);aps.put("content-state",content);
        aps.put("stale-date",content.get("endAt"));
        if("start".equals(event)) {
            aps.put("attributes-type","XiaoxingReminderAttributes");
            aps.put("input-push-token",1);
            aps.put("attributes",Map.of("owner",content.get("owner"),"key",content.get("key"),"eventId",content.get("eventId")));
            boolean en="en-us".equals(content.get("language"));
            aps.put("alert",Map.of("title",en?"Upcoming reminder":"即将到来的提醒","body",content.get("title")));
        }
        if("end".equals(event)) aps.put("dismissal-date",now.getEpochSecond());
        return Map.of("aps",aps);
    }
    private static long number(Object x) {return x instanceof Number n?n.longValue():0;}
    private static Instant expiry(Object x) {return PushDeliveryWorker.expiryInstant(x);}
}
