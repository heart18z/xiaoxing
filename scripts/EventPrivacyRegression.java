import org.springblade.modules.smartreminder.service.*;
import java.util.*;
import java.lang.reflect.*;

/** In-memory fixtures only. AuthUtil's unauthenticated -1 is the test viewer. */
public class EventPrivacyRegression extends Stage1Regression {
  static Object invoke(String name,Class<?>[] types,Object... args)throws Exception{var m=SmartReminderService.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(service,args);}
  public static void main(String[] args)throws Exception{
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    db.update("insert into blade_user(id,account,name) values(-1,'viewer','陈通')");
    event(81,-1,2);event(82,3,2);
    db.update("update blade_smart_event set event_summary='消防紧急抽检-PRIVATE',event_time='2099-01-01 15:00:00' where id=82");
    db.update("update blade_smart_event set source_candidate_id=181 where id=81");
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json) values(181,-1,181,'{}')");
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,event_id) values(181,-1,'user','TEXT','通知陈颖开会',81)");
    String secret="李莹莹安排消防紧急抽检-PRIVATE";
    String payload="{\"timeConflict\":true,\"eventId\":\"81\",\"relatedEventIds\":[\"82\"],\"conflictSummary\":\""+secret+"\"}";
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,payload_json,event_id) values(182,-1,'assistant','CONFLICT',?,?,81),(183,2,'assistant','CONFLICT',?,?,81)",secret,payload,secret,payload);
    db.update("insert into blade_smart_timeline(id,event_id,branch_id,node_type,content,payload_json) values(184,81,10081,'TIME_CONFLICT_DETECTED',?,?),(185,81,10081,'NEXT_EVALUATION_PLANNED','规划下一次评估',?)",secret,payload,payload);
    var noAi=new NewApiClient(null,mapper){@Override public AiAnswer chat(String system,String user){throw new AssertionError("Viewing a conversation must not call AI");}};
    var scoped=new SmartEventContentService(db,mapper,noAi);
    service=new SmartReminderService(db,mapper,noAi,new FakeConfig(),null,scoped,new SmartSocialService(db),new SmartScheduleService(db,mapper,noAi));
    var creator=service.eventConversation(81L,-1L);
    check(!creator.toString().contains("PRIVATE")&&!creator.toString().contains("relatedEventIds"),"creator conversation redacts historical cross-event conflict and metadata");
    check(creator.toString().contains("通知陈颖开会"),"legacy creation source remains visible without AI inference");
    var recipient=service.eventConversation(81L,2L);
    check(!recipient.toString().contains("PRIVATE")&&!recipient.toString().contains("sourceContent"),"viewing recipient conversation does not reveal private conflict or query helper fields");
    check(!service.eventDetail(81L).toString().contains("PRIVATE"),"event detail timeline and diagnostics are private-safe");
    db.update("insert into blade_smart_timeline(id,event_id,branch_id,node_type,content,create_time) values(186,81,10081,'TIME_CONFLICT_DETECTED',?,'2099-01-01 00:00:00')",secret);
    check(!service.listEvents("sent","").toString().contains("PRIVATE"),"event list latest-progress preview also redacts historical conflicts");
    boolean denied=false;try{service.eventConversation(82L,2L);}catch(org.springblade.core.log.exception.ServiceException expected){denied=true;}check(denied,"unrelated event conversation access rejected");
    String recent=(String)invoke("recentConversation",new Class<?>[]{Long.class,Long.class,int.class},-1L,Long.MAX_VALUE,30);
    check(recent.contains("通知陈颖开会"),"privacy test reads actual history before checking redaction");
    check(!recent.contains("PRIVATE"),"legacy conflict is also removed from future LLM chat context");
    String schedules=(String)invoke("recipientScheduleContext",new Class<?>[]{Long.class,Long.class},81L,2L);
    check(!schedules.contains("PRIVATE")&&!schedules.contains("eventId")&&!schedules.contains("eventNo"),"scheduled evaluation only receives anonymous occupancy times");
    Map<String,Object> data=new HashMap<>();data.put("event_id",81L);data.put("creator_user_id",-1L);data.put("recipient_user_id",2L);data.put("recipient_name","陈颖");data.put("event_no","E81");data.put("event_summary","开会");
    invoke("sendTimeConflict",new Class<?>[]{Map.class,Long.class,String.class,String.class,com.fasterxml.jackson.databind.node.ObjectNode.class},data,10081L,secret,secret,mapper.readTree(payload));
    var newest=db.queryForMap("select content,payload_json from blade_smart_chat_message where user_id=-1 order by id desc limit 1");
    check(!newest.toString().contains("PRIVATE")&&!newest.toString().contains("relatedEventIds"),"new creator conflict uses safe server-authored content");
    Map<String,Object> business=new HashMap<>(Map.of("messageType","CONFLICT","content","接收人尚未授权","payloadJson","{\"detail\":\"接收人尚未授权\"}"));ConflictPrivacy.sanitize(business);
    check(business.get("content").equals("接收人尚未授权"),"non-scheduling business conflicts retain actionable explanation");
    System.out.println("ALL EVENT PRIVACY AND CONVERSATION REGRESSIONS PASSED");
  }
}
