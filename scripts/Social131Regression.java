import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.push.PushSchema;
import org.springblade.core.log.exception.ServiceException;
import java.util.*;

/** Isolated H2 data. No real users, email, AI or device notifications. */
public class Social131Regression {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
 public static int findInSet(String key,String values){return values!=null&&Arrays.asList(values.split(",")).contains(key)?1:0;}
 public static void main(String[] args)throws Exception {
  var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:social131;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
  var tx=new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
  db.execute("create alias find_in_set for 'Social131Regression.findInSet'");
  db.execute("create table blade_user(id bigint primary key,account varchar(50),name varchar(50),real_name varchar(50),avatar varchar(200),role_id varchar(50) default '2099000000000000001',is_deleted int default 0)");
  db.execute("create table blade_friendship(id bigint,owner_user_id bigint,friend_user_id bigint,status varchar(20),permission_mode varchar(30))");
  db.execute("create table blade_friend_request(id bigint primary key,applicant_user_id bigint,target_user_id bigint,request_message text,request_type varchar(30),permission_mode varchar(30),request_status varchar(20),create_time timestamp,update_time timestamp)");
  db.execute("create table blade_smart_chat_message(id bigint primary key,user_id bigint,message_role varchar(20),message_type varchar(40),content text,payload_json text,event_id bigint,notification_id bigint,is_read int,create_time timestamp)");
  PushSchema.migrate(db);
  db.update("insert into blade_user(id,account,name,real_name,avatar) values(-1,'owner','','本名','/owner.png'),(2,'friend','好友','好友','/friend.png'),(3,'other','别的发起人','别的发起人','/other.png')");
  db.update("insert into blade_smart_push_device(id,user_id,secret_hash,device_token,environment,binding_id,enabled,updated_at) values('fixture',2,'fixture','fixture','production','binding',1,now())");
  var mapper=new ObjectMapper();
  var content=new SmartEventContentService(db,mapper,null){
   @Override public String latestTask(Long eventId,Long userId){return "my task";}
   @Override public String latestEventSummary(Long eventId,String fallback){return fallback;}
  };
  var service=new SmartReminderService(db,mapper,null,null,null,content,new SmartSocialService(db),null,null);
  Long request=tx.execute(s->service.sendFriendRequest(2L,"hi","MUTUAL"));
  check(db.queryForObject("select count(*) from blade_friend_request",Integer.class)==1,"request persists");
  var notice=db.queryForMap("select * from blade_smart_chat_message");
  check(notice.get("message_type").equals("FRIEND_REQUEST")&&notice.get("user_id").toString().equals("2")&&notice.get("event_id")==null,"request notifies recipient with no event");
  check(notice.get("content").toString().startsWith("本名")&&mapper.readTree(notice.get("payload_json").toString()).path("requestId").asText().equals(request.toString()),"sender real name fallback and exact request id");
  check(db.queryForObject("select count(*) from blade_smart_push_delivery",Integer.class)==1,"one request has one device delivery");
  try{service.sendFriendRequest(2L,"again","MUTUAL");throw new AssertionError("duplicate request");}catch(ServiceException expected){}
  check(db.queryForObject("select count(*) from blade_smart_chat_message",Integer.class)==1,"duplicate request cannot send duplicate notification");
  tx.execute(s->{service.sendFriendRequest(3L,"rollback","MUTUAL");s.setRollbackOnly();return null;});
  check(db.queryForObject("select count(*) from blade_friend_request",Integer.class)==1&&db.queryForObject("select count(*) from blade_smart_chat_message",Integer.class)==1,"request and notification rollback together");
  db.execute("create table blade_smart_event(id bigint primary key,event_no varchar(50),event_summary text,event_time timestamp,deadline_time timestamp,event_status varchar(20),create_time timestamp,creator_user_id bigint)");
  db.execute("create table blade_smart_event_branch(id bigint primary key,event_id bigint,recipient_user_id bigint,branch_status varchar(20),next_evaluate_time timestamp,task_time_scoped int default 0,task_event_time timestamp,task_deadline_time timestamp,current_fact text)");
  db.execute("create table blade_smart_timeline(id bigint,event_id bigint,branch_id bigint,node_type varchar(50),content text,create_time timestamp)");
  db.update("insert into blade_smart_event values(100,'sent','sent task',null,null,'ACTIVE',now(),-1),(101,'received','received task',null,null,'ACTIVE',now(),3)");
  for(int i=0;i<6;i++){
   long id=9007199254740993L+i;
   db.update("insert into blade_user(id,name,avatar) values(?,?,?)",id,"person"+i,"/avatar"+i+".png");
   db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,branch_status) values(?,?,?,'ACTIVE')",1000+i,100,id);
  }
  db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,branch_status) values(2000,101,-1,'ACTIVE'),(2001,101,2,'ACTIVE')");
  var sent=service.listEvents("sent","ACTIVE");
  check(sent.size()==1&&sent.get(0).get("recipientCount").toString().equals("6"),"event retains total recipient count");
  var people=(List<Map<String,Object>>)sent.get(0).get("people");
  check(people.size()==4&&people.get(0).get("id").equals("9007199254740993")&&people.get(0).get("avatar").equals("/avatar0.png"),"four ordered actual avatars and exact 64-bit ids");
  var received=service.listEvents("received","ACTIVE");
  var creators=(List<Map<String,Object>>)received.get(0).get("people");
  check(received.size()==1&&creators.size()==1&&creators.get(0).get("id").equals("3")&&!received.toString().contains("/friend.png"),"received card includes creator only, no other recipient profile");
  System.out.println("ALL SOCIAL 131 REGRESSIONS PASSED");
 }
}
