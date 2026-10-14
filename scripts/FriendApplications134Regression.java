import org.springblade.modules.smartreminder.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Isolated request history; the default test AuthUtil user is -1. */
public class FriendApplications134Regression {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
 public static void main(String[] args){
  var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:applications134;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
  db.execute("create table blade_user(id bigint primary key,account varchar(30),name varchar(50),real_name varchar(50),phone varchar(30),avatar varchar(1000),is_deleted int default 0)");
  db.execute("create table blade_friend_request(id bigint primary key,applicant_user_id bigint,target_user_id bigint,request_status varchar(20),request_type varchar(20),request_message varchar(100),permission_mode varchar(30),create_time timestamp,update_time timestamp)");
  db.execute("create table blade_friendship(owner_user_id bigint,friend_user_id bigint,status varchar(20))");
  db.update("insert into blade_user(id,name,avatar) values(-1,'本人','/me.png'),(2,'收到申请','/incoming.png'),(3,'发出申请','/outgoing.png'),(4,'已通过','/accepted.png'),(5,'已接受','/accepted2.png'),(6,'已删除','/deleted.png')");
  db.update("update blade_user set is_deleted=1 where id=6");
  db.update("insert into blade_friendship values(-1,4,'ACTIVE'),(-1,5,'ACTIVE')");
  long exact=9007199254740993L;
  db.update("insert into blade_friend_request values(?,2,-1,'PENDING','FRIEND','hi','MUTUAL',now(),now()),(2,-1,3,'PENDING','FRIEND','hi','MUTUAL',now(),now()),(3,-1,4,'ACCEPTED','FRIEND','hi','MUTUAL',now(),now()),(4,5,-1,'ACCEPTED','FRIEND','hi','MUTUAL',now(),now()),(5,4,-1,'REJECTED','FRIEND','hi','MUTUAL',now(),now()),(6,-1,5,'CANCELLED','FRIEND','hi','MUTUAL',now(),now()),(7,2,3,'PENDING','FRIEND','other people','MUTUAL',now(),now()),(8,6,-1,'ACCEPTED','FRIEND','deleted','MUTUAL',now(),now()),(9,4,-1,'PENDING','PERMISSION','change','I_CAN_REMIND',now(),now())",exact);
  var service=new SmartReminderService(db,new ObjectMapper(),null,null,null,null,new SmartSocialService(db),null);
  var result=service.friendRequests();
  check(((List<?>)result.get("incoming")).size()==2&&((List<?>)result.get("outgoing")).size()==1,"pending arrays retain old client contracts");
  var rows=(List<Map<String,Object>>)result.get("items");
  check(rows.size()==7,"unified list includes both directions and processed records");
  check(rows.stream().noneMatch(row->Set.of("7","8").contains(row.get("id"))),"unrelated requests and deleted profiles are excluded");
  var incoming=rows.stream().filter(row->row.get("id").equals(Long.toString(exact))).findFirst().orElseThrow();
  check(incoming.get("direction").equals("incoming")&&incoming.get("avatar").equals("/incoming.png"),"incoming person and exact 64-bit id preserved");
  var outgoing=rows.stream().filter(row->row.get("id").equals("2")).findFirst().orElseThrow();
  check(outgoing.get("direction").equals("outgoing")&&outgoing.get("avatar").equals("/outgoing.png"),"outgoing person resolved correctly");
  check(rows.get(0).get("requestStatus").equals("PENDING")&&rows.get(0).get("direction").equals("incoming")&&rows.get(2).get("direction").equals("outgoing"),"pending incoming first, outgoing next, processed last");
  check(rows.stream().filter(row->row.get("requestStatus").equals("ACCEPTED")).allMatch(row->Boolean.TRUE.equals(row.get("friendActive"))),"accepted current friendships return boolean active flag");
  db.update("update blade_friendship set status='REMOVED' where friend_user_id=4");
  rows=(List<Map<String,Object>>)service.friendRequests().get("items");
  check(rows.stream().filter(row->row.get("id").equals("3")).allMatch(row->Boolean.FALSE.equals(row.get("friendActive"))),"history does not report removed friendship as still added");
  db.update("update blade_friend_request set request_status='ACCEPTED',update_time=now() where id=?",exact);
  result=service.friendRequests();
  check(((List<?>)result.get("incoming")).size()==1&&((List<Map<String,Object>>)result.get("items")).stream().anyMatch(row->row.get("id").equals(Long.toString(exact))&&row.get("requestStatus").equals("ACCEPTED")),"processed request remains in history while pending count falls");
 }
}
