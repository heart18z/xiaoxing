import org.springblade.modules.smartreminder.service.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;

/** Real persistence and authorization paths with deterministic AI, no production traffic. */
public class AttachmentCancellationRegression extends Stage1Regression {
  static class DeferredAi extends FakeAi {
    @Override public AiAnswer chat(String system,String user) {
      if(system.startsWith("decision-fixture"))return new AiAnswer("{\"action\":\"DEFER\",\"reason\":\"等待原定时间\",\"nextEvaluateTime\":\"2099-01-01 10:00:00\"}","","fixture","");
      return super.chat(system,user);
    }
  }
  static Map<?,?> as(long user,long message,long event,String text,String answer) {
    return tx.execute(status->{try{return (Map<?,?>)complete.invoke(service,prepared.newInstance(user,message,text,null,event,"",List.of()),answer,"");}
      catch(InvocationTargetException ex){throw new RuntimeException(ex.getCause());}catch(Exception ex){throw new RuntimeException(ex);}});
  }
  static void denied(Runnable action,String label) {boolean denied=false;try{action.run();}catch(RuntimeException ex){denied=true;}check(denied,label);}
  static long create(String name,String recipient,String mode,String ids) throws Exception {
    var result=as(1,101,11,"按照片提醒", "{\"intent\":\"create_event\",\"events\":[{\"summary\":\""+name+"\",\"recipientNames\":[\""+recipient+"\"],\"firstEvaluateTime\":\"2099-01-01 09:00:00\",\"attachmentMode\":\""+mode+"\",\"attachmentIds\":"+ids+"}]}");
    var method=SmartReminderService.class.getDeclaredMethod("confirmWithScheduleReviewFor",Long.class,Long.class,boolean.class);method.setAccessible(true);
    tx.execute(status->{try{return method.invoke(service,1L,Long.valueOf(result.get("candidateId").toString()),false);}catch(Exception ex){throw new RuntimeException(ex);}});
    return db.queryForObject("select id from blade_smart_event where event_summary=?",Long.class,name);
  }
  public static void main(String[] args)throws Exception {
    Stage1Regression.main(args);db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
    var ai=new DeferredAi();service=new SmartReminderService(db,mapper,ai,new FakeConfig(),null,content,new SmartSocialService(db),new SmartScheduleService(db,mapper,ai),null);
    event(7101,1,2);
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content,next_evaluate_time) values(27101,7101,3,'保留参会任务','2099-01-01 12:00:00')");
    call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"7101\",\"cancelRecipientNames\":[\"chen\"]}}");
    check(count("select count(*) from blade_smart_event_branch where event_id=7101 and evaluation_requested_at is not null")==1,"cancellation queues only selected branch immediately");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_event_branch where id=17101 and branch_status='STOPPED' and next_evaluate_time is null")==1,"confirmed cancellation cannot DEFER and stops after notification");
    check(count("select count(*) from blade_smart_notification where event_id=7101 and recipient_user_id=2 and notification_content like '%无需继续执行%'")==1,"cancelled recipient receives explicit cancellation");
    check(count("select count(*) from blade_smart_event_branch where id=27101 and branch_status='ACTIVE' and task_content='保留参会任务'")==1,"unaffected branch retains task and active status");
    check(count("select count(*) from blade_smart_notification where event_id=7101 and recipient_user_id=3")==0,"cancellation is never broadcast to another branch");
    service.evaluateRequestedBranches();check(count("select count(*) from blade_smart_notification where event_id=7101")==1,"completed cancellation is not resent on worker retry");
    event(7102,1,2);db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,next_evaluate_time) values(27102,7102,3,now())");
    call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"7102\",\"keepRecipientNames\":[\"bao\"]}}");service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_event_branch where event_id=7102 and branch_status='ACTIVE' and recipient_user_id=3")==1,"keeping actual tag-resolved account preserves that branch");
    check(count("select count(*) from blade_smart_event_branch where event_id=7102 and branch_status='STOPPED' and recipient_user_id=2")==1,"keep list notifies and stops excluded participants");
    denied(()->call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"7102\",\"keepRecipientNames\":[\"主任\"]}}"),"unresolved role or invented account cannot cancel people");
    denied(()->call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"7102\",\"cancelRecipientNames\":[\"ba\"]}}"),"partial account match is rejected");
    event(7103,1,2);
    call("{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\"7103\",\"cancelRecipientNames\":[\"chen\"]}}");
    db.execute("alter table blade_smart_notification add constraint fail_cancel_delivery check(event_id<>7103)");
    service.evaluateRequestedBranches();
    check(count("select count(*) from blade_smart_event_branch where event_id=7103 and branch_status='ACTIVE'")==1,"notification persistence failure cannot silently stop branch");
    db.execute("alter table blade_smart_notification drop constraint fail_cancel_delivery");

    Path file=Files.createTempFile("event-attachment-", ".txt");Files.writeString(file,"用户提供的文档原文");
    try {
      for(long id:List.of(8101L,8102L,8103L))db.update("insert into blade_smart_file(id,user_id,original_name,stored_name,file_path,extension,extracted_text) values(?,?,?,?,?,'txt',?)",id,id==8102?2:1,"材料"+id+".txt",id+".txt",file.toString(),"原文"+id);
      db.update("update blade_smart_chat_message set payload_json=? where id=101","{\"fileIds\":[\"8101\"]}");
      var attachments=new EventAttachments(db,mapper);
      db.update("insert into blade_smart_chat_message(id,user_id,message_role,content) values(81011,1,'user','把刚才的照片发给陈颖')");
      ObjectNode later=mapper.createObjectNode().put("attachmentMode","DELIVER");later.putArray("relatedMessageIds").add("101");
      attachments.prepare(1L,81011L,later);
      check(attachments.selected(later).equals(List.of(8101L)),"text follow-up preserves explicitly delivered attachments from linked source message");
      long ref=create("车票识别仅提取乘车内容","我","REFERENCE","[\"8101\"]");
      check(attachments.list(1L,ref).isEmpty(),"reference-only ticket never creates attachment delivery grant");
      as(1,101,ref,"已完成","{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\""+ref+"\",\"fact\":\"已完成\",\"stopBranch\":true,\"attachmentMode\":\"REFERENCE\"}}");
      check(count("select count(*) from blade_smart_event_branch where event_id=? and branch_status='STOPPED'",ref)==1,"creator completing own task stops immediately rather than queues participant cancellation");
      check(count("select count(*) from blade_smart_timeline where event_id=? and node_type='BRANCH_CANCEL_REQUESTED'",ref)==0,"own completion is not mislabeled as a cancellation notice");
      long sent=create("把材料交给陈颖","陈颖","DELIVER","[\"8101\"]");
      check(attachments.owner(2L,sent,8101L)==1L,"recipient can view explicitly delivered original file");
      long branch=db.queryForObject("select id from blade_smart_event_branch where event_id=?",Long.class,sent);
      attachments.share(sent,branch,1L,List.of(8101L),"SENT");
      check(count("select count(*) from blade_smart_timeline where event_id=? and node_type='EVENT_ATTACHMENTS'",sent)==1,"replaying a delivery does not duplicate attachment grants");
      denied(()->attachments.owner(3L,sent,8101L),"other users cannot fetch event attachment");
      denied(()->attachments.owner(2L,ref,8101L),"same file cannot be accessed through unrelated event");
      check(count("select count(*) from blade_smart_chat_message where event_id=? and user_id=2 and message_type='EVENT_ASSIGNED' and payload_json like '%attachmentIds%'",sent)==1,"creation card includes delivered attachment button references");
      db.update("insert into blade_smart_chat_message(id,user_id,message_role,content,payload_json) values(81022,2,'user','已完成，拍照反馈','{\"fileIds\":[\"8102\"]}')");
      as(2,81022,sent,"已完成，拍照反馈","{\"intent\":\"feedback\",\"feedback\":{\"eventId\":\""+sent+"\",\"fact\":\"已完成\",\"stopBranch\":true,\"attachmentMode\":\"DELIVER\"}}");
      check(attachments.owner(1L,sent,8102L)==2L,"creator can read completion feedback file after branch stops");
      check(count("select count(*) from blade_smart_chat_message where event_id=? and user_id=1 and message_type='FEEDBACK' and payload_json like '%8102%'",sent)==1,"feedback card contains returned attachment");
      denied(()->attachments.owner(3L,sent,8102L),"feedback attachment stays private to creator and its branch");
      var timeline=db.queryForList("select id,branch_id as branchId,node_type as nodeType,payload_json as payloadJson from blade_smart_timeline where event_id=? order by id",sent);
      attachments.decorateTimeline(1L,sent,timeline);
      var creation=timeline.stream().filter(n->"EVENT_CREATED".equals(n.get("nodeType"))).findFirst().orElseThrow();
      var feedback=timeline.stream().filter(n->"RECIPIENT_FEEDBACK".equals(n.get("nodeType"))).findFirst().orElseThrow();
      check(creation.get("attachmentIds").equals(List.of("8101")),"creation timeline button includes only sent attachments");
      check(feedback.get("attachmentIds").equals(List.of("8102")),"feedback timeline button includes only that feedback's attachments");
      check(timeline.stream().noneMatch(n->"EVENT_ATTACHMENTS".equals(n.get("nodeType"))),"storage grants do not duplicate business timeline entries");
      var unauthorized=db.queryForList("select node_type as nodeType,payload_json as payloadJson from blade_smart_timeline where event_id=?",sent);
      attachments.decorateTimeline(3L,sent,unauthorized);
      check(unauthorized.stream().noneMatch(n->n.containsKey("attachmentIds")),"timeline attachment IDs never bypass viewer authorization");
      denied(()->attachments.prepare(1L,101L,mapper.createObjectNode().set("attachmentIds",mapper.createArrayNode().add("8103"))),"unrelated owned attachment requires current context link");
      var fileService=new SmartFileService(db,ai);
      check(fileService.content(8101L,attachments.owner(2L,sent,8101L)).get("text").equals("原文8101"),"authorized document preview reads text without exposing file path");
      check(Arrays.equals(fileService.download(8101L,1L).getBody(),Files.readAllBytes(file)),"download preserves exact original bytes");
      long updated=create("后续补发材料","陈颖","REFERENCE","[\"8101\"]");
      as(1,101,updated,"补发附件","{\"intent\":\"update_event\",\"eventAction\":{\"eventId\":\""+updated+"\",\"recipientName\":\"陈颖\",\"attachmentMode\":\"DELIVER\"}}");
      check(attachments.owner(2L,updated,8101L)==1L,"attachment-only creator update grants access and queues evaluation");
      check(count("select count(*) from blade_smart_event_branch where event_id=? and evaluation_requested_at is not null",updated)==1,"new attachment triggers immediate assessment");
    } finally {Files.deleteIfExists(file);}
    System.out.println("ALL ATTACHMENT DELIVERY AND CANCELLATION REGRESSIONS PASSED");
  }
}
