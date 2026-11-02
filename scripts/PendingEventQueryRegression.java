import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.node.*;
import org.springblade.modules.smartreminder.service.*;

/** Offline regression: no production writes and no model/network calls. */
public class PendingEventQueryRegression extends Stage1Regression {
  static final LocalDateTime NOW=LocalDateTime.of(2026,11,2,21,31);
  static ObjectNode item(String summary,String owner,String recipient,String eventTime,String state) {
    var e=mapper.createObjectNode().put("id",summary).put("creatorUserId",owner).put("eventStatus","ACTIVE").put("latestSummary",summary).put("summary",summary);
    if(eventTime!=null)e.put("eventTime",eventTime);
    e.putArray("branches").addObject().put("recipientUserId",recipient).put("branchStatus",state).put("latestSummary",summary).put("taskTimeScoped",0);
    return e;
  }
  static String answer(ArrayNode rows,String question){return PendingEventQuery.reply(rows,902,PendingEventQuery.resolve(question,NOW.toLocalDate()).orElseThrow(),NOW);}
  public static void main(String[] args)throws Exception {
    for(String question:List.of("总结下这个月我还要做的事","我还有哪些事没做？","最近我还有那些事没做","我还有什么事没做","帮我看看本周还有哪些事情没完成","列出我的待办","我还有什么没做","总结下这个月我还要做的事。"))
      check(PendingEventQuery.resolve(question,NOW.toLocalDate()).isPresent(),"pending query recognized: "+question);
    for(String other:List.of("我已完成，不用提醒了","我没有完成","列出待办，再取消会议","帮我总结这个月已完成的事","提醒我明天完成报告","总结陈通这个月还要做的事","总结11月我还要做的事","总结下这个月我还要做的事并下载报告","今天和明天有哪些事没做"))
      check(PendingEventQuery.resolve(other,NOW.toLocalDate()).isEmpty(),"leave non-simple query with model: "+other);
    var range=PendingEventQuery.resolve("总结下这个月我还要做的事",NOW.toLocalDate()).orElseThrow();
    check(range.end().equals(LocalDate.of(2026,11,30)),"remaining month includes future through month end");
    check(PendingEventQuery.resolve("看看本周待办",LocalDate.of(2026,11,8)).orElseThrow().end().equals(LocalDate.of(2026,11,8)),"Sunday does not include next week");
    check(PendingEventQuery.resolve("看看下月待办",LocalDate.of(2026,12,31)).orElseThrow().end().equals(LocalDate.of(2027,1,31)),"next month crosses year");
    var rows=mapper.createArrayNode();
    rows.add(item("准备演示材料","902","902","2026-11-08 09:00:00","ACTIVE"));
    rows.add(item("报名费用未处理","901","902","2026-10-31 09:00:00","ACTIVE"));
    rows.add(item("确认场地","902","903","2026-11-10 09:00:00","ACTIVE"));
    rows.add(item("持续整理资料","902","902",null,"ACTIVE"));
    rows.add(item("明年拜年","902","902","2027-02-01 09:00:00","ACTIVE"));
    rows.add(item("已提交材料","902","902","2026-11-08 09:00:00","COMPLETED"));
    rows.add(item("取消的会议","902","902","2026-11-08 09:00:00","STOPPED"));
    rows.add(item("外部秘密","901","903","2026-11-08 09:00:00","ACTIVE"));
    rows.add(item("主事件已停止","902","902",null,"ACTIVE").put("eventStatus","STOPPED"));
    String reply=answer(rows,"总结下这个月我还要做的事");
    check(reply.contains("**4 项**")&&reply.contains("准备演示材料")&&reply.contains("报名费用未处理")&&reply.contains("确认场地")&&reply.contains("持续整理资料"),"answer lists concrete current tasks");
    check(!reply.contains("明年拜年")&&!reply.contains("已提交材料")&&!reply.contains("取消的会议")&&!reply.contains("外部秘密")&&!reply.contains("主事件已停止"),"future outside month, ended and unauthorized tasks excluded");
    check(reply.contains("我需要处理")&&reply.contains("我发起，待对方处理")&&reply.contains("已过计划时间")&&reply.contains("尚未设置时间"),"roles and overdue/undated tasks clearly distinguished");
    check(answer(rows,"我还有哪些事没做").contains("明年拜年"),"unbounded pending query does not lose future tasks");
    check(answer(mapper.createArrayNode(),"我还有什么没做").contains("没有查到"),"empty result is explicit, not a promise");
    var scoped=item("个人时间优先","901","902","2026-11-08 09:00:00","ACTIVE");
    ((ObjectNode)scoped.path("branches").get(0)).put("taskTimeScoped",1).put("taskEventTime","2026-12-08 09:00:00");
    check(!answer(mapper.createArrayNode().add(scoped),"总结下这个月我还要做的事").contains("个人时间优先"),"recipient scoped date overrides shared date");
    var epoch=item("时间戳事项","902","902",null,"ACTIVE").put("eventTime",LocalDateTime.of(2026,12,8,9,0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
    check(!answer(mapper.createArrayNode().add(epoch),"总结下这个月我还要做的事").contains("时间戳事项"),"Jackson numeric timestamps are filtered in Beijing time");

    Stage1Regression.main(args);
    db.update("insert into blade_user(id,account,name) values(902,'todo-reader','查询者'),(903,'todo-other','其他人')");
    event(97001,902,902);event(97002,903,902);event(97003,903,903);event(97004,902,902);
    db.update("update blade_smart_event set event_summary='具体的测试待办' where id=97001");
    db.update("update blade_smart_event_branch set task_content='具体的测试待办',latest_summary='具体的测试待办' where event_id=97001");
    db.update("update blade_smart_event_branch set task_content='对方发给我的任务',latest_summary='对方发给我的任务' where event_id=97002");
    db.update("update blade_smart_event_branch set task_content='无权查看的秘密',latest_summary='无权查看的秘密' where event_id=97003");
    db.update("update blade_smart_event_branch set branch_status='STOPPED',task_content='已经结束的任务' where event_id=97004");
    long eventsBefore=count("select count(*) from blade_smart_event"),timelineBefore=count("select count(*) from blade_smart_timeline");
    var result=(Map<?,?>)complete.invoke(service,prepared.newInstance(902L,970001L,"总结下这个月我还要做的事",null,null,"",List.of()),"{\"intent\":\"chat\",\"reply\":\"好的，我按本月剩余时间帮你梳理还没完成的事项。\",\"eventActions\":[{\"type\":\"stop_event\",\"eventId\":\"97001\"}]}","");
    String text=result.get("reply").toString();
    System.out.println("DB fixture reply: "+text);
    check(text.contains("具体的测试待办")&&text.contains("对方发给我的任务")&&!text.contains("无权查看的秘密")&&!text.contains("已经结束的任务"),"completeChat replaces empty acknowledgment with authorized DB task list");
    check(count("select count(*) from blade_smart_event")==eventsBefore&&count("select count(*) from blade_smart_timeline")==timelineBefore&&count("select count(*) from blade_smart_event_branch where event_id=97001 and branch_status='ACTIVE'")==1,"pending question cannot create, change or stop events");
    check(count("select count(*) from blade_smart_chat_message where user_id=902 and message_role='assistant' and message_type='TEXT'")==1,"legacy clients receive one normal text reply");
    System.out.println("PASS PendingEventQueryRegression");
  }
}
