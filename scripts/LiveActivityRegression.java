import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.modules.smartreminder.push.*;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/** Exercises real SQL planning, device proof, payloads and worker transitions. No remote APNs or production data. */
public class LiveActivityRegression {
    static final JdbcTemplate db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:live;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
    static final ObjectMapper json=new ObjectMapper();
    static final SecretCodec secrets=new SecretCodec("fixture-only-live-activity-key");
    static final String device="11111111-1111-4111-a111-111111111111",binding="22222222-2222-4222-a222-222222222222",secret="ab".repeat(32);
    static int assertions;
    static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);assertions++;}
    static void denied(Runnable r,String message) {try{r.run();throw new AssertionError(message);}catch(org.springblade.core.log.exception.ServiceException expected){assertions++;}}
    static String hash(String x)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));}
    static class FakeApns extends ApnsClient {
        final List<Map<String,Object>> deliveries=new ArrayList<>();
        Result next=new Result(200,"Accepted");
        FakeApns(){super(new PushProperties(),json);}
        @Override public boolean ready(String environment){return true;}
        @Override public Result sendLive(String env,String token,String id,Instant expires,Map<String,Object> payload) {
            deliveries.add(Map.of("token",token,"payload",payload));return next;
        }
        String event(int index){return ((Map<?,?>)deliveries.get(index).get("payload")).get("aps") instanceof Map<?,?> a?a.get("event").toString():"";}
    }
    static void event(long id,long recipient,String text,Instant time,Instant deadline,String status) {
        db.update("insert into blade_smart_event(id,creator_user_id,event_status,event_time,deadline_time,time_description) values(?,1,?,?,?,'')",id,status,local(time),local(deadline));
        db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content,branch_status,create_time) values(?,?,?,?,?,?)",id,id,recipient,text,status,local(Instant.now()));
    }
    static Object local(Instant time){return time==null?null:Timestamp.valueOf(LocalDateTime.ofInstant(time,LiveActivityPlans.ZONE));}
    static LiveActivityService.TokenRequest request(String kind,String token,String key){return new LiveActivityService.TokenRequest(device,secret,binding,"2",kind,token,key,true);}
    static int count(String sql,Object... args){return db.queryForObject(sql,Integer.class,args);}
    public static void main(String[] args)throws Exception {
        db.execute("create table blade_user(id bigint primary key,is_deleted int default 0,status int default 1)");
        db.execute("create table blade_smart_event(id bigint primary key,creator_user_id bigint,event_status varchar(20),event_time datetime,deadline_time datetime,time_description text)");
        db.execute("create table blade_smart_event_branch(id bigint primary key,event_id bigint,recipient_user_id bigint,task_content text,branch_status varchar(20),task_time_scoped int default 0,task_event_time datetime,task_deadline_time datetime,create_time datetime)");
        db.update("insert into blade_user(id) values(1),(2),(3)");
        PushSchema.migrate(db);LiveActivitySchema.migrate(db);LiveActivitySchema.migrate(db);
        db.update("insert into blade_smart_push_device(id,user_id,secret_hash,device_token,environment,binding_id,enabled,updated_at) values(?,2,?,'','production',?,1,now())",device,hash(secret),binding);
        var plans=new LiveActivityPlans(db);var api=new LiveActivityService(db,secrets,plans);var apns=new FakeApns();var worker=new LiveActivityWorker(db,plans,secrets,apns,json);
        Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),at=now.plusSeconds(3600);
        event(10,2,"下午3点参加会议",at,null,"ACTIVE");
        check(plans.candidates(2,now.minusSeconds(1)).isEmpty(),"outside one hour excluded");
        var first=plans.candidates(2,now).get(0);
        check(first.eventId().equals("10"),"other user's assignment included exactly at one hour");
        check(plans.candidates(1,now).isEmpty(),"creator cannot see another recipient's lock-screen projection");
        check(plans.candidates(2,at.plusSeconds(1799)).size()==1,"30 minute grace retains active task");
        check(plans.candidates(2,at.plusSeconds(1800)).isEmpty(),"grace boundary ends automatically");
        event(11,2,"今天完成材料",at,null,"ACTIVE");
        event(12,2,"下午4点提交材料",null,at.plusSeconds(1),"ACTIVE");
        event(13,3,"下午3点其他人私密任务",at,null,"ACTIVE");
        event(14,2,"下午3点已完成",at,null,"COMPLETED");
        event(15,2,"下午3点已停止",at,null,"STOPPED");
        check(plans.candidates(2,now).size()==1,"date-only, future, others and terminal tasks excluded");
        check(plans.candidates(2,now.plusSeconds(1)).size()==2,"deadline uses same one-hour lead");
        event(18,2,"半小时后打电话",now.plusSeconds(1800),null,"ACTIVE");
        check(plans.candidates(2,now).stream().anyMatch(p->p.eventId().equals("18")),"explicit relative time remains eligible");
        db.update("update blade_smart_event_branch set branch_status='STOPPED' where id=18");
        db.update("update blade_smart_event_branch set task_time_scoped=1,task_event_time=null,task_deadline_time=null where id=10");
        check(plans.candidates(2,now).isEmpty(),"explicit cleared branch time never falls back to parent's time");
        db.update("update blade_smart_event_branch set task_time_scoped=0 where id=10");
        event(16,2,"现在去取材料",null,null,"ACTIVE");
        check(plans.candidates(2,now.plusSeconds(1)).get(0).kind().equals("NOW"),"immediate task has bounded creation-time window");
        db.update("update blade_smart_event_branch set branch_status='STOPPED' where id=16");
        event(17,1,"下午3点自己的提醒",at,null,"ACTIVE");
        check(plans.candidates(1,now).get(0).eventId().equals("17"),"self-reminder supported");
        denied(()->api.token(new LiveActivityService.TokenRequest(device,"ff".repeat(32),binding,"2","START","cc".repeat(32),null,true)),"wrong proof rejected");
        denied(()->api.token(new LiveActivityService.TokenRequest(device,secret,binding,"3","START","cc".repeat(32),null,true)),"wrong owner rejected");
        denied(()->api.token(request("UPDATE","dd".repeat(32),"ab".repeat(20))),"unissued activity token cannot attach");
        api.token(request("START","cc".repeat(32),null));
        api.token(new LiveActivityService.TokenRequest(device,secret,binding,"2","ENABLED",null,null,false));
        api.token(request("START","cc".repeat(32),null));
        worker.process(device,now);
        check(apns.deliveries.isEmpty(),"late start-token rotation cannot override the user's disabled setting");
        api.token(request("ENABLED",null,null));
        check(!db.queryForObject("select start_token from blade_smart_live_device",String.class).contains("cc".repeat(32)),"start token encrypted");
        worker.process(device,now);
        check(apns.deliveries.size()==1 && apns.event(0).equals("start"),"one push-to-start issued");
        check(apns.deliveries.get(0).get("token").equals("cc".repeat(32)),"dedicated ActivityKit token used");
        var aps=(Map<?,?>)((Map<?,?>)apns.deliveries.get(0).get("payload")).get("aps");
        check(aps.get("attributes-type").equals("XiaoxingReminderAttributes") && aps.get("input-push-token").equals(1),"ActivityKit attributes and update-token request encoded");
        check(((Map<?,?>)aps.get("content-state")).get("title").equals("下午3点参加会议"),"only own branch title projected");
        worker.process(device,now.plusSeconds(10));
        check(apns.deliveries.size()==1,"poll/restart cannot duplicate start");
        api.token(request("UPDATE","dd".repeat(32),first.key()));
        worker.process(device,now.plusSeconds(15));
        check(apns.event(1).equals("update"),"start callback update token activates server updates");
        worker.process(device,now.plusSeconds(20));check(apns.deliveries.size()==2,"unchanged plans do not flood APNs");
        db.update("update blade_smart_event_branch set task_content='下午3点改为线上会议' where id=10");
        worker.process(device,now.plusSeconds(25));check(apns.deliveries.size()==3 && apns.event(2).equals("update"),"task text updates existing card");
        db.update("update blade_smart_event_branch set branch_status='STOPPED' where id=12");
        worker.process(device,at);check(apns.deliveries.size()==4 && apns.event(3).equals("update"),"at-time phase changes without per-second pushes");
        db.update("update blade_smart_event_branch set branch_status='COMPLETED' where id=10");
        worker.process(device,at.plusSeconds(5));
        check(apns.event(4).equals("end") && count("select count(*) from blade_smart_live_activity where status='ENDED'")==1,"completion ends immediately on scheduler tick");
        check(count("select count(*) from blade_smart_event_branch where id=10 and branch_status='COMPLETED'")==1,"display worker never changes task business status");
        db.update("update blade_smart_event_branch set branch_status='ACTIVE' where id=10");
        worker.process(device,at.plusSeconds(10));check(apns.deliveries.size()==5,"same ended window does not restart");
        db.update("update blade_smart_event set event_time=? where id=10",local(at.plusSeconds(86400)));
        check(plans.candidates(2,at).isEmpty(),"reschedule to tomorrow drops today's activity");
        Instant tomorrow=now.plusSeconds(86400);
        worker.process(device,tomorrow);check(apns.deliveries.size()==6,"new date has a new window");
        var next=plans.candidates(2,tomorrow).get(0);
        api.token(request("DISMISSED",null,next.key()));
        worker.process(device,tomorrow.plusSeconds(1));check(apns.deliveries.size()==6,"user dismissal respected across polling");
        db.update("update blade_smart_event set event_time=? where id=10",local(at.plusSeconds(172800)));
        apns.next=new ApnsClient.Result(0,"ProviderConnectionFailed");
        worker.process(device,now.plusSeconds(172800));
        worker.process(device,now.plusSeconds(172801));
        check(apns.deliveries.size()==7,"uncertain start response must not produce duplicate starts");
        // Late update token after a stop/failure must still receive end, not be promoted to ACTIVE.
        var late=plans.candidates(2,now.plusSeconds(172800)).get(0);
        api.token(request("UPDATE","ee".repeat(32),late.key()));apns.next=new ApnsClient.Result(200,"Accepted");
        worker.process(device,now.plusSeconds(172802));
        check(apns.event(7).equals("end"),"late token cleans up uncertain activity");
        db.update("update blade_smart_event set event_time=? where id=10",local(at.plusSeconds(259200)));
        worker.process(device,now.plusSeconds(259200));
        var cancelBeforeToken=plans.candidates(2,now.plusSeconds(259200)).get(0);
        db.update("update blade_smart_event_branch set branch_status='STOPPED' where id=10");
        worker.process(device,now.plusSeconds(259201));
        check(count("select count(*) from blade_smart_live_activity where activity_key=? and status='ENDING'",cancelBeforeToken.key())==1,"cancel before token persists pending end");
        api.token(request("UPDATE","ff".repeat(32),cancelBeforeToken.key()));
        worker.process(device,now.plusSeconds(259202));
        check(apns.event(9).equals("end"),"late token after cancellation receives end");
        db.update("update blade_smart_push_device set binding_id='33333333-3333-4333-a333-333333333333',user_id=3 where id=?",device);
        denied(()->api.token(request("START","cc".repeat(32),null)),"old binding cannot register after account switch");
        worker.process(device,now.plusSeconds(259203));check(apns.deliveries.size()==10,"old binding cannot start another user's activity");
        System.out.println("PASS: "+assertions+" live activity planning, authorization and lifecycle checks");
    }
}
