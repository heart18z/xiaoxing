import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatJobRegression {
  public static void main(String[] args)throws Exception{
    var source=new DriverManagerDataSource("jdbc:h2:mem:jobs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000","sa","");
    // Match MySQL Connector/J's DATETIME values, not H2's Timestamp-only shape.
    var db=new JdbcTemplate(source){
      @Override public List<Map<String,Object>> queryForList(String sql,Object... args){
        var rows=super.queryForList(sql,args);
        for(var row:rows)row.replaceAll((key,value)->value instanceof java.sql.Timestamp stamp?stamp.toLocalDateTime():value);
        return rows;
      }
    };var tm=new DataSourceTransactionManager(source);
    db.execute("create table blade_user(id bigint primary key,is_deleted int default 0)");db.update("insert into blade_user(id) values(1),(2)");
    db.execute("create table blade_app_chat_job(user_id bigint,request_id varchar(80),payload_hash varchar(64),job_status varchar(16),result_json clob,error_message varchar(240),created_at timestamp,updated_at timestamp,primary key(user_id,request_id))");
    db.execute("create table effects(user_id bigint,note varchar(80))");
    var calls=new AtomicInteger();var gate=new CountDownLatch(1);var started=new CountDownLatch(1);
    var cancelGate=new CountDownLatch(1);var cancelStarted=new CountDownLatch(1);
    var fake=new SmartReminderService(db,new ObjectMapper(),null,null,null,null,null,null){
      @Override public Map<String,Object> sendChatStream(ChatRequest r,Long user,String account,java.util.function.Consumer<String> reply,java.util.function.Consumer<String> reasoning){
        calls.incrementAndGet();db.update("insert into effects values(?,?)",user,r.getContent());
        if(r.getContent().equals("slow")){reasoning.accept("正在核对");started.countDown();try{gate.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new RuntimeException(e);}}
        if(r.getContent().equals("cancel")){cancelStarted.countDown();try{cancelGate.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new RuntimeException(e);}}
        ChatRunRegistry.check();
        if(r.getContent().equals("fail"))throw new IllegalStateException("fixture failure after write");
        ChatRunRegistry.beginActions();return Map.of("reply","已处理");
      }
    };
    var runs=new ChatRunRegistry();var jobs=new ChatJobService(db,new ObjectMapper(),fake,runs,tm);
    try{
      var request=request("1111111111111111","slow");jobs.submit(1L,"fixture",request);started.await(2,TimeUnit.SECONDS);
      check(jobs.status(2L,request.getRequestId()).get("status").equals("NOT_FOUND"),"status is owner scoped");
      check(jobs.status(1L,request.getRequestId()).get("status").equals("RUNNING"),"running outcome visible without holding HTTP connection");
      var foreignFrames=new ArrayList<Map<String,Object>>();jobs.observe(2L,request.getRequestId(),foreignFrames::add);
      check(foreignFrames.size()==1&&((Map<?,?>)foreignFrames.get(0).get("data")).get("status").equals("NOT_FOUND"),"observer cannot read another owner's reasoning");
      var snapshots=new ArrayList<Map<String,Object>>();
      try{jobs.observe(1L,request.getRequestId(),frame->{snapshots.add(frame);throw new java.io.UncheckedIOException(new java.io.IOException("observer disconnected"));});throw new AssertionError("observer should disconnect");}catch(java.io.UncheckedIOException expected){}
      check(snapshots.get(0).get("reasoning").equals("正在核对"),"reasoning visible while business transaction remains uncommitted");
      check(db.queryForObject("select count(*) from effects",Integer.class)==0,"observer never exposes a success before transaction commit");
      jobs.submit(1L,"fixture",request);gate.countDown();await(jobs,request.getRequestId());
      check(calls.get()==1&&db.queryForObject("select count(*) from effects",Integer.class)==1,"retry never repeats mutation");
      var completedFrames=new ArrayList<Map<String,Object>>();jobs.observe(1L,request.getRequestId(),completedFrames::add);
      check(((Map<?,?>)completedFrames.get(0).get("data")).get("status").equals("SUCCEEDED"),"disconnect does not cancel work; reconnect returns committed outcome");
      jobs.submit(1L,"fixture",request);check(calls.get()==1,"completed retry returns saved outcome");
      try{jobs.submit(1L,"fixture",request(request.getRequestId(),"changed"));throw new AssertionError("id reused with changed payload");}catch(org.springblade.core.log.exception.ServiceException expected){}
      jobs.submit(1L,"fixture",request("2222222222222222","fail"));await(jobs,"2222222222222222");
      check(jobs.status(1L,"2222222222222222").get("status").equals("FAILED"),"failure durable");
      check(db.queryForObject("select count(*) from effects",Integer.class)==1,"failed mutation rolled back");
      jobs.submit(1L,"fixture",request("4444444444444444","cancel"));cancelStarted.await(2,TimeUnit.SECONDS);
      check(!runs.stop(2L,"4444444444444444"),"another user cannot stop the job");
      check(runs.stop(1L,"4444444444444444"),"owner can stop before actions");cancelGate.countDown();await(jobs,"4444444444444444");
      check(jobs.status(1L,"4444444444444444").get("status").equals("CANCELLED"),"cancellation stored durably");
      check(db.queryForObject("select count(*) from effects",Integer.class)==1,"cancelled writes rolled back");
      var restarted=new ChatJobService(db,new ObjectMapper(),fake,new ChatRunRegistry(),tm);
      try{check(restarted.status(1L,request.getRequestId()).get("status").equals("SUCCEEDED"),"result survives service restart");}finally{restarted.close();}
      db.update("insert into blade_app_chat_job values(1,'3333333333333333','fixture','RUNNING',null,null,?,now())",new java.sql.Timestamp(System.currentTimeMillis()-1000000));
      check(jobs.status(1L,"3333333333333333").get("status").equals("UNKNOWN"),"stale execution is not falsely marked success or retried");
      System.out.println("PASS durable result, ownership, duplicate suppression, payload mismatch, rollback, restart and uncertain outcome");
    }finally{gate.countDown();cancelGate.countDown();jobs.close();}
  }
  static ChatRequest request(String id,String content){var r=new ChatRequest();r.setRequestId(id);r.setContent(content);return r;}
  static void await(ChatJobService jobs,String id)throws Exception{for(int i=0;i<200;i++){if(Set.of("SUCCEEDED","FAILED","CANCELLED").contains(jobs.status(1L,id).get("status")))return;Thread.sleep(20);}throw new AssertionError("job timed out");}
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
