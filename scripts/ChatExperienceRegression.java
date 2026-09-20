import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springblade.modules.smartreminder.service.*;
import java.util.*;
public class ChatExperienceRegression extends Stage1Regression {
 public static void main(String[] args)throws Exception {
  Stage1Regression.main(args);
  var method=SmartReminderService.class.getDeclaredMethod("resolvePerson",Long.class,String.class);method.setAccessible(true);
  List<?> self=(List<?>)method.invoke(service,1L,"发起人");check(self.size()==1,"own display name resolves without friendship");
  check(((Map<?,?>)self.get(0)).get("id").toString().equals("1"),"self resolves to authenticated user");
  db.update("update blade_user set name='发起人' where id=2");
  check(((List<?>)method.invoke(service,1L,"发起人")).size()==2,"same-name friend remains ambiguous, never silently routes to self");
  db.update("update blade_user set name='陈颖' where id=2");
  NewApiClient noExtra=new NewApiClient(null,mapper){@Override public AiAnswer chat(String s,String u){throw new AssertionError("unnecessary model call");}};
  SmartEventContentService fast=new SmartEventContentService(db,mapper,noExtra);
  var event=mapper.readTree("{\"recipientTasks\":[{\"recipientName\":\"我\",\"content\":\"明天14点交标书\"}],\"relatedMessageIds\":[\"101\"]}");
  check(fast.task(event,Map.of("name","发起人","self",true)).equals("明天14点交标书"),"self task reuses main-model result without extra request");
  check(fast.selectContext(1L,101L,event).equals(List.of("101")),"provided context IDs avoid extra model request");
  var alias=mapper.readTree("{\"recipientTasks\":[{\"recipientName\":\"老板\",\"content\":\"明天14点交标书\"}]}");
  check(fast.task(alias,Map.of("name","陈颖","requestedName","老板")).equals("明天14点交标书"),"resolved alias reuses scoped task");
  long before=count("select count(*) from blade_smart_timeline where event_id=11 and node_type='RECIPIENT_FEEDBACK'");
  var reply=call("{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\"11\",\"fact\":\"已经出去了\"}}");
  check(reply.get("reply").toString().contains("已经结束"),"closed feedback returns honest conversational reply");
  check(count("select count(*) from blade_smart_timeline where event_id=11 and node_type='RECIPIENT_FEEDBACK'")==before,"closed event is not silently reopened or modified");
  check(db.queryForObject("select message_type from blade_smart_chat_message where user_id=1 order by id desc limit 1",String.class).equals("TEXT"),"closed feedback is plain assistant text");
  System.out.println("CHAT EXPERIENCE REGRESSIONS PASSED");
 }
}
