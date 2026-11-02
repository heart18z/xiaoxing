import org.springblade.modules.smartreminder.service.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** Real service decisions and H2 records; never uses a live model or production data. */
public class EventReplyRegression extends Stage1Regression {
  static java.lang.reflect.Constructor<?> quoted;
  static Map<?,?> reply(long eventId,String json) {
    return tx.execute(status->{
      try{return (Map<?,?>)complete.invoke(service,quoted.newInstance(1L,101L,"本次只回复所选事件",null,eventId,"",List.of(),eventId),json,"");}
      catch(java.lang.reflect.InvocationTargetException e){throw new RuntimeException(e.getCause());}
      catch(Exception e){throw new RuntimeException(e);}
    });
  }
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    quoted=Class.forName("org.springblade.modules.smartreminder.service.SmartReminderService$PreparedChat").getDeclaredConstructor(Long.class,Long.class,String.class,Long.class,Long.class,String.class,List.class,Long.class);
    quoted.setAccessible(true);
    event(9601,1,2);event(9602,1,3);
    var blocked=reply(9601,"{\"intent\":\"stop_event\",\"eventActions\":[{\"eventId\":\"9601\"},{\"eventId\":\"9602\"}]}");
    check(blocked.get("intent").equals("clarify"),"cross-event model action becomes a clear explanation");
    check(count("select count(*) from blade_smart_event where id in(9601,9602) and event_status='ACTIVE'")==2,"cross-event batch has no partial writes");
    for(String intent:List.of("create_event","merge_events","revise_candidate","cancel_candidate"))check(reply(9601,"{\"intent\":\""+intent+"\"}").get("intent").equals("clarify"),"quoted turn cannot "+intent);
    var feedback=(ObjectNode)mapper.readTree("{\"intent\":\"feedback\",\"feedback\":{\"fact\":\"收到\"}}");
    check(EventReplyScope.bind(feedback,9601L)&&feedback.path("feedback").path("eventId").asText().equals("9601"),"missing feedback target is bound to explicit selection");
    var other=(ObjectNode)mapper.readTree("{\"intent\":\"feedback\",\"feedbacks\":[{\"eventId\":\"9602\"}]}");
    check(!EventReplyScope.bind(other,9601L),"feedback cannot target another event");
    check(EventReplyScope.bind(other,null),"legacy unquoted routing remains unchanged");
    reply(9601,"{\"intent\":\"stop_event\",\"eventAction\":{\"reason\":\"已完成\"}}");
    check(count("select count(*) from blade_smart_event where id=9601 and event_status='STOPPED'")==1,"quoted target is used when model omits event ID");
    check(count("select count(*) from blade_smart_event where id=9602 and event_status='ACTIVE'")==1,"other reminders remain active");
    var context=SmartReminderService.class.getDeclaredMethod("allEventContext",Long.class,Long.class);context.setAccessible(true);
    var creator=mapper.readTree((String)context.invoke(service,1L,9602L));
    check(creator.size()==1&&creator.get(0).path("id").asText().equals("9602"),"quoted model context only contains selected event");
    check(mapper.readTree((String)context.invoke(service,2L,9602L)).isEmpty(),"foreign event cannot be used as quote context");
    var ai=new FakeAi();
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),new SmartFileService(db,ai),content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai),null);
    var prepare=SmartReminderService.class.getDeclaredMethod("prepareChat",org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest.class,Long.class,String.class);prepare.setAccessible(true);
    var request=new org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest();request.setContent("改到下周");request.setReplyEventId(9602L);
    long before=count("select count(*) from blade_smart_chat_message where user_id=2");
    try {prepare.invoke(service,request,2L,"chen");throw new AssertionError("unauthorized quote accepted");}
    catch(java.lang.reflect.InvocationTargetException expected){check(expected.getCause() instanceof org.springblade.core.log.exception.ServiceException,"quote authorization fails before model execution");}
    check(count("select count(*) from blade_smart_chat_message where user_id=2")==before,"unauthorized quote creates no chat message");
    Object preparedReply=prepare.invoke(service,request,1L,"creator");
    var row=db.queryForMap("select id,event_id,payload_json from blade_smart_chat_message where user_id=1 and message_role='user' order by id desc limit 1");
    check(((Number)row.get("event_id")).longValue()==9602,"quoted user message stores selected event ID");
    check(mapper.readTree(row.get("payload_json").toString()).path("replyEventSummary").asText().length()>0,"quote summary persists for history rendering");
    check(count("select count(*) from blade_smart_message_event where event_id=9602 and message_id=?",row.get("id"))==1,"quoted user message is linked to event conversation");
    System.out.println("PASS EventReplyRegression");
  }
}
