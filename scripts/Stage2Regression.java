import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springblade.modules.smartreminder.service.*;
import java.util.*;
import java.lang.reflect.*;

/** Extends the isolated Stage 1 fixture; deterministic AI responses, no live events touched. */
public class Stage2Regression extends Stage1Regression {
  static Object invoke(String method,Class<?>[] types,Object... args) throws Exception {
    Method m=SmartReminderService.class.getDeclaredMethod(method,types);m.setAccessible(true);
    return tx.execute(status->{try{return m.invoke(service,args);}catch(Exception e){throw new RuntimeException(e instanceof InvocationTargetException?e.getCause():e);}});
  }
  public static void main(String[] args) throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    SmartSocialService social=new SmartSocialService(db);
    social.remember(1L,101L,mapper.readTree("[{\"alias\":\"贾东\",\"personUserId\":\"2\"}]"));
    check(social.aliases(1L).stream().anyMatch(row->row.get("aliasName").equals("贾东")),"alias memory persists for the current user");
    check(social.aliases(3L).isEmpty(),"other users cannot inherit a private alias");
    List<?> resolved=(List<?>)invoke("resolvePerson",new Class<?>[]{Long.class,String.class},1L,"贾东");
    check(resolved.size()==1,"persisted alias resolves to one existing friend");
    boolean invalid=false;try{social.remember(3L,999L,mapper.readTree("[{\"alias\":\"陌生人\",\"personUserId\":\"2\"}]"));}catch(Exception e){invalid=true;}
    check(invalid,"AI cannot bind an alias to a non-friend");
    social.saveAiAvatar(1L,"/avatars/shapes-4.svg");social.saveAiAvatar(2L,"/avatars/thumbs-2.svg");
    check(social.aiAvatar(1L).equals("/avatars/shapes-4.svg")&&social.aiAvatar(2L).equals("/avatars/thumbs-2.svg"),"AI avatar is durable and personal per user");
    social.saveAiAvatar(1L,null);check(social.aiAvatar(1L).equals("/avatars/shapes-4.svg"),"unrelated profile update preserves AI avatar");
    event(201,1,2);db.update("update blade_smart_event_branch set branch_status='STOPPED',stop_reason='已完成',next_evaluate_time=null,current_fact='旧任务已完成' where event_id=201");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,branch_status) values(20201,201,3,'ACTIVE')");
    long eventCount=count("select count(*) from blade_smart_event");
    call("""
      {"intent":"update_event","eventAction":{"eventId":"201","resumeRecipientNames":["贾东"],"eventTime":"2099-01-02 15:00:00","nextEvaluateTime":"2099-01-02 14:00:00","recipientTasks":[{"recipientName":"陈颖","content":"明天下午三点参加工程试运行会议"}]}}
      """);
    check(count("select count(*) from blade_smart_event_branch where id=10201 and branch_status='ACTIVE' and stop_reason is null and next_evaluate_time is not null")==1,"resumes existing stopped branch and schedules a new evaluation");
    check(count("select count(*) from blade_smart_event_branch where id=10201 and current_fact is null")==1,"resume clears the previous completed fact");
    check(count("select count(*) from blade_smart_event")==eventCount&&count("select count(*) from blade_smart_event_branch where event_id=201")==2,"resume creates no duplicate event or branch");
    check(count("select count(*) from blade_smart_timeline where event_id=201 and node_type='AI_BRANCH_RESUMED'")==1,"resume leaves an AI timeline receipt");
    check(count("select count(*) from blade_smart_chat_message where event_id=201 and user_id=2 and message_type='EVENT_ASSIGNED'")==1,"resumed recipient receives a scoped notification");
    event(203,1,2);
    duringEvaluation=()->db.update("update blade_smart_event_branch set next_evaluate_time='2099-01-03 14:00:00',lock_time=null where event_id=203");
    invoke("claimEvaluation",new Class<?>[]{Long.class,boolean.class},10203L,false);
    check(count("select count(*) from blade_smart_notification where event_id=203")==0,"AI result from before rescheduling cannot notify a resumed branch");
    check(count("select count(*) from blade_smart_event_branch where event_id=203 and next_evaluate_time='2099-01-03 14:00:00'")==1,"stale evaluation preserves the resumed schedule");
    event(202,1,2);db.update("update blade_smart_event set event_status='STOPPED' where id=202");
    reject("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"202\",\"resumeRecipientNames\":[\"陈颖\"]}}","stopped parent cannot be silently restarted");
    ArrayNode plans=(ArrayNode)mapper.readTree("[{\"summary\":\"不同事项\",\"recipientNames\":[\"陈颖\"],\"recipients\":[{\"id\":\"2\",\"name\":\"陈颖\",\"taskContent\":\"不同事项\"}],\"eventTime\":\"2099-01-02 15:00:00\",\"relatedMessageIds\":[\"101\"]}]");
    SmartScheduleService schedules=new SmartScheduleService(db,mapper,new FakeAi());
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"2\",\"otherEventId\":\"201\"}]}";
    check(schedules.review(plans,Set.of()).size()==1,"different simultaneous task is presented as a conflict");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id,status) values(97,3,2,'ACTIVE')");
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,candidate_status) values(801,3,101,?,'PENDING')",plans.toString());
    long before=count("select count(*) from blade_smart_event");
    Map<?,?> first=(Map<?,?>)invoke("confirmWithScheduleReviewFor",new Class<?>[]{Long.class,Long.class,boolean.class},3L,801L,false);
    check(Boolean.TRUE.equals(first.get("confirmationRequired"))&&count("select count(*) from blade_smart_event")==before,"conflict blocks creation until the later arranger explicitly agrees");
    Map<?,?> accepted=(Map<?,?>)invoke("confirmWithScheduleReviewFor",new Class<?>[]{Long.class,Long.class,boolean.class},3L,801L,true);
    check(accepted.containsKey("eventIds")&&count("select count(*) from blade_smart_event")==before+1,"explicitly confirmed reviewed conflict allows creation");
    scheduleAnswer="{\"conflicts\":[]}";
    check(schedules.review(plans,Set.of()).isEmpty(),"same-event semantic judgment produces no false warning");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id,status) values(99,2,1,'ACTIVE')");
    db.update("insert into blade_friend_request(id,applicant_user_id,target_user_id,request_type,request_status) values(98,2,1,'PERMISSION','PENDING')");
    tx.executeWithoutResult(status->social.removeFriend(1L,2L));
    check(count("select count(*) from blade_friendship where ((owner_user_id=1 and friend_user_id=2) or (owner_user_id=2 and friend_user_id=1)) and status='ACTIVE'")==0,"unfriend removes both reminder permission directions");
    check(count("select count(*) from blade_friend_request where id=98 and request_status='CANCELLED'")==1,"unfriend cancels pending permission changes");
    check(social.aliases(1L).isEmpty(),"removed friend no longer resolves through cached alias context");
    check(count("select count(*) from blade_smart_event where id=201")==1,"unfriend preserves historical events");
    reject("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"201\",\"resumeRecipientNames\":[\"陈颖\"]}}","removed friend cannot be reactivated through chat");
    System.out.println("ALL STAGE 2 REGRESSIONS PASSED");
  }
}
