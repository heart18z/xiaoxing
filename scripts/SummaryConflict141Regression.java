import org.springblade.modules.smartreminder.service.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Confirmed amendments and private conflict detail; H2 and fake AI only. */
public class SummaryConflict141Regression extends Stage1Regression {
  static String summaryInput="",evaluationInput="",evaluationSystem="";
  static class ReviewAi extends FakeAi {
    @Override public AiAnswer chat(String system,String user) {
      if(system.contains("事件概述")) {
        summaryInput=user;
        var result=mapper.createObjectNode().put("content","叫陈颖10月17日去西安。");
        var tasks=result.putArray("tasks");
        try { for(var row:mapper.readTree(user).path("tasks")) tasks.addObject().put("branchId",row.path("id").asText()).put("content",row.path("task").asText()); }
        catch(Exception e){throw new RuntimeException(e);}
        return new AiAnswer(result.toString(),"","fixture","");
      }
      if(system.startsWith("decision-fixture")) {
        evaluationInput=user;evaluationSystem=system;
        return new AiAnswer("{\"action\":\"ASK_CREATOR\",\"timeConflict\":true,\"conflictingEventIds\":[\"9941\",\"9942\",\"9941\",\"999999999\"],\"reminderContent\":\"MODEL_PRIVATE_TEXT\",\"nextEvaluateTime\":\"2099-10-18 18:00:00\"}","","fixture","");
      }
      return super.chat(system,user);
    }
  }
  static Map<String,Object> notice(long event,long user) {
    var row=db.queryForMap("select content,payload_json,message_type from blade_smart_chat_message where event_id=? and user_id=? and message_type='CONFLICT' order by id desc limit 1",event,user);
    return new HashMap<>(Map.of("content",row.get("content"),"payloadJson",row.get("payload_json"),"messageType",row.get("message_type")));
  }
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    var ai=new ReviewAi();content=new SmartEventContentService(db,mapper,ai);
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai),null);
    event(9910,1,2);
    db.update("update blade_smart_event set original_text='叫陈颖10月17日去西安。',event_summary=original_text where id=9910");
    db.update("update blade_smart_event_branch set task_content='10月17日去西安' where event_id=9910");
    content.refreshEventOverview(9910L);
    check(content.latestEventSummary(9910L,"").contains("17日"),"fixture starts with old overview");
    db.update("update blade_smart_chat_message set content='好' where id=101");
    var changed=call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"9910\",\"summary\":\"叫陈颖10月18日去西安。\",\"eventTime\":\"2099-10-18 09:00:00\",\"deadlineTime\":\"2099-10-18 23:59:00\",\"recipientTasks\":[{\"recipientName\":\"陈颖\",\"content\":\"10月18日去西安\"}]}}");
    check(changed.get("reply").toString().contains("18日"),"chat receipt uses confirmed summary");
    check(content.latestEventSummary(9910L,"").equals("叫陈颖10月18日去西安。"),"new date is immediately visible without background AI");
    check(content.latestTask(9910L,2L).contains("18日"),"recipient task changes with confirmation");
    content.refreshEventOverview(9910L);
    check(summaryInput.contains("confirmedUpdates")&&summaryInput.contains("18日")&&summaryInput.contains("好"),"overview gets accepted action alongside short acknowledgement");
    check(content.latestEventSummary(9910L,"").equals("叫陈颖10月18日去西安。"),"stale generated wording cannot undo an explicit single-recipient amendment");
    check(db.queryForObject("select original_text from blade_smart_event where id=9910",String.class).contains("17日"),"original historical request stays intact");
    event(9920,1,2);
    db.update("update blade_smart_event_branch set task_content='10月17日去西安' where event_id=9920");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content) values(29920,9920,3,'10月20日提交报告')");
    call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"9920\",\"recipientName\":\"陈颖\",\"summary\":\"10月18日去西安\",\"recipientTasks\":[{\"recipientName\":\"陈颖\",\"content\":\"10月18日去西安\"}]}}");
    String overview=content.latestEventSummary(9920L,"");
    check(overview.contains("18日")&&overview.contains("20日"),"scoped change preserves the other person's task in the overview");
    check(content.latestTask(9920L,3L).contains("20日")&&!content.latestTask(9920L,3L).contains("西安"),"recipient summaries remain isolated");

    db.update("update blade_smart_event_branch set next_evaluate_time='2099-12-31 23:00:00',evaluation_requested_at=null,evaluate_lock=0");
    event(9940,1,1);event(9941,2,1);event(9942,3,3);
    db.update("update blade_smart_event_branch set task_content='10月18日去厦门开会' where event_id=9940");
    db.update("update blade_smart_event_branch set task_content='10月18日去福州出差',next_evaluate_time='2099-12-31 23:00:00' where event_id=9941");
    db.update("update blade_smart_event set event_summary='PARENT_PRIVATE_DETAIL' where id=9941");
    db.update("update blade_smart_event_branch set task_content='OTHER_PERSON_SECRET',next_evaluate_time='2099-12-31 23:00:00' where event_id=9942");
    var claim=SmartReminderService.class.getDeclaredMethod("claimEvaluation",Long.class,boolean.class);claim.setAccessible(true);
    claim.invoke(service,19940L,false);
    var self=notice(9940,1);
    check(self.toString().contains("福州")&&self.toString().contains("厦门"),"personal conflict identifies both current and conflicting events");
    check(!self.toString().contains("OTHER_PERSON_SECRET")&&!self.toString().contains("PARENT_PRIVATE_DETAIL")&&!self.toString().contains("MODEL_PRIVATE_TEXT"),"only verified own branch data appears in conflict notice");
    check(mapper.readTree(self.get("payloadJson").toString()).path("conflictingEvents").size()==1,"unknown and duplicate conflict references are rejected");
    ConflictPrivacy.sanitizePersonal(self,1L);
    check(self.get("content").toString().contains("福州"),"own chat sync retains personal conflict detail");
    var shared=new HashMap<>(self);ConflictPrivacy.sanitize(shared);
    check(!shared.toString().contains("福州"),"shared history redacts personal schedule");
    var stranger=new HashMap<>(self);ConflictPrivacy.sanitizePersonal(stranger,2L);
    check(!stranger.toString().contains("福州"),"wrong inbox owner cannot read personal conflict detail");
    check(evaluationInput.contains("9941")&&evaluationInput.contains("福州")&&!evaluationInput.contains("OTHER_PERSON_SECRET")&&!evaluationInput.contains("PARENT_PRIVATE_DETAIL"),"semantic evaluation receives only current recipient's schedule");
    check(evaluationSystem.contains("conflictingEventIds"),"runtime prompt requires concrete conflict references for configured models too");
    check(!db.queryForObject("select content from blade_smart_timeline where event_id=9940 and node_type='TIME_CONFLICT_DETECTED'",String.class).contains("福州"),"shared timeline never republishes private details");
    event(9943,2,1);db.update("update blade_smart_event_branch set task_content='另一项安排' where event_id=9943");
    claim.invoke(service,19943L,false);
    check(notice(9943,1).toString().contains("福州")&&!notice(9943,2).toString().contains("福州"),"recipient gets exact conflict while external sender sees only availability");
    System.out.println("ALL SUMMARY AND PERSONAL CONFLICT 141 REGRESSIONS PASSED");
  }
}
