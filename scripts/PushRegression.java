package org.springblade.modules.smartreminder.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.sql.Timestamp;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline H2 + fake APNs. Never contacts a real device, database or Apple endpoint. */
public class PushRegression {
  static JdbcTemplate db; static TransactionTemplate tx; static PushDeviceService devices;
  static final String installation=UUID.randomUUID().toString(), secret="a".repeat(64), token="b".repeat(64);
  static final ObjectMapper json=new ObjectMapper();
  static class FakeApns extends ApnsClient {
    int sends; Result next=new Result(200,"Accepted"); Map<String,Object> payload; List<String> ids=new ArrayList<>();
    FakeApns(PushProperties p){super(p,json);}
    @Override public boolean ready(String env){return true;}
    @Override public Result send(String env,String deviceToken,String apnsId,String messageId,Instant expires,Map<String,Object> body){
      check(deviceToken.equals(token),"worker decrypts device token only at delivery"); sends++;payload=body;ids.add(apnsId);return next;
    }
  }
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  static long count(String sql,Object...args){return db.queryForObject(sql,Long.class,args);}
  static String register(long user,String device,String proof){return tx.execute(s->devices.register(user,new PushDeviceService.Registration(device,proof,token,"production","com.dfyj.xiaoxing","zh-cn","1.0"))).get("bindingId").toString();}
  static long message(long id,long user,String role,String type,boolean read){
    return tx.execute(s->{db.update("insert into blade_smart_chat_message values(?,?,?,?,?,?)",id,user,99L,read?1:0,type,"Private report, salary and names");PushOutbox.enqueue(db,id,user,role,type,99L,read);return id;});
  }
  static long job(long message){return db.queryForObject("select id from blade_smart_push_delivery where message_id=?",Long.class,message);}
  static String status(long message){return db.queryForObject("select status from blade_smart_push_delivery where message_id=?",String.class,message);}
  public static void main(String[] args)throws Exception {
    var ds=new DriverManagerDataSource("jdbc:h2:mem:push;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
    PushSchema.migrate(db);PushSchema.migrate(db);
    db.execute("create table blade_user(id bigint primary key,is_deleted int default 0,status int default 1)");
    db.execute("create table blade_smart_chat_message(id bigint primary key,user_id bigint,event_id bigint,is_read int,message_type varchar(40),content text)");
    db.update("insert into blade_user(id) values(1),(2)");
    var p=new PushProperties();p.setEnabled(true);var codec=new SecretCodec("test-only-no-production-secrets");var apns=new FakeApns(p);
    devices=new PushDeviceService(db,codec,p,apns);var worker=new PushDeliveryWorker(db,codec,apns,p);
    try {
      var localExpiry=java.time.LocalDateTime.of(2026,9,11,15,0);
      check(PushDeliveryWorker.expiryInstant(localExpiry).equals(localExpiry.atZone(java.time.ZoneId.systemDefault()).toInstant()),"MySQL LocalDateTime expiry is supported");
      String first=register(1,installation,secret);
      check(first.equals(register(1,installation,secret)),"same login refresh keeps binding stable");
      check(!db.queryForObject("select device_token from blade_smart_push_device",String.class).contains(token),"device token encrypted at rest");
      boolean rejected=false;try{register(2,installation,"c".repeat(64));}catch(RuntimeException e){rejected=true;}
      check(rejected,"other installation secret cannot overwrite binding");
      message(1,1,"user","TEXT",false);message(2,1,"assistant","REMINDER",true);message(3,2,"assistant","REMINDER",false);
      check(count("select count(*) from blade_smart_push_delivery")==0,"no push for user text, read messages or another user's device");
      tx.execute(s->{message(4,1,"assistant","REMINDER",false);s.setRollbackOnly();return null;});
      check(count("select count(*) from blade_smart_chat_message where id=4")==0 && count("select count(*) from blade_smart_push_delivery where message_id=4")==0,"business rollback also rolls back push outbox");
      message(5,1,"assistant","REMINDER",false);worker.deliver(job(5));worker.deliver(job(5));
      check(apns.sends==1 && status(5).equals("SENT"),"claimed delivery sends once across repeated worker runs");
      check(json.writeValueAsString(apns.payload).contains("Private report") && apns.payload.get("recipientUserId").equals("1"),"notification shows this recipient's message content and retains recipient scope");
      check(PushDeliveryWorker.notificationPreview("**报告**\n2026-09-13T16:05").equals("报告 2026-09-13 16:05"),"notification preview removes markdown and ISO separator");
      String bounded=PushDeliveryWorker.notificationPreview("😀".repeat(500));
      check(bounded.codePointCount(0,bounded.length())==401&&!bounded.contains("�"),"long unicode preview is bounded without splitting emoji");
      var emptyPayload=PushDeliveryWorker.payload(Map.of("message_type","REMINDER","event_id",99,"message_id",5,"user_id",1));
      check(json.writeValueAsString(emptyPayload).contains("你有一条新的事件提醒"),"empty message retains nonempty fallback");
      message(6,1,"assistant","FEEDBACK",false);db.update("update blade_smart_chat_message set is_read=1 where id=6");worker.deliver(job(6));
      check(status(6).equals("CANCELLED") && apns.sends==1,"read-before-send suppresses redundant push");
      message(7,1,"assistant","QUESTION",false);String second=register(2,installation,secret);worker.deliver(job(7));
      check(!first.equals(second) && status(7).equals("CANCELLED"),"account switch never redirects old queued messages to new user");
      devices.revoke(new PushDeviceService.Revocation(installation,secret,first));
      check(count("select count(*) from blade_smart_push_device where enabled=1 and user_id=2")==1,"delayed old logout cannot revoke a new account binding");
      devices.revoke(new PushDeviceService.Revocation(installation,"c".repeat(64),second));
      check(count("select count(*) from blade_smart_push_device where enabled=1")==1,"wrong revoke proof cannot disable a device");
      devices.revoke(new PushDeviceService.Revocation(installation,secret,second));
      check(count("select count(*) from blade_smart_push_device where enabled=1")==0,"valid installation proof revokes without OAuth session");
      register(1,installation,secret);message(8,1,"assistant","EVENT_ASSIGNED",false);
      apns.next=new ApnsClient.Result(429,"TooManyRequests");worker.deliver(job(8));
      check(status(8).equals("PENDING"),"APNs throttling schedules retry");
      String stable=apns.ids.get(apns.ids.size()-1);db.update("update blade_smart_push_delivery set next_attempt_at=now() where message_id=8");apns.next=new ApnsClient.Result(200,"Accepted");worker.deliver(job(8));
      check(status(8).equals("SENT") && stable.equals(apns.ids.get(apns.ids.size()-1)),"retry retains APNs ID and reaches sent state");
      message(9,1,"assistant","REMINDER",false);apns.next=new ApnsClient.Result(410,"Unregistered");worker.deliver(job(9));
      check(status(9).equals("FAILED") && count("select count(*) from blade_smart_push_device where enabled=1")==0,"uninstalled device token is retired");
      register(1,installation,secret);message(10,1,"assistant","REMINDER",false);db.update("update blade_smart_push_delivery set expires_at=? where message_id=10",Timestamp.from(Instant.now().minusSeconds(1)));worker.runBatch();
      check(status(10).equals("EXPIRED"),"expired notifications are not delivered late");
      String other=UUID.randomUUID().toString();register(2,other,"d".repeat(64));
      check(count("select count(*) from blade_smart_push_device where enabled=1")==1 && count("select count(*) from blade_smart_push_device where id=? and enabled=0",installation)==1,"reinstall/token reuse disables obsolete installation");
      KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));KeyPair pair=generator.generateKeyPair();
      String jwt=ApnsClient.signJwt("7U8S8PWU2W","RAR492G6T7",pair.getPrivate(),Instant.ofEpochSecond(1700000000));String[] parts=jwt.split("\\.");
      Signature verifier=Signature.getInstance("SHA256withECDSAinP1363Format");verifier.initVerify(pair.getPublic());verifier.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
      check(Base64.getUrlDecoder().decode(parts[2]).length==64 && verifier.verify(Base64.getUrlDecoder().decode(parts[2])),"APNs ES256 uses valid 64-byte JOSE signature");
      check(json.readTree(Base64.getUrlDecoder().decode(parts[1])).path("iat").asLong()==1700000000,"JWT issued-at timestamp is in seconds");
      p.setProductionKeyPath("relative.p8");check(!new ApnsClient(p,json).ready("production"),"missing/non-absolute private key does not enable transport");
      check(!new ApnsClient.Result(400,"BadTopic").retryable(),"permanent APNs configuration errors do not spin retries");
      System.out.println("ALL PUSH REGRESSIONS PASSED");
    } finally {worker.close();}
  }
}
