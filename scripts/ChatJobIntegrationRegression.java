import java.util.*;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest;

/** Full chat preparation/action path on a detached worker, no request/auth thread context. */
public class ChatJobIntegrationRegression extends Stage1Regression {
  public static void main(String[] args)throws Exception{
    Stage1Regression.main(args);
    db.execute("alter table blade_user add column email varchar(45)");
    db.execute("create table blade_app_chat_job(user_id bigint,request_id varchar(80),payload_hash varchar(64),job_status varchar(16),result_json clob,error_message varchar(240),created_at timestamp,updated_at timestamp,primary key(user_id,request_id))");
    event(9000,1,2);
    var ai=new FakeAi(){
      @Override public AiAnswer chatStream(AiConfigService.AiRuntimeConfig c,String system,String user,java.util.function.Consumer<String> delta,java.util.function.Consumer<String> reasoning){
        reasoning.accept("核对事件权限");return new AiAnswer("{\"intent\":\"stop_event\",\"eventActions\":[{\"type\":\"stop_event\",\"eventId\":\"9000\"}]}","核对事件权限","fixture","");
      }
    };
    var config=new FakeConfig(){@Override public String languageInstruction(Long user){return "";}};
    var actual=new SmartReminderService(db,mapper,ai,config,new SmartFileService(db,ai),new SmartEventContentService(db,mapper,ai),new SmartSocialService(db),new SmartScheduleService(db,mapper,ai));
    var jobs=new ChatJobService(db,mapper,actual,new ChatRunRegistry(),tx.getTransactionManager());
    try{
      var request=new ChatRequest();request.setRequestId("integration000001");request.setContent("停止我的9000事件");jobs.submit(1L,"creator",request);
      for(int i=0;i<400;i++){String state=jobs.status(1L,request.getRequestId()).get("status").toString();if(state.equals("FAILED"))throw new AssertionError("real background action failed");if(state.equals("SUCCEEDED"))break;Thread.sleep(20);}
      check(jobs.status(1L,request.getRequestId()).get("status").equals("SUCCEEDED"),"real detached chat reaches committed success");
      check(count("select count(*) from blade_smart_event where id=9000 and event_status='STOPPED'")==1,"detached action uses explicit authenticated owner");
      check(count("select count(*) from blade_smart_chat_message where user_id=1 and content='停止我的9000事件'")==1,"user message persisted exactly once");
    }finally{jobs.close();}
  }
}
