import org.springblade.modules.smartreminder.service.*;
import java.util.*;

/** In-memory only: no production database, model, or messages. */
public class LiveSummaryRegression extends Stage1Regression {
  static String summaryInput;
  static Runnable duringSummary;
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    db.update("insert into blade_user(id,account,name) values(-1,'viewer','陈通')");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id) values(401,1,-1)");
    event(401,1,-1);
    db.update("update blade_smart_event set original_text='提醒陈通15点开会以及陈颖提交论证报告',event_summary='提醒陈通15点开会以及陈颖提交论证报告',event_time='2099-01-01 15:00:00' where id=401");
    db.update("update blade_smart_event_branch set task_content='15点开会' where event_id=401");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content,next_evaluate_time) values(20401,401,2,'提交论证报告-PRIVATE','2099-01-01 14:00:00')");
    call("""
      {"intent":"update_event","eventAction":{"eventId":"401","recipientName":"陈通","eventTime":"2099-01-01 16:00:00",
      "recipientTasks":[{"recipientName":"陈通","content":"2099-01-01 16:00开会"}]}}
      """);
    check(db.queryForObject("select task_content from blade_smart_event_branch where id=10401",String.class).contains("16:00"),"targeted task is updated immediately");
    check(db.queryForObject("select task_content from blade_smart_event_branch where id=20401",String.class).equals("提交论证报告-PRIVATE"),"partial update preserves other recipient task");
    check(count("select count(*) from blade_smart_event_branch where id=10401 and task_event_time='2099-01-01 16:00:00' and task_time_scoped=1")==1,"targeted time is stored on branch");
    check(count("select count(*) from blade_smart_event where id=401 and event_time='2099-01-01 15:00:00'")==1,"targeted update does not change everyone else's time");
    check(count("select count(*) from blade_smart_event_branch where id=20401 and evaluation_requested_at is not null")==0,"unchanged recipient is not re-evaluated");
    check(count("select count(*) from blade_smart_timeline where branch_id=10401 and node_type='BRANCH_TASK_UPDATED'")==1,"task updates are audited on the affected branch");
    String all=content.latestEventSummary(401L,"");
    check(all.contains("陈通")&&all.contains("16:00")&&all.contains("陈颖")&&all.contains("论证报告"),"creator aggregate retains every latest task");
    var detail=service.eventDetail(401L);
    check(!detail.toString().contains("PRIVATE")&&!detail.toString().contains("论证报告"),"recipient detail contains no other recipient task including raw response fields");
    check(detail.toString().contains("16:00"),"recipient detail shows own updated task and time");
    check(!service.listEvents("received","").toString().contains("PRIVATE"),"recipient event list is also scoped");
    check(db.queryForObject("select original_text from blade_smart_event where id=401",String.class).contains("15点"),"original request is preserved");
    event(402,-1,2);db.update("update blade_smart_event_branch set task_content='参加会议' where event_id=402");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content) values(20402,402,3,'提交报告')");
    check(service.eventDetail(402L).toString().contains("提交报告"),"creator detail includes all branches");
    db.update("update blade_smart_event_branch set latest_summary='旧投影不应覆盖任务：已经完成',current_fact='已完成尾款支付' where id=10401");
    check(content.latestTask(401L,-1L).contains("16:00"),"receipt and old projection cannot replace current task");
    var ai=new FakeAi(){@Override public AiAnswer chat(String system,String user){
      if(system.contains("事件概述")){
        summaryInput=user;if(duringSummary!=null){var hook=duringSummary;duringSummary=null;hook.run();}
        var result=mapper.createObjectNode().put("content","陈通16点开会，陈颖提交论证报告-PRIVATE。");var out=result.putArray("tasks");
        try{for(var item:mapper.readTree(user).path("tasks"))out.addObject().put("branchId",item.path("id").asText()).put("content",item.path("task").asText("待确认"));}catch(Exception e){throw new RuntimeException(e);}
        return new AiAnswer(result.toString(),"","fixture","");
      }
      return super.chat(system,user);
    }};
    content=new SmartEventContentService(db,mapper,ai);
    content.refreshEventOverview(401L);
    check(!summaryInput.contains("已完成尾款支付")&&!summaryInput.contains("旧投影"),"overview input contains current tasks not receipts or old summaries");
    check(summaryInput.contains("2099-01-01 16:00:00"),"model time fields are readable strings, not epoch milliseconds");
    check(content.latestEventSummary(401L,"").equals("陈通16点开会，陈颖提交论证报告-PRIVATE。"),"creator uses compact persisted overview");
    check(!content.refreshEventOverview(401L),"unchanged task overview does not call model again");
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    detail=service.eventDetail(401L);
    check(!detail.toString().contains("PRIVATE")&&!detail.toString().contains("overview_source_hash"),"recipient cannot read organizer overview or cache metadata");
    check(detail.toString().contains("已完成尾款支付"),"latest progress is returned separately from current task");
    duringSummary=()->db.update("update blade_smart_event_branch set task_content='改为17:00开会' where id=10401");
    db.update("update blade_smart_event_branch set task_content='16:30开会' where id=10401");
    content.refreshEventOverview(401L);
    check(content.latestEventSummary(401L,"").contains("17:00"),"stale overview cannot hide a concurrent task update");
    check(!content.latestEventSummary(401L,"").contains("陈通16点"),"old cached overview is rejected immediately");
    db.update("update blade_smart_event_branch set task_content='共同知悉验收结果。',task_time_scoped=0 where event_id=401");
    String grouped=content.latestEventSummary(401L,"");
    check(grouped.indexOf("共同知悉验收结果")==grouped.lastIndexOf("共同知悉验收结果")&&!grouped.contains("。；"),"fallback groups identical tasks and normalizes punctuation");
    var badAi=new FakeAi(){@Override public AiAnswer chat(String s,String u){throw new RuntimeException("fixture outage");}};
    new SmartEventContentService(db,mapper,badAi).refreshEventOverview(401L);
    check(count("select count(*) from blade_smart_event where id=401 and overview_retry_after is not null")==1,"failed overview is retried without overwriting tasks");
    db.update("update blade_smart_event set overview_retry_after=null where id=401");
    var batch=SmartEventContentService.class.getDeclaredMethod("refreshSummaryBatch");batch.setAccessible(true);batch.invoke(content);
    check(count("select count(*) from blade_smart_event where overview_source_hash is not null")>0,"bounded background overview backfill executes");
    var noAi=new NewApiClient(null,mapper){@Override public AiAnswer chat(String s,String u){throw new AssertionError("read API called model");}};
    service=new SmartReminderService(db,mapper,noAi,new FakeConfig(),null,new SmartEventContentService(db,mapper,noAi),new SmartSocialService(db),new SmartScheduleService(db,mapper,noAi));
    var sync=service.syncChatMessages(150,"");String revision=sync.get("revision").toString();
    check(sync.containsKey("messages"),"initial sync sends visible snapshot without calling AI");
    check(!service.syncChatMessages(150,revision).containsKey("messages"),"unchanged sync omits message payload");
    long msg=com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,is_read) values(?,-1,'assistant','REMINDER','实时测试提醒',0)",msg);
    sync=service.syncChatMessages(150,revision);
    check(sync.toString().contains("实时测试提醒"),"new reminder appears on next sync without page navigation");
    check(!sync.toString().contains("其他人的私密聊天"),"sync never includes another user's messages");
    revision=sync.get("revision").toString();
    tx.execute(s->service.clearChatContext());
    sync=service.syncChatMessages(150,revision);
    check(((List<?>)sync.get("messages")).isEmpty(),"context clear on another device is reflected in sync");
    System.out.println("ALL LIVE MESSAGE AND SCOPED SUMMARY REGRESSIONS PASSED");
  }
}
