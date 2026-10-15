import com.fasterxml.jackson.databind.node.*;
import org.springblade.modules.smartreminder.service.*;
import java.util.*;

/** In-memory confirmation, notification, privacy and race regressions; no real users or push service. */
public class AssignmentConflictRegression extends Stage2Regression {
  static ArrayNode plans(String... tasks) {
    var result=mapper.createArrayNode();
    for(String task:tasks) {
      var event=result.addObject().put("summary",task).put("eventTime","2099-03-01 15:00:00").put("firstEvaluateTime","2099-03-01 14:00:00");
      event.putArray("recipientNames").add("出差接收人");
      event.putArray("recipients").addObject().put("id","9302").put("name","出差接收人").put("taskContent",task);
      event.putArray("relatedMessageIds").add("101");
    }
    return result;
  }
  static void candidate(long id,ArrayNode plans) {
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,candidate_status) values(?,9301,101,?,'PENDING')",id,plans.toString());
  }
  static Map<?,?> confirm(long id,boolean accepted)throws Exception {
    return (Map<?,?>)invoke("confirmWithScheduleReviewFor",new Class<?>[]{Long.class,Long.class,boolean.class},9301L,id,accepted);
  }
  public static void main(String[] args)throws Exception {
    Stage2Regression.main(args);
    db.update("insert into blade_user(id,account,name) values(9301,'trip-sender','外出发起人'),(9302,'trip-recipient','出差接收人'),(9303,'uninvolved','无关接收人')");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id,status) values(9301,9301,9302,'ACTIVE')");
    event(9300,3,9302);
    db.update("update blade_smart_event set event_summary='第三方父事件隐私',event_time='2099-03-01 08:00:00' where id=9300");
    db.update("update blade_smart_event_branch set task_content='3月1日去福州出差' where event_id=9300");
    db.update("insert into blade_smart_push_device(id,user_id,secret_hash,environment,device_token,binding_id,enabled,updated_at) values('9302',9302,'fixture-only','sandbox','not-a-real-token','trip-binding',1,now())");
    long id=9100000000000000001L;
    candidate(id,plans("3月1日下午去厦大"));
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"9302\",\"proposedIndex\":0,\"otherEventId\":\"9300\"}]}";
    long before=count("select count(*) from blade_smart_event");
    check(Boolean.TRUE.equals(confirm(id,false).get("confirmationRequired")),"estimated travel overlap requires sender confirmation before assignment");
    check(count("select count(*) from blade_smart_event")==before,"no event or notification is created before consent");
    var stored=mapper.readTree(db.queryForObject("select event_json from blade_smart_candidate where id=?",String.class,id));
    check(!stored.toString().contains("otherEventId")&&!stored.toString().contains("福州"),"sender card excludes other event IDs and recipient private details");
    var accepted=confirm(id,true);
    long created=((Number)((List<?>)accepted.get("eventIds")).get(0)).longValue();
    var notice=db.queryForMap("select id,content,payload_json from blade_smart_chat_message where event_id=? and user_id=9302 and message_type='CONFLICT'",created);
    check(notice.get("content").toString().contains("福州")&&notice.get("content").toString().contains("厦大"),"accepted assignment immediately names both conflicting tasks");
    check(!notice.toString().contains("第三方父事件隐私"),"notification uses the recipient branch task rather than shared parent details");
    check(count("select count(*) from blade_smart_push_delivery where message_id=? and status='PENDING'",notice.get("id"))==1,"conflict push is queued at confirmation without waiting for evaluation time");
    check(count("select count(*) from blade_smart_chat_message where event_id=? and message_type='CONFLICT' and user_id<>9302",created)==0,"no private conflict details sent to sender or unrelated recipients");
    var personal=new HashMap<String,Object>(notice);personal.put("messageType","CONFLICT");
    ConflictPrivacy.sanitizePersonal(personal,9302L);
    check(personal.get("content").toString().contains("福州"),"recipient inbox preserves their specific conflict details");
    var shared=new HashMap<String,Object>(notice);shared.put("messageType","CONFLICT");ConflictPrivacy.sanitize(shared);
    check(!shared.toString().contains("福州")&&!shared.toString().contains("conflictingEvents"),"shared conversation strips cross-event details");
    var stranger=new HashMap<String,Object>(notice);stranger.put("messageType","CONFLICT");ConflictPrivacy.sanitizePersonal(stranger,9301L);
    check(!stranger.toString().contains("福州"),"another viewer cannot use personal conflict payload");
    boolean repeated=false;try{confirm(id,true);}catch(RuntimeException expected){repeated=true;}
    check(repeated&&count("select count(*) from blade_smart_chat_message where event_id=? and message_type='CONFLICT'",created)==1,"double confirmation cannot duplicate conflict notices");

    id++;
    candidate(id,plans("3月1日去西安","3月1日到客户现场"));
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"9302\",\"proposedIndex\":0,\"otherEventId\":\"batch\",\"otherProposedIndex\":1}]}";
    confirm(id,false);var batch=confirm(id,true);
    var ids=(List<?>)batch.get("eventIds");
    for(Object eventId:ids)check(count("select count(*) from blade_smart_chat_message where event_id=? and user_id=9302 and message_type='CONFLICT'",eventId)==1,"each conflicting batch task identifies its counterpart immediately");

    id++;
    candidate(id,plans("3月1日另一项外出任务"));
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"9302\",\"proposedIndex\":0,\"otherEventId\":\"9300\"}]}";
    confirm(id,false);
    db.update("update blade_smart_event_branch set task_content='3月1日改为去杭州出差' where event_id=9300");
    check(Boolean.TRUE.equals(confirm(id,true).get("confirmationRequired")),"changed schedule requires renewed confirmation rather than accepting stale consent");
    db.update("update blade_smart_event_branch set branch_status='STOPPED' where event_id=9300");
    scheduleAnswer="{\"conflicts\":[]}";
    var noConflict=confirm(id,false);Object clearId=((List<?>)noConflict.get("eventIds")).get(0);
    check(count("select count(*) from blade_smart_chat_message where event_id=? and message_type='CONFLICT'",clearId)==0,"removed conflict sends normal assignment without obsolete warnings");

    var review=new SmartScheduleService(db,mapper,new FakeAi());
    scheduleAnswer="{\"conflicts\":[{\"recipientId\":\"9302\",\"proposedIndex\":1,\"otherEventId\":\"batch\",\"otherProposedIndex\":8}]}";
    boolean invalid=false;try{review.review(plans("去西安","去客户现场"),Set.of());}catch(RuntimeException expected){invalid=true;}
    check(invalid,"invented batch task references are rejected");
    System.out.println("ALL ASSIGNMENT CONFLICT REGRESSIONS PASSED");
  }
}
