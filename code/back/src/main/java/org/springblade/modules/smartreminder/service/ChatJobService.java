package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ChatRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** HTTP is only a submission/observation channel; disconnects never cancel accepted work. */
@Service
@lombok.extern.slf4j.Slf4j
public class ChatJobService {
  private final JdbcTemplate db;
  private final ObjectMapper mapper;
  private final SmartReminderService reminder;
  private final ChatRunRegistry runs;
  private final TransactionTemplate tx;
  private final ThreadPoolExecutor executor=new ThreadPoolExecutor(4,4,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(24),r->{Thread t=new Thread(r,"app-chat-job");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
  private final ConcurrentHashMap<String,String> thoughts=new ConcurrentHashMap<>();
  public ChatJobService(JdbcTemplate db,ObjectMapper mapper,SmartReminderService reminder,ChatRunRegistry runs,PlatformTransactionManager transactions){this.db=db;this.mapper=mapper;this.reminder=reminder;this.runs=runs;this.tx=new TransactionTemplate(transactions);}

  public Map<String,Object> submit(Long user,String account,ChatRequest input){
    String id=input.getRequestId(),content=Objects.toString(input.getContent(),"").trim();
    if(id==null||!id.matches("[A-Za-z0-9-]{16,80}"))throw new ServiceException("无效的请求编号");
    var files=input.getFileIds()==null?List.<Long>of():List.copyOf(input.getFileIds());
    if(content.length()>20000||files.size()>6||(content.isEmpty()&&files.isEmpty()))throw new ServiceException("请检查消息长度和附件数量");
    ChatRequest request=new ChatRequest();request.setRequestId(id);request.setContent(content);request.setFileIds(files);
    String hash;
    try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(List.of(content,files))));}catch(Exception e){throw new IllegalStateException(e);}
    Boolean created=tx.execute(status->{
      var previous=db.queryForList("select payload_hash from blade_app_chat_job where user_id=? and request_id=?",user,id);
      if(!previous.isEmpty()){if(!hash.equals(previous.get(0).get("payload_hash")))throw new ServiceException("请求编号已用于其他消息");return false;}
      db.queryForObject("select id from blade_user where id=? and is_deleted=0 for update",Long.class,user);
      // Recheck after locking: concurrent retries may have observed the same empty row.
      previous=db.queryForList("select payload_hash from blade_app_chat_job where user_id=? and request_id=? for update",user,id);
      if(!previous.isEmpty()){if(!hash.equals(previous.get(0).get("payload_hash")))throw new ServiceException("请求编号已用于其他消息");return false;}
      if(db.queryForObject("select count(*) from blade_app_chat_job where user_id=? and job_status in ('QUEUED','RUNNING') and created_at>?",Integer.class,user,new java.sql.Timestamp(System.currentTimeMillis()-900000))>0)throw new ServiceException("上一条消息仍在处理，请先等待结果");
      db.update("insert into blade_app_chat_job(user_id,request_id,payload_hash,job_status,created_at,updated_at) values(?,?,?,'QUEUED',now(),now())",user,id,hash);return true;
    });
    if(Boolean.TRUE.equals(created)){
      ChatRunRegistry.Run run=null;
      try{run=runs.register(user,id);final var accepted=run;executor.execute(()->execute(user,account,request,accepted));}
      catch(RuntimeException e){if(run!=null)runs.finish(run);db.update("update blade_app_chat_job set job_status='FAILED',error_message=?,updated_at=now() where user_id=? and request_id=?","服务繁忙，本条消息尚未执行，请稍后重试",user,id);}
    }
    return status(user,id);
  }
  private void execute(Long user,String account,ChatRequest request,ChatRunRegistry.Run run){
    String id=request.getRequestId(),key=user+":"+id;
    try{
      runs.attach(run);
      db.update("update blade_app_chat_job set job_status='RUNNING',updated_at=now() where user_id=? and request_id=?",user,id);
      tx.execute(status->{
        var result=reminder.sendChatStream(request,user,account,ignored->{},delta->thoughts.compute(key,(k,old)->{String next=Objects.toString(old,"")+delta;return next.length()>16000?next.substring(next.length()-16000):next;}));
        try{db.update("update blade_app_chat_job set job_status='SUCCEEDED',result_json=?,updated_at=now() where user_id=? and request_id=?",mapper.writeValueAsString(result),user,id);}catch(Exception e){throw new IllegalStateException(e);}
        return null;
      });
    }catch(Exception e){
      String state=e instanceof CancellationException?"CANCELLED":"FAILED";
      String message=state.equals("CANCELLED")?"已停止生成":"本条消息未处理成功，请稍后重试";
      // Guard the terminal success in case commit succeeded but its acknowledgement was lost.
      try{db.update("update blade_app_chat_job set job_status=?,error_message=?,updated_at=now() where user_id=? and request_id=? and job_status<>'SUCCEEDED'",state,message,user,id);}catch(Exception ignored){log.warn("chat_job_outcome_unknown user={} request={}",user,id);}
      log.warn("chat_job_failed user={} request={} errorType={}",user,id,e.getClass().getSimpleName());
    }finally{thoughts.remove(key);runs.finish(run);}
  }
  public Map<String,Object> status(Long user,String id){
    if(id==null||!id.matches("[A-Za-z0-9-]{16,80}"))throw new ServiceException("无效的请求编号");
    var rows=db.queryForList("select job_status,result_json,error_message,created_at from blade_app_chat_job where user_id=? and request_id=?",user,id);
    if(rows.isEmpty())return Map.of("status","NOT_FOUND","requestId",id);
    var row=rows.get(0);String state=Objects.toString(row.get("job_status"));
    if(List.of("QUEUED","RUNNING").contains(state)&&((java.util.Date)row.get("created_at")).getTime()<System.currentTimeMillis()-900000)state="UNKNOWN";
    Map<String,Object> result=new LinkedHashMap<>();result.put("requestId",id);result.put("status",state);
    result.put("reasoning",thoughts.getOrDefault(user+":"+id,""));
    if(row.get("error_message")!=null)result.put("message",row.get("error_message"));
    if(row.get("result_json")!=null)try{result.put("result",mapper.readValue(row.get("result_json").toString(),Map.class));}catch(Exception e){throw new IllegalStateException(e);}
    return result;
  }
  @PreDestroy public void close(){executor.shutdown();}
}
