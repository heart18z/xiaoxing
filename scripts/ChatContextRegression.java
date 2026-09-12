import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.*;
import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;

/** Isolated H2 database and fake models; no production writes or paid AI requests. */
public class ChatContextRegression extends Stage1Regression {
  static Object invoke(String name,Class<?>[] types,Object... args)throws Exception {
    var m=SmartReminderService.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(service,args);
  }
  static String prepare(String input)throws Exception {
    var req=new ChatRequest();req.setContent(input);
    Object result=tx.execute(s->{try{return invoke("prepareChat",new Class<?>[]{ChatRequest.class,Long.class,String.class},req,-1L,"viewer");}catch(Exception e){throw new RuntimeException(e);}});
    var accessor=result.getClass().getDeclaredMethod("userPrompt");accessor.setAccessible(true);return (String)accessor.invoke(result);
  }
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);
    db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    db.update("insert into blade_user(id,account,name) values(-1,'viewer','清除测试')");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id,permission_mode) values(7001,-1,2,'MUTUAL')");
    db.update("insert into blade_smart_person_alias(id,owner_user_id,person_user_id,alias_name) values(7002,-1,2,'老贾')");
    db.update("insert into blade_smart_user_preference(user_id,ai_avatar,language) values(-1,'kept-avatar','en-us')");
    for(int i=0;i<65;i++)event(2000+i,-1,2);
    for(int i=0;i<70;i++){
      event(3000+i,-1,2);
      db.update("update blade_smart_event set event_status=?,update_time=? where id=?",i%2==0?"STOPPED":"COMPLETED",Timestamp.valueOf(LocalDateTime.of(2025,1,1,0,0).plusMinutes(i)),3000+i);
    }
    event(4000,2,-1);event(4001,2,-1);event(4002,2,3);
    db.update("update blade_smart_event_branch set branch_status='STOPPED' where event_id=4001");
    db.update("update blade_smart_event set event_summary='OTHER-PERSON-PRIVATE' where id in(4000,4001,4002)");
    db.update("update blade_smart_event_branch set task_content='我的个人分支任务' where recipient_user_id=-1");
    for(int i=0;i<14;i++)db.update("insert into blade_smart_timeline(id,event_id,branch_id,node_type,content,create_time) values(?,2000,12000,'RECIPIENT_FEEDBACK',?,?)",8000+i,"进展"+i,Timestamp.valueOf(LocalDateTime.of(2025,1,1,0,0).plusMinutes(i)));
    var events=mapper.readTree((String)invoke("allEventContext",new Class<?>[]{Long.class},-1L));
    check(events.size()==116,"all 66 active events plus exactly 50 ended events are included");
    Set<String> ids=new HashSet<>();events.forEach(e->ids.add(e.path("id").asText()));
    check(ids.contains("2000")&&ids.contains("2064")&&ids.contains("4000"),"active events cannot be crowded out by completed events");
    check(!ids.contains("3000")&&ids.contains("3069")&&ids.contains("4001"),"ended limit applies separately and includes ended recipient branch");
    check(!events.toString().contains("OTHER-PERSON-PRIVATE")&&!ids.contains("4002"),"context keeps recipient scoping and excludes unrelated events");
    var first=java.util.stream.StreamSupport.stream(events.spliterator(),false).filter(e->e.path("id").asText().equals("2000")).findFirst().orElseThrow();
    check(first.path("recentTimeline").size()==12&&first.path("recentTimeline").get(0).path("content").asText().equals("进展13"),"each event retains latest 12 timeline entries");
    String fields=first.toString().toLowerCase(Locale.ROOT);
    check(fields.contains("originaltext")&&fields.contains("eventtime")&&fields.contains("deadlinetime")&&fields.contains("latestfact")&&fields.contains("nextevaluatetime"),"event facts and recipient details are retained");
    long oldMessage=IdWorker.getId(),oldCard=IdWorker.getId(),candidate=IdWorker.getId();
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,event_id,is_read) values(?,-1,'assistant','QUESTION','OLD-QUESTION',2000,0),(?,2,'user','TEXT','OTHER-USER-HISTORY',null,0)",oldMessage,oldCard);
    db.update("insert into blade_smart_candidate(id,user_id,source_message_id,event_json,candidate_status) values(?,-1,?,'[{\"eventTime\":\"2099-01-01 15:00:00\"}]','PENDING')",candidate,oldMessage);
    db.update("insert into blade_smart_message_event(event_id,user_id,message_id) values(2000,-1,?)",oldMessage);
    long messagesBefore=count("select count(*) from blade_smart_chat_message"),eventsBefore=count("select count(*) from blade_smart_event");
    String eventBefore=(String)invoke("allEventContext",new Class<?>[]{Long.class},-1L);
    String auditBefore=service.eventConversation(2000L,-1L).toString();
    var cleared=tx.execute(s->service.clearChatContext());
    long boundary=Long.parseLong(cleared.get("contextStartId").toString());
    check(count("select count(*) from blade_smart_chat_message")==messagesBefore&&count("select count(*) from blade_smart_event")==eventsBefore,"clear deletes no messages or events");
    check(service.chatMessages(150).isEmpty(),"cleared history stays empty and legacy event cards cannot reappear");
    check(eventBefore.equals(invoke("allEventContext",new Class<?>[]{Long.class},-1L)),"clearing preserves complete event background and schedules");
    check(auditBefore.equals(service.eventConversation(2000L,-1L).toString()),"event conversation audit still exposes authorized original history");
    check(count("select count(*) from blade_smart_person_alias where owner_user_id=-1")==1&&count("select count(*) from blade_friendship where owner_user_id=-1")==1,"persistent aliases and friendship remain");
    check(count("select count(*) from blade_smart_user_preference where user_id=-1 and language='en-us' and ai_avatar='kept-avatar'")==1,"clear preserves user settings");
    check(invoke("latestFocusedEvent",new Class<?>[]{Long.class},-1L)==null&&invoke("latestPendingCandidate",new Class<?>[]{Long.class},-1L)==null,"old focus and unconfirmed candidate are cleared");
    check(content.contextWindow(-1L,Long.MAX_VALUE).isEmpty(),"new event creation context cannot link pre-clear messages");
    check(invoke("pendingReplyContext",new Class<?>[]{Long.class,Long.class},-1L,Long.MAX_VALUE).equals("[]"),"old questions cannot be implicitly answered after clear");
    boolean denied=false;try{tx.execute(s->service.confirmWithScheduleReview(candidate,true));}catch(RuntimeException e){denied=true;}
    check(denied,"server rejects old unconfirmed action even if directly submitted");
    check(count("select count(*) from blade_smart_chat_message where user_id=2 and content='OTHER-USER-HISTORY'")==1,"another user's history is unchanged");
    long last=0;for(int i=0;i<45;i++){last=IdWorker.getId();db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,is_read) values(?,-1,?,'TEXT',?,1)",last,i%2==0?"user":"assistant","NEW-"+i);}
    long current=IdWorker.getId();db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content) values(?,-1,'user','TEXT','CURRENT')",current);
    var lines=(List<?>)invoke("recentConversationLines",new Class<?>[]{Long.class,Long.class,int.class},-1L,current,40);
    check(lines.size()==40&&lines.get(0).toString().contains("NEW-5")&&lines.get(39).toString().contains("NEW-44"),"rolling window uses latest 40 messages, excludes current input");
    check(!lines.toString().contains("OLD-QUESTION")&&service.chatMessages(150).size()==46,"refresh only returns post-clear messages");
    var ai=new FakeAi();var large=new FakeConfig(){@Override public AiRuntimeConfig enabledConfig(){return new AiRuntimeConfig(1L,"fixture","","","fixture",1048576,1048576,1024,java.math.BigDecimal.ONE,"",false,1000,"intent-fixture","decision-fixture",true);}};
    service=new SmartReminderService(db,mapper,ai,large,new SmartFileService(db,ai),content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    String prompt=prepare("清除后的新问题");
    check(prompt.contains("老贾")&&prompt.contains("MUTUAL")&&prompt.contains("事件2000")&&prompt.contains("我的个人分支任务"),"actual next request always carries people, aliases, permissions and events");
    check(!prompt.contains("OLD-QUESTION")&&prompt.contains("清除后的新问题"),"actual next prompt excludes archived chat while keeping current input");
    var tiny=new AiConfigService.AiRuntimeConfig(1L,"small","","","small",5000,1000,100,java.math.BigDecimal.ZERO,"",false,1000,"","",true);
    String fitted=(String)invoke("fitChatPrompt",new Class<?>[]{String.class,List.class,String.class,String.class,AiConfigService.AiRuntimeConfig.class},"BACKGROUND",List.of("OLD".repeat(400),"RECENT"),"CURRENT","SYSTEM",tiny);
    check(fitted.contains("BACKGROUND")&&fitted.contains("RECENT")&&fitted.contains("CURRENT")&&!fitted.contains("OLD"),"overflow drops oldest whole messages, never slices background");
    denied=false;try{invoke("fitChatPrompt",new Class<?>[]{String.class,List.class,String.class,String.class,AiConfigService.AiRuntimeConfig.class},"BACKGROUND".repeat(400),List.of(),"CURRENT","SYSTEM",tiny);}catch(InvocationTargetException e){denied=e.getCause() instanceof org.springblade.core.log.exception.ServiceException;}
    check(denied,"oversized essential background fails explicitly instead of silently losing events");
    var runs=new ChatRunRegistry();var run=runs.register(-1L,"context-clear-active-test");check(runs.hasActive(-1L)&&!runs.hasActive(2L),"clear guard sees active requests only for this user");runs.finish(run);check(!runs.hasActive(-1L),"clear guard releases after generation ends");
    check(boundary>oldMessage&&last>boundary,"persisted boundary orders old and new records");
    System.out.println("ALL CHAT CONTEXT REGRESSIONS PASSED");
  }
}
