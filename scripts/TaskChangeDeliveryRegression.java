import org.springblade.modules.smartreminder.service.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Isolated H2 + fake model; no production messages or live model calls. */
public class TaskChangeDeliveryRegression extends Stage1Regression {
  static int scheduleCalls;
  static String decisionAction="DEFER",decisionSystem="";
  static class DeferringAi extends FakeAi {
    @Override public AiAnswer chat(String system,String user){
      if(system.startsWith("decision-fixture")){decisionSystem=system;return new AiAnswer("{\"action\":\""+decisionAction+"\",\"reason\":\"根据接收人知晓情况评估\",\"reminderContent\":\"陈颖，报告改为下午3点提交。\",\"nextEvaluateTime\":\"2099-01-01 14:00:00\"}","","fixture","");}
      if(system.contains("创建前日程语义审查器"))scheduleCalls++;
      return super.chat(system,user);
    }
  }
  static String update(long id,String time,String task){return "{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\""+id+"\",\"recipientName\":\"陈颖\",\"eventTime\":\""+time+"\",\"recipientTasks\":[{\"recipientName\":\"陈颖\",\"content\":\""+task+"\"}]}}";}
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    var ai=new DeferringAi();var schedules=new SmartScheduleService(db,mapper,ai);
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),schedules);
    event(5001,1,2);
    db.update("update blade_smart_event set event_time='2099-01-01 13:00:00' where id=5001");
    db.update("update blade_smart_event_branch set task_content='下午1点提交报告' where event_id=5001");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content,next_evaluate_time) values(25001,5001,3,'私人任务不应泄露','2099-01-01 12:00:00')");
    db.update("insert into blade_smart_chat_message(id,user_id,event_id,message_role,message_type,content) values(65001,2,5001,'assistant','EVENT_ASSIGNED','请于13:00提交报告')");
    db.update("insert into blade_smart_chat_message(id,user_id,event_id,message_role,message_type,content) values(65002,1,null,'user','TEXT','通知陈颖下午1点提交报告'),(65003,1,null,'user','TEXT','改到下午3点提交吧'),(65004,3,5001,'user','TEXT','OTHER_RECIPIENT_PRIVATE'),(65005,1,5001,'user','TEXT','UNLINKED_FOCUS_CHAT')");
    db.update("insert into blade_smart_message_event(event_id,user_id,message_id) values(5001,1,65002),(5001,1,65003),(5001,3,65004)");
    String action=update(5001,"2099-01-01 15:00:00","下午3点提交报告");
    call(action);
    check(count("select count(*) from blade_smart_notification where event_id=5001 and recipient_user_id=2")==0,"time change queues AI assessment and never sends before evaluation");
    check(count("select count(*) from blade_smart_notification where event_id=5001 and recipient_user_id=3")==0,"unchanged recipient gets no update notification");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_evaluation where event_id=5001 and decision_action='DEFER'")==1,"AI may still defer future reminders");
    check(count("select count(*) from blade_smart_notification where event_id=5001")==0,"AI DEFER is respected; no hardcoded change notification");
    String snapshot=db.queryForObject("select input_snapshot from blade_smart_evaluation where event_id=5001",String.class);
    check(snapshot.contains("creator_event_conversation")&&snapshot.contains("recipient_event_conversation")&&snapshot.contains("请于13:00提交报告")&&snapshot.contains("改到下午3点提交吧"),"AI receives both raw event conversations including original delivered time and creator amendment");
    check(!snapshot.contains("OTHER_RECIPIENT_PRIVATE")&&!snapshot.contains("UNLINKED_FOCUS_CHAT"),"other recipients and unlinked focus chats are excluded");
    check(!snapshot.contains("original_text")&&!snapshot.contains("changeEvidence")&&snapshot.contains("timeline"),"no fake original_text or redundant before/after summaries; timeline retained");
    var parsedInput=mapper.readTree(snapshot);
    check(parsedInput.path("current_state").path("event_time").asText().equals("2099-01-01 15:00:00"),"current state time is readable and separate from raw conversations");
    check(decisionSystem.contains("不能认定")&&decisionSystem.contains("不得机械地见变更就通知"),"runtime guidance fixes mistaken comparison while preserving AI discretion");
    call(action);
    check(count("select count(*) from blade_smart_notification where event_id=5001")==0,"same task/time update does not resend");
    call(update(5001,"2099-01-01 15:00:00","下午3点提交最终版报告"));
    check(count("select count(*) from blade_smart_notification where event_id=5001")==0,"task-only change also waits for AI decision");
    decisionAction="SEND";service.evaluateRequestedBranches();
    String notice=db.queryForObject("select notification_content from blade_smart_notification where event_id=5001",String.class);
    check(notice.equals("陈颖，报告改为下午3点提交。"),"SEND uses AI-authored message without deterministic rewrite");
    check(count("select count(*) from blade_smart_notification where event_id=5001 and recipient_user_id=3")==0,"AI branch decision cannot broadcast to another recipient");
    decisionAction="DEFER";
    call("{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\"5001\",\"recipientName\":\"陈颖\",\"fact\":\"材料已准备好\"}}");
    check(count("select count(*) from blade_smart_notification where event_id=5001")==1,"ordinary feedback still waits for AI assessment");
    String first=mapper.readTree(update(5001,"2099-01-01 16:00:00","下午4点提交报告")).path("eventAction").toString();
    reject("{\"intent\":\"update_event\",\"eventActions\":["+first+",{\"eventId\":\"23\",\"recipientNames\":[\"不存在的好友\"]}]}","later failure rejects update transaction");
    check(count("select count(*) from blade_smart_notification where event_id=5001")==1,"rollback cannot leave phantom change notifications");
    db.update("update blade_smart_event_branch set branch_status='STOPPED' where event_id=5001 and recipient_user_id=2");
    call(update(5001,"2099-01-01 16:00:00","下午4点提交报告"));
    check(count("select count(*) from blade_smart_notification where event_id=5001")==1,"stopped branch is not notified or resumed");

    db.update("insert into blade_user(id,account,name) values(9001,'cache-person','缓存测试')");event(6001,1,9001);
    db.update("update blade_smart_event_branch set task_content='原有安排' where event_id=6001");
    ArrayNode proposed=(ArrayNode)mapper.readTree("[{\"summary\":\"另一项任务\",\"eventTime\":\"2099-01-01 17:00:00\",\"recipients\":[{\"id\":\"9001\",\"name\":\"缓存测试\"}]}]");
    int start=scheduleCalls;schedules.review(proposed,Set.of());
    ((ObjectNode)proposed.get(0)).putArray("relatedMessageIds").add("101");((ObjectNode)proposed.get(0)).putArray("scheduleConflicts");
    schedules.review(proposed,Set.of());
    check(scheduleCalls==start+1,"unchanged review reused despite added chat/card metadata");
    db.update("update blade_smart_event set event_time='2099-01-01 18:00:00' where id=6001");schedules.review(proposed,Set.of());
    check(scheduleCalls==start+2,"existing time change forces model recheck");
    db.update("update blade_smart_event_branch set task_content='改成新任务' where event_id=6001");schedules.review(proposed,Set.of());
    check(scheduleCalls==start+3,"existing task change forces model recheck");
    event(6002,1,9001);schedules.review(proposed,Set.of());check(scheduleCalls==start+4,"new active event invalidates review");
    db.update("update blade_smart_event_branch set branch_status='STOPPED' where event_id=6001");schedules.review(proposed,Set.of());
    check(scheduleCalls==start+5,"stopped event changes review snapshot");
    ((ObjectNode)proposed.get(0)).put("summary","修改后的拟创建任务");schedules.review(proposed,Set.of());check(scheduleCalls==start+6,"candidate change forces model recheck");
    scheduleAnswer="invalid";((ObjectNode)proposed.get(0)).put("summary","模型失败场景");
    for(int i=0;i<2;i++){try{schedules.review(proposed,Set.of());throw new AssertionError("must reject invalid model answer");}catch(org.springblade.core.log.exception.ServiceException expected){}}
    check(scheduleCalls==start+8,"failed or malformed reviews are never cached");scheduleAnswer="{\"conflicts\":[]}";

    var candidate=call("{\"intent\":\"create_event\",\"events\":[{\"summary\":\"通知陈颖明天下午3点开会\",\"recipientNames\":[\"陈颖\"],\"recipientTasks\":[{\"recipientName\":\"陈颖\",\"content\":\"明天下午3点开会\"}],\"eventTime\":\"2099-01-01 15:00:00\",\"firstEvaluateTime\":\"2099-01-01 14:00:00\"}]}");
    int beforeConfirm=scheduleCalls;
    var confirm=SmartReminderService.class.getDeclaredMethod("confirmWithScheduleReviewFor",Long.class,Long.class,boolean.class);confirm.setAccessible(true);
    tx.execute(status->{try{return confirm.invoke(service,1L,Long.valueOf(candidate.get("candidateId").toString()),false);}catch(Exception e){throw new RuntimeException(e);}});
    check(scheduleCalls==beforeConfirm,"real candidate-confirm path avoids repeated model call when schedule unchanged");
    // A positive cached conflict is still shown and still requires explicit approval.
    long occupied=db.queryForObject("select min(e.id) from blade_smart_event e join blade_smart_event_branch b on b.event_id=e.id where b.recipient_user_id=2 and b.branch_status='ACTIVE' and e.event_status='ACTIVE'",Long.class);
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"2\",\"otherEventId\":\""+occupied+"\"}]}";
    var conflictCandidate=call("{\"intent\":\"create_event\",\"events\":[{\"summary\":\"不同的待确认事项\",\"recipientNames\":[\"陈颖\"],\"recipientTasks\":[{\"recipientName\":\"陈颖\",\"content\":\"下午3点参加另一场会议\"}],\"eventTime\":\"2099-01-01 15:00:00\",\"firstEvaluateTime\":\"2099-01-01 14:00:00\"}]}");
    int beforeConflictConfirm=scheduleCalls;
    Map<?,?> conflict=(Map<?,?>)tx.execute(status->{try{return confirm.invoke(service,1L,Long.valueOf(conflictCandidate.get("candidateId").toString()),false);}catch(Exception e){throw new RuntimeException(e);}});
    check(Boolean.TRUE.equals(conflict.get("confirmationRequired"))&&scheduleCalls==beforeConflictConfirm,"cached positive conflict still requires user approval");
    // Expiration is tested without waiting ten minutes or changing the system clock.
    var field=SmartScheduleService.class.getDeclaredField("reviews");field.setAccessible(true);
    Map<String,Object> cache=(Map<String,Object>)field.get(schedules);
    var record=cache.values().iterator().next().getClass();var ctor=record.getDeclaredConstructor(long.class,List.class);ctor.setAccessible(true);
    for(String key:new ArrayList<>(cache.keySet()))cache.put(key,ctor.newInstance(0L,List.of()));
    scheduleAnswer="{\"conflicts\":[]}";
    schedules.review(proposed,Set.of());check(scheduleCalls==beforeConflictConfirm+1,"expired results are not reused");
    System.out.println("ALL TASK CHANGE DELIVERY AND REVIEW CACHE REGRESSIONS PASSED");
  }
}
