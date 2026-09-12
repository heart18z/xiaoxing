import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springblade.modules.smartreminder.service.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/** Isolated regression runner. H2 only; never contacts production or a live AI service. */
public class Stage1Regression {
  static final ObjectMapper mapper = new ObjectMapper();
  static JdbcTemplate db;
  static TransactionTemplate tx;
  static SmartReminderService service;
  static SmartEventContentService content;
  static Constructor<?> prepared;
  static Method complete;
  static boolean stopDuringEvaluation, decisionWasCalled;
  static Runnable duringEvaluation;
  static String scheduleAnswer="{\"conflicts\":[]}";
  static class FakeConfig extends AiConfigService {
    FakeConfig() { super(db,null); }
    @Override public AiRuntimeConfig enabledConfigForUser(Long userId,String type) { return enabledConfig(); }
    @Override public AiRuntimeConfig enabledConfig() { return new AiRuntimeConfig(1L,"fixture","","","fixture",65536,65536,1024,java.math.BigDecimal.ONE,"xhigh",false,1000,"","decision-fixture",true); }
  }
  static class FakeAi extends NewApiClient {
    FakeAi() { super(null, mapper); }
    @Override public AiAnswer chat(AiConfigService.AiRuntimeConfig config,String system,String user) { return chat(system,user); }
    @Override public AiAnswer chat(String system, String user) {
      if(system.contains("创建前日程语义审查器"))return new AiAnswer(scheduleAnswer,"","fixture","");
      if(system.startsWith("decision-fixture")) {
        decisionWasCalled=true;
        if(duringEvaluation!=null){Runnable hook=duringEvaluation;duringEvaluation=null;hook.run();}
        if(stopDuringEvaluation)call("{\"intent\":\"stop_event\",\"eventAction\":{\"eventId\":\"51\"}}");
        return new AiAnswer("{\"action\":\"SEND\",\"reminderContent\":\"这是测试提醒\",\"nextEvaluateTime\":\"2099-01-01 10:00:00\"}","","fixture","");
      }
      if(system.contains("historicalNotifications")||system.contains("历史通知严格"))return new AiAnswer("{\"tasks\":[],\"notifications\":[{\"messageId\":\"150\",\"content\":\"发起人提醒你：昨天准备首付款申请\"}]}","","fixture","");
      return new AiAnswer(system.contains("messageIds") ? "{\"messageIds\":[\"91\",\"93\",\"101\",\"999\"]}" : "{\"content\":\"请确认你的安排。\"}", "", "fixture", "");
    }
  }
  static void check(boolean value, String name) {
    if (!value) throw new AssertionError(name);
    System.out.println("PASS " + name);
  }
  static long count(String sql, Object... args) { return db.queryForObject(sql, Long.class, args); }
  static void event(long id,long owner,long recipient) {
    db.update("insert into blade_smart_event(id,event_no,creator_user_id,original_text,event_summary) values(?,?,?,?,?)",id,"E"+id,owner,"要求"+id,"事件"+id);
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,next_evaluate_time) values(?,?,?,now())",id+10000,id,recipient);
  }
  static Map<?,?> call(String json) {
    return tx.execute(status -> {
      try { return (Map<?,?>) complete.invoke(service, prepared.newInstance(1L,101L,"合并或停止所选事件",null,11L,""), json, ""); }
      catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
      catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    });
  }
  static void reject(String json,String name) {
    boolean failed=false;
    try { call(json); } catch(RuntimeException e) { failed=true; System.out.println("EXPECTED "+e.getMessage()); }
    check(failed,name);
  }
  public static void main(String[] args) throws Exception {
    var ds=new DriverManagerDataSource("jdbc:h2:mem:stage1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
    org.springblade.modules.smartreminder.push.PushSchema.migrate(db);
    String schema=Files.readString(Path.of("sql/ai_reminder_v1.sql"));
    Matcher tables=Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS.*?\\) ENGINE=.*?;").matcher(schema);
    while(tables.find()) {
      String ddl=tables.group().replaceAll("(?s) ENGINE=.*?;$", ";")
        .replaceAll("(?m)^\\s*(?:UNIQUE )?KEY[^\\r\\n]*[\\r\\n]+", "")
        .replaceAll(",\\s*\\)", ")");
      db.execute(ddl);
    }
    db.execute("alter table blade_smart_event add conversation_context_json text");
    for(String column:List.of("overview_summary text","overview_source_hash varchar(64)","overview_updated_at timestamp(6)","overview_retry_after timestamp"))db.execute("alter table blade_smart_event add "+column);
    db.execute("alter table blade_smart_event_branch add task_content text");
    for(String column:List.of("latest_summary text","summary_source_hash varchar(64)","summary_updated_at timestamp(6)","summary_retry_after timestamp","task_time_scoped int default 0","task_event_time timestamp","task_deadline_time timestamp"))db.execute("alter table blade_smart_event_branch add "+column);
    db.execute("create table blade_smart_message_event(event_id bigint,user_id bigint,message_id bigint,primary key(event_id,user_id,message_id))");
    db.execute("create table blade_smart_person_alias(id bigint primary key,owner_user_id bigint,person_user_id bigint,alias_name varchar(100),source_message_id bigint,create_time datetime,update_time datetime,unique(owner_user_id,alias_name))");
    db.execute("create table blade_smart_user_preference(user_id bigint primary key,ai_avatar varchar(1000),language varchar(10) default 'zh-cn',chat_context_start_id bigint not null default 0,update_time datetime)");
    db.execute("create table blade_user(id bigint primary key,account varchar(50),name varchar(50),real_name varchar(50),phone varchar(50),avatar varchar(100),is_deleted int default 0)");
    db.update("insert into blade_user(id,account,name) values(1,'creator','发起人'),(2,'chen','陈颖'),(3,'bao','包诗琴')");
    db.update("insert into blade_friendship(id,owner_user_id,friend_user_id) values(1,1,2),(2,1,3)");
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,content) values(91,1,'user','陈颖进场，包诗琴准备付款'),(92,1,'user','无关闲聊'),(93,1,'assistant','是否合并为一个事件？'),(101,1,'user','是的'),(999,2,'user','其他人的私密聊天')");
    FakeAi ai=new FakeAi();content=new SmartEventContentService(db,mapper,ai);
    service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    Class<?> type=Class.forName("org.springblade.modules.smartreminder.service.SmartReminderService$PreparedChat");
    prepared=type.getDeclaredConstructors()[0];prepared.setAccessible(true);
    complete=SmartReminderService.class.getDeclaredMethod("completeChat",type,String.class,String.class);complete.setAccessible(true);
    event(11,1,2);event(12,1,3);
    var receipt=call("{\"intent\":\"stop_event\",\"reply\":\"虚假模型回复\",\"eventActions\":[{\"type\":\"stop_event\",\"eventId\":\"11\"},{\"type\":\"stop_event\",\"eventId\":\"12\"}]}");
    check(count("select count(*) from blade_smart_event where id in(11,12) and event_status='STOPPED'")==2,"batch stops every event");
    check(count("select count(*) from blade_smart_event_branch where event_id in(11,12) and next_evaluate_time is null and branch_status='STOPPED'")==2,"batch disables every scheduled branch");
    check(receipt.get("reply").toString().contains("2 个事件")&&!receipt.get("reply").toString().contains("虚假"),"receipt reflects executed operations");
    event(21,1,2);event(22,2,3);
    reject("{\"intent\":\"stop_event\",\"eventActions\":[{\"eventId\":\"21\"},{\"eventId\":\"22\"}]}","unauthorized batch rejected");
    check(count("select count(*) from blade_smart_event where id=21 and event_status='ACTIVE'")==1,"no partial stop on authorization failure");
    event(23,1,3);
    reject("{\"intent\":\"stop_event\",\"eventActions\":[{\"type\":\"stop_event\",\"eventId\":\"21\"},{\"type\":\"update_event\",\"eventId\":\"23\",\"recipientNames\":[\"不存在的好友\"]}]}","second operation failure rejected");
    check(count("select count(*) from blade_smart_event where id=21 and event_status='ACTIVE'")==1,"transaction rolls back earlier successful operation");
    event(31,1,2);event(32,1,3);
    String merge="""
      {"intent":"merge_events","eventAction":{"sourceEventIds":["31","32"]},"events":[{"summary":"陈颖进场与包诗琴准备付款","recipientNames":["陈颖","包诗琴"],"recipientTasks":[{"recipientName":"陈颖","content":"今天14点进场开工"},{"recipientName":"包诗琴","content":"准备项目首付款申请"}],"firstEvaluateTime":"2099-01-01 09:00:00"}]}
      """;
    long activeBefore=count("select count(*) from blade_smart_event where event_status='ACTIVE'");
    Map<?,?> merged=call(merge);long mergedId=Long.parseLong(merged.get("eventId").toString());
    check(count("select count(*) from blade_smart_event where id in(31,32) and event_status='STOPPED'")==2,"merge stops both sources");
    check(count("select count(*) from blade_smart_event where event_status='ACTIVE'")==activeBefore-1,"merge creates exactly one replacement");
    check(count("select count(*) from blade_smart_event_branch where event_id=?",mergedId)==2,"merge retains both recipients");
    check(count("select count(*) from blade_smart_timeline where node_type='EVENT_MERGED'")==4,"merge records bidirectional links");
    String bao=db.queryForObject("select content from blade_smart_chat_message where event_id=? and user_id=3 and message_type='EVENT_ASSIGNED'",String.class,mergedId);
    check(bao.contains("首付款")&&!bao.contains("陈颖")&&!bao.contains("进场"),"recipient notification contains only own task");
    var selected=content.selectContext(1L,101L,mapper.createObjectNode());
    check(selected.containsAll(List.of("91","93","101"))&&!selected.contains("92")&&!selected.contains("999"),"AI selects relevant original messages within authorized context window");
    check(count("select count(*) from blade_smart_message_event where event_id=? and message_id in(91,93,101)",mergedId)==3,"creation links original request, clarification and confirmation");
    var notification=db.queryForMap("select id,message_type as messageType,event_id as eventId,content,payload_json as payloadJson from blade_smart_chat_message where event_id=? and user_id=3 and message_type='EVENT_ASSIGNED'",mergedId);
    db.update("update blade_smart_event_branch set task_content='新的安排' where event_id=? and recipient_user_id=3",mergedId);
    content.personalizeAssignment(notification,3L);
    check(notification.get("content").equals(bao),"later task edits do not rewrite original notification");
    String originalLegacy="通知陈颖昨天14点进场，并让包诗琴昨天准备首付款申请";
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,event_id) values(150,3,'assistant','EVENT_ASSIGNED',?,?)",originalLegacy,mergedId);
    content.prepareAssignments(3L);
    var legacy=db.queryForMap("select id,message_type as messageType,event_id as eventId,content,payload_json as payloadJson from blade_smart_chat_message where id=150");
    content.personalizeAssignment(legacy,3L);
    check(legacy.get("content").toString().contains("昨天")&&!legacy.get("content").toString().contains("陈颖"),"legacy projection retains original time and excludes other recipient");
    check(db.queryForObject("select content from blade_smart_chat_message where id=150",String.class).equals(originalLegacy),"historical source message is never overwritten");
    event(41,1,2);event(42,1,3);
    db.execute("alter table blade_smart_event_branch add constraint fail_new_branch check(event_id<=100000 or event_id="+mergedId+")");
    reject(merge.replace("\"31\"","\"41\"").replace("\"32\"","\"42\""),"merge failure during new-branch creation rejected");
    check(count("select count(*) from blade_smart_event where id in(41,42) and event_status='ACTIVE'")==2,"failed merge restores both original events");
    event(51,1,2);db.update("update blade_smart_event_branch set task_content='本人事项' where event_id=51");
    Method evaluate=SmartReminderService.class.getDeclaredMethod("claimEvaluation",Long.class,boolean.class);evaluate.setAccessible(true);
    stopDuringEvaluation=true;evaluate.invoke(service,10051L,false);
    check(decisionWasCalled&&count("select count(*) from blade_smart_notification where event_id=51")==0,"AI decision finishing after stop cannot send an old reminder");
    check(count("select count(*) from blade_smart_event_branch where event_id=51 and next_evaluate_time is null")==1,"stale evaluation cannot restore stopped schedule");
    stopDuringEvaluation=false;event(52,1,2);db.update("update blade_smart_event_branch set task_content='本人事项' where event_id=52");
    evaluate.invoke(service,10052L,false);
    check(count("select count(*) from blade_smart_notification where event_id=52")==1,"active evaluation still sends its notification transactionally");
    System.out.println("ALL STAGE 1 REGRESSIONS PASSED");
  }
}
