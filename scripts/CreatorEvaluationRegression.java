import org.springblade.modules.smartreminder.service.*;
import java.util.*;
import java.lang.reflect.*;

/** Isolated H2 tests: never notify real users or call an external model. */
public class CreatorEvaluationRegression extends Stage1Regression {
  static Object invoke(String name,Class<?>[] types,Object... args)throws Exception {
    var m=SmartReminderService.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(service,args);
  }
  static void feedback(long event,String fact){call("{\"intent\":\"feedback\",\"reply\":\"已记录\",\"feedback\":{\"eventId\":\""+event+"\",\"fact\":\""+fact+"\"}}");}
  static void future(long event){db.update("update blade_smart_event_branch set next_evaluate_time='2099-01-01 14:00:00',task_content='参加会议' where event_id=?",event);}
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    event(301,1,2);future(301);feedback(301,"同意改为明天下午三点");
    check(count("select count(*) from blade_smart_event_branch where event_id=301 and evaluation_requested_at is not null and next_evaluate_time='2099-01-01 14:00:00'")==1,"creator feedback queues immediate evaluation without losing scheduled fallback");
    check(count("select count(*) from blade_smart_notification where event_id=301")==0,"saving feedback does not bypass the evaluation AI");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=301 and trigger_type='CREATOR_FEEDBACK'")==1,"feedback worker evaluates before scheduled time and records trigger");
    check(count("select count(*) from blade_smart_notification where event_id=301")==1,"AI decision notifies affected recipient");
    check(count("select count(*) from blade_smart_event_branch where event_id=301 and evaluation_requested_at is null and evaluate_lock=0")==1,"completed queue item is acknowledged");
    feedback(301,"同意改为明天下午三点");service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=301")==1,"same feedback does not trigger another assessment");
    call("{\"intent\":\"chat\",\"reply\":\"你好\"}");service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=301")==1,"ordinary chat does not trigger evaluation");
    event(302,1,2);future(302);
    call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"302\",\"eventTime\":\"2099-01-02 15:00:00\",\"fact\":\"发起人同意改期\"}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=302 and evaluation_requested_at is not null")==1,"creator confirmation parsed as event update also queues assessment");
    service.evaluateRequestedBranches();
    event(303,1,2);future(303);feedback(303,"第一版反馈");
    duringEvaluation=()->feedback(303,"最新反馈：改为下午四点");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_notification where event_id=303")==0,"in-flight stale assessment cannot send notification");
    check(count("select count(*) from blade_smart_event_branch where event_id=303 and evaluation_requested_at is not null and evaluate_lock=0")==1,"newer feedback remains queued after stale result discarded");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=303")==1,"latest version evaluated once");
    String snapshot=db.queryForObject("select input_snapshot from blade_smart_evaluation where event_id=303",String.class);
    check(snapshot.contains("最新反馈"),"evaluation sees latest committed facts");
    event(304,1,2);future(304);
    reject("{\"intent\":\"update_event\",\"eventActions\":[{\"eventId\":\"304\",\"fact\":\"待回滚\"},{\"eventId\":\"22\",\"fact\":\"无权操作\"}]}","unauthorized batch rejected");
    check(count("select count(*) from blade_smart_event_branch where event_id=304 and evaluation_requested_at is not null")==0,"rollback cannot leave an evaluation job");
    event(307,1,2);future(307);
    reject("{\"intent\":\"update_event\",\"eventActions\":[{\"eventId\":\"304\",\"fact\":\"待回滚\"},{\"eventId\":\"307\",\"recipientNames\":[\"不存在的好友\"]}]}","later operation failure rolls back earlier feedback");
    check(count("select count(*) from blade_smart_event_branch where event_id=304 and evaluation_requested_at is not null")==0,"transaction rollback removes already queued job");
    feedback(304,"请尽快同步");
    var tries=new java.util.concurrent.atomic.AtomicInteger();
    var failing=new FakeAi(){@Override public AiAnswer chat(AiConfigService.AiRuntimeConfig c,String system,String user){if(system.startsWith("decision-fixture")){tries.incrementAndGet();throw new IllegalStateException("simulated upstream unavailable");}return super.chat(c,system,user);}};
    service=new SmartReminderService(db,mapper,failing,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,failing));
    service.evaluateRequestedBranches();service.evaluateRequestedBranches();
    check(tries.get()==1&&count("select count(*) from blade_smart_event_branch where event_id=304 and evaluation_requested_at>now() and evaluate_lock=0")==1,"upstream failure persists delayed retry without hot looping");
    var ai=new FakeAi();service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    event(305,1,2);future(305);feedback(305,"待同步内容");
    call("{\"intent\":\"stop_event\",\"eventAction\":{\"eventId\":\"305\"}}");service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=305")==0,"stopping an event cancels pending evaluation");
    event(306,1,2);future(306);db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,next_evaluate_time) values(20306,306,3,'2099-01-01 14:00:00')");
    call("{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\"306\",\"recipientName\":\"陈颖\",\"fact\":\"同意陈颖调整时间\"}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=306 and evaluation_requested_at is not null and recipient_user_id=2")==1&&count("select count(*) from blade_smart_event_branch where event_id=306 and evaluation_requested_at is not null and recipient_user_id=3")==0,"targeted feedback only evaluates the affected recipient branch");
    db.update("insert into blade_user(id,account,name) values(-1,'viewer','卡片测试')");
    long older=com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),newer=com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,candidate_status) values(?,-1,101,'[{\"eventTime\":\"2099-01-01 15:00:00\"}]','PENDING'),(?,-1,101,'[{}]','CONFIRMED')",older,newer);
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,payload_json) values(?,-1,'assistant','CANDIDATE',?),(?,-1,'assistant','CANDIDATE',?)",older,"{\"candidateId\":"+older+"}",newer,"{\"candidateId\":\""+newer+"\"}");
    var cards=service.chatMessages(30);
    check(cards.stream().anyMatch(m->m.get("id").equals(String.valueOf(older))&&"CANDIDATE_EXPIRED".equals(m.get("messageType"))),"superseded operation card is disabled even when legacy message remained pending");
    check(cards.stream().anyMatch(m->m.get("id").equals(String.valueOf(newer))&&"CANDIDATE_CONFIRMED".equals(m.get("messageType"))),"authoritative status handles legacy numeric/string JSON IDs");
    boolean denied=false;try{tx.execute(s->service.confirmWithScheduleReview(older,true));}catch(RuntimeException e){denied=true;}
    check(denied,"server rejects clicking superseded card even with forged request");
    long expired=com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,candidate_status) values(?,-1,101,'[{\"eventTime\":\"2000-01-01 00:00:00\"}]','PENDING')",expired);
    denied=false;try{tx.execute(s->service.confirmWithScheduleReview(expired,true));}catch(RuntimeException e){denied=true;}
    check(denied,"server rejects expired event-time card");
    System.out.println("ALL CREATOR EVALUATION AND CARD REGRESSIONS PASSED");
  }
}
