import org.springblade.modules.smartreminder.service.*;
import java.util.*;

/** Replays both model intent shapes and old-client receipts against isolated persistence. */
public class CompletionRead143Regression extends AttachmentCancellationRegression {
  static void activeSelf(long event) {
    event(event,1,1);
    db.update("update blade_smart_event_branch set next_evaluate_time='2099-10-31 08:30:00',evaluation_requested_at=now(),evaluation_token='stale',evaluate_lock=1 where event_id=?",event);
  }
  public static void main(String[] args) throws Exception {
    AttachmentCancellationRegression.main(args);
    activeSelf(9301);
    String done="劳动关系确认已经完成，不用提醒了";
    as(1,101,9301,done,"{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"9301\",\"summary\":\"劳动关系确认已完成，无需再提醒\",\"fact\":\"已完成\"}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=9301 and branch_status='STOPPED' and next_evaluate_time is null and evaluation_requested_at is null and evaluation_token is null and evaluate_lock=0")==1,"self update closes synchronously and invalidates all pending evaluations");
    check(count("select count(*) from blade_smart_event where id=9301 and event_status<>'ACTIVE'")==1,"self parent event closes with its last branch");
    activeSelf(9302);
    as(1,101,9302,done,"{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\"9302\",\"fact\":\"劳动关系确认已完成\",\"stopBranch\":false}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=9302 and branch_status='STOPPED'")==1,"explicit self completion survives a missing model stop flag");
    long id=9310;
    for(String text:List.of("如果完成了就不用提醒了","还没完成，不用提醒了吗？","部分已完成，不用提醒已完成的部分","已完成一项，其他继续提醒","已经完成了，明天继续提醒","她说“已完成，不用提醒了”","不是已经完成，不用提醒了吗")) {
      activeSelf(id);
      as(1,101,id,text,"{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\""+id+"\",\"fact\":\""+text+"\",\"stopBranch\":false}}");
      check(count("select count(*) from blade_smart_event_branch where event_id=? and branch_status='ACTIVE'",id)==1,"ambiguous/partial completion stays active: "+text);
      id++;
    }
    event(9340,1,2);
    as(1,101,9340,done,"{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"9340\",\"fact\":\"已完成\"}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=9340 and branch_status='ACTIVE'")==1,"self fallback cannot stop someone else's branch without notifying them");

    event(9350,1,2);
    db.update("insert into blade_smart_notification(id,event_id,branch_id,sender_user_id,recipient_user_id,notification_content) values(93501,9350,19350,1,2,'待办'),(93502,9350,29350,1,3,'另一人的待办')");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id) values(29350,9350,3)");
    as(2,101,9350,"收到","{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\"9350\",\"fact\":\"收到\"}}");
    check(count("select count(*) from blade_smart_notification where id=93501 and read_time is not null")==1,"recipient reply records missing read receipt without depending on client version");
    check(count("select count(*) from blade_smart_notification where id=93502 and read_time is null")==1,"reply cannot mark another recipient's reminder read");
    check(count("select count(*) from blade_smart_timeline where event_id=9350 and node_type='NOTIFICATION_READ' and actor_user_id=2")==1,"reply emits an auditable read node");
    check(count("select count(*) from blade_smart_event_branch where id=19350 and branch_status='ACTIVE'")==1,"acknowledging read is not task completion");

    var read=SmartReminderService.class.getDeclaredMethod("markReadFor",Long.class,List.class);read.setAccessible(true);
    db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,event_id,payload_json,is_read) values(93601,2,'assistant','EVENT_ASSIGNED','任务',9350,'{\"branchId\":\"19350\"}',0)");
    read.invoke(service,3L,List.of(93601L));
    check(count("select count(*) from blade_smart_chat_message where id=93601 and is_read=0")==1,"read receipt cannot target another account's assignment");
    long before=count("select count(*) from blade_smart_timeline where event_id=9350 and node_type='NOTIFICATION_READ'");
    read.invoke(service,2L,List.of(93601L));read.invoke(service,2L,List.of(93601L));
    check(count("select count(*) from blade_smart_timeline where event_id=9350 and node_type='NOTIFICATION_READ'")==before+1,"assignment without notification id records one read receipt on repeated requests");

    db.update("insert into blade_user(id,account,name) values(-1,'viewer','发起人')");
    event(9370,-1,2);
    db.update("insert into blade_smart_timeline(id,event_id,branch_id,actor_user_id,node_type,content) values(93701,9370,19370,2,'RECIPIENT_FEEDBACK','收到'),(93702,9370,19370,-1,'RECIPIENT_FEEDBACK','发起人补充')");
    var detail=service.eventDetail(9370L);
    @SuppressWarnings("unchecked") var nodes=(List<Map<String,Object>>)detail.get("timeline");
    check(nodes.stream().anyMatch(n->"93701".equals(n.get("id"))&&Boolean.TRUE.equals(n.get("readConfirmed"))),"historical recipient reply displays read evidence without inventing a read timestamp");
    check(nodes.stream().noneMatch(n->"93702".equals(n.get("id"))&&Boolean.TRUE.equals(n.get("readConfirmed"))),"creator feedback cannot imply recipient reading");
    content.closeSummaryWorker();
    System.out.println("PASS completion/read compatibility regression");
  }
}
