import com.fasterxml.jackson.databind.node.*;
import org.springblade.modules.smartreminder.service.*;
import java.util.*;

/** Isolated simulated accounts. No production events or notifications. */
public class ScheduleEvidenceRegression extends Stage2Regression {
  static int calls;
  static String captured;
  static class CaptureAi extends FakeAi {
    @Override public AiAnswer chat(String system,String user) {
      calls++;captured=user;
      return new AiAnswer(scheduleAnswer,"","fixture","");
    }
  }
  static ArrayNode plan(long person,String time) throws Exception {
    return (ArrayNode)mapper.readTree("[{\"summary\":\"下午4点聊天\",\"eventTime\":\""+time+"\",\"recipientNames\":[\"模拟乙\"],\"recipients\":[{\"id\":\""+person+"\",\"name\":\"模拟乙\",\"taskContent\":\"下午4点聊天\"}],\"relatedMessageIds\":[\"101\"]}]");
  }
  public static void main(String[] args) throws Exception {
    Stage2Regression.main(args);
    db.update("insert into blade_user(id,account,name) values(9004,'schedule-a','模拟乙'),(9005,'schedule-b','模拟丙')");
    var schedules=new SmartScheduleService(db,mapper,new CaptureAi());
    var a=plan(9004,"2099-01-02 16:00:00");
    check(schedules.review(a,Set.of()).isEmpty()&&calls==0,"empty recipient calendar never asks model to invent conflicts");
    var batch=a.deepCopy();batch.add(plan(9005,"2099-01-02 16:00:00").get(0));
    check(schedules.review(batch,Set.of()).isEmpty()&&calls==0,"different recipients in one batch are not conflicts");
    event(6200,1,9004);
    db.update("update blade_smart_event set event_time='2099-01-01 15:00:00' where id=6200");
    db.update("update blade_smart_event_branch set task_time_scoped=1,task_event_time='2099-01-02 15:00:00',task_content='下午3点开会' where event_id=6200");
    scheduleAnswer="{\"conflicts\":[]}";
    schedules.review(a,Set.of());
    var data=mapper.readTree(captured);var existing=data.path("existing").get(0);
    check(existing.path("eventTime").asText().equals("2099-01-02 15:00:00"),"review gets recipient-scoped local dates rather than epoch timestamps or parent dates");
    check(existing.path("createdAt").isTextual()&&data.path("timezone").asText().equals("Asia/Shanghai"),"relative historical wording has explicit date and timezone context");
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"9005\",\"otherEventId\":\"batch\"}]}";
    boolean rejected=false;
    try{schedules.review(batch,Set.of());}catch(RuntimeException expected){rejected=true;}
    check(rejected,"model cannot claim batch conflict for a recipient with only one proposed task");
    db.update("update blade_smart_event_branch set branch_status='STOPPED' where event_id=6200");
    check(schedules.review(a,Set.of()).isEmpty(),"stopped branch disappears from current review even after cached results");

    scheduleAnswer="{\"conflicts\":[]}";
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id,status) values(6201,1,9004,'ACTIVE')");
    ((ObjectNode)a.get(0)).putArray("scheduleConflicts").addObject().put("key","old-conflict");
    long candidateId=9000000000000006201L;
    var payload=mapper.createObjectNode().put("candidateId",Long.toString(candidateId)).put("reasoningContent","preserved reasoning");payload.set("events",a);
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,ai_reply,candidate_status) values(?,1,101,?,'是否继续？','PENDING')",candidateId,a.toString());
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,payload_json) values(6201,1,'assistant','CANDIDATE','是否继续？',?)",payload.toString());
    var result=(Map<?,?>)invoke("confirmWithScheduleReviewFor",new Class<?>[]{Long.class,Long.class,boolean.class},1L,candidateId,false);
    check(result.containsKey("eventIds"),"removed conflict allows confirmation without stale approval");
    var row=db.queryForMap("select content,message_type,payload_json from blade_smart_chat_message where id=6201");
    check(!row.get("content").equals("是否继续？")&&row.get("message_type").equals("CANDIDATE_CONFIRMED"),"created card no longer asks an obsolete conflict question");
    var saved=mapper.readTree(row.get("payload_json").toString());
    check(saved.path("reasoningContent").asText().equals("preserved reasoning")&&saved.path("events").get(0).path("scheduleConflicts").isEmpty(),"confirmation refreshes conflict evidence and preserves other message metadata");
    var historical=new HashMap<String,Object>();historical.put("messageType","CANDIDATE_CONFIRMED");
    historical.put("content","模拟乙在拟安排的时间已有其他不同事项，可能无法同时参加。是否仍要继续安排？");
    invoke("refreshConfirmedQuestion",new Class<?>[]{Map.class,ObjectNode.class},historical,payload);
    check(historical.get("content").equals("已确认并创建提醒。"),"legacy confirmed cards render a receipt rather than the obsolete question");
    historical.put("messageType","CANDIDATE");historical.put("content","模拟乙在拟安排的时间已有其他不同事项，可能无法同时参加。是否仍要继续安排？");
    invoke("refreshConfirmedQuestion",new Class<?>[]{Map.class,ObjectNode.class},historical,payload);
    check(historical.get("content").toString().endsWith("是否仍要继续安排？"),"pending conflict question remains actionable");
    System.out.println("ALL SCHEDULE EVIDENCE REGRESSIONS PASSED");
  }
}
