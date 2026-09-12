import org.springblade.modules.smartreminder.service.*;
import java.util.*;

/** Isolated original-intent, acknowledgement, cache and privacy regression. */
public class OriginalWordingRegression extends Stage1Regression {
  static String input,prompt;
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    db.update("insert into blade_user(id,account,name) values(-1,'viewer','陈通')");
    event(701,-1,2);
    db.update("update blade_smart_event set original_text='早上',event_time='2026-09-11 18:00:00' where id=701");
    db.update("update blade_smart_event_branch set task_content='请2026-09-11 18:00前往见客户，并提前安排好出行时间。' where event_id=701");
    for(int i=0;i<3;i++){
      String text=List.of("通知陈颖明天7点见客户","早上","改到下午6点吧").get(i);
      db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content) values(?,-1,'user','TEXT',?)",7010+i,text);
      content.link(701L,-1L,7010L+i);
    }
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content) values(7019,-1,'user','TEXT','不相关私密事件-SECRET')");
    var ai=new FakeAi(){@Override public AiAnswer chat(String system,String user){
      input=user;prompt=system;
      return new AiAnswer("{\"content\":\"通知陈颖9月11日下午6点见客户\",\"tasks\":[{\"branchId\":\"10701\",\"content\":\"9月11日下午6点见客户\"}]}","","fixture","");
    }};
    content=new SmartEventContentService(db,mapper,ai);
    content.refreshEventOverview(701L);
    check(input.contains("通知陈颖明天7点见客户")&&input.contains("改到下午6点吧"),"overview sees original creator wording and later change");
    check(!input.contains("SECRET"),"unlinked creator chat is not used as event evidence");
    check(prompt.contains("不得改成分派付款职责")&&prompt.contains("ReminderWording")==false&&prompt.contains("提前安排出行"),"overview explicitly prohibits inferred responsibilities and travel instructions");
    check(content.latestEventSummary(701L,"").equals("通知陈颖9月11日下午6点见客户"),"creator keeps a simple notification sentence");
    check(content.latestTask(701L,2L).equals("9月11日下午6点见客户"),"personal display removes invented instruction using original evidence");
    check(db.queryForObject("select task_content from blade_smart_event_branch where id=10701",String.class).contains("提前安排"),"historical business task is not destructively rewritten");
    check(!content.refreshEventOverview(701L),"unchanged original evidence does not call model again");
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content) values(7020,-1,'user','TEXT','改到下午5点')");content.link(701L,-1L,7020L);
    check(!content.latestEventSummary(701L,"").equals("通知陈颖9月11日下午6点见客户"),"new linked creator message invalidates cached wording");
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    var feedback=SmartReminderService.class.getDeclaredMethod("applyFeedback",Long.class,com.fasterxml.jackson.databind.node.ObjectNode.class,String.class,Long.class);feedback.setAccessible(true);
    var node=mapper.createObjectNode().put("eventId","701").put("fact","同意执行、没有任何冲突，并已完成").put("stopBranch",true);
    tx.execute(s->{try{return feedback.invoke(service,2L,node,"ok",701L);}catch(Exception e){throw new RuntimeException(e);}});
    check(db.queryForObject("select current_fact from blade_smart_event_branch where id=10701",String.class).equals("ok"),"simple acknowledgement is stored verbatim, not as an invented promise");
    check(count("select count(*) from blade_smart_event_branch where id=10701 and branch_status='ACTIVE'")==1,"acknowledgement cannot mark the task completed or stopped");
    db.update("update blade_smart_timeline set content='已确认收到并同意按18点前往，未提出调整需求' where event_id=701 and node_type='RECIPIENT_FEEDBACK'");
    db.update("update blade_smart_event_branch set current_fact='已确认收到并同意按18点前往，未提出调整需求' where id=10701");
    var detail=service.eventDetail(701L);
    check(((List<Map<String,Object>>)detail.get("branches")).get(0).get("currentFact").equals("ok"),"legacy current receipt displays the linked original acknowledgement");
    check(((List<Map<String,Object>>)detail.get("timeline")).stream().filter(t->"RECIPIENT_FEEDBACK".equals(t.get("nodeType"))).allMatch(t->"ok".equals(t.get("content"))),"legacy feedback timeline is readable without rewriting database history");
    var method=SmartReminderService.class.getDeclaredMethod("intentPrompt",AiConfigService.AiRuntimeConfig.class,Long.class);method.setAccessible(true);
    check(method.invoke(service,new FakeConfig().enabledConfig(),-1L).toString().contains(ReminderWording.RULES),"custom intent prompt also receives original-wording rules");
    check(!ReminderWording.isAcknowledgement("ok，但明天不能参加")&&!ReminderWording.isAcknowledgement("都完成了"),"substantive replies are not reduced to acknowledgements");
    System.out.println("ALL ORIGINAL WORDING REGRESSIONS PASSED");
  }
}
