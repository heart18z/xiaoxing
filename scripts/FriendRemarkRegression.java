import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.core.log.exception.ServiceException;
import java.util.*;

/** Offline fixtures only; the HTTP endpoint additionally requires AppRoleGuard. */
public class FriendRemarkRegression {
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static int findInSet(String needle,String values){return values!=null&&Arrays.asList(values.split(",")).contains(needle)?1:0;}
  public static void main(String[] args)throws Exception{
    var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:friends;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","sa",""));
    db.execute("create alias find_in_set for 'FriendRemarkRegression.findInSet'");
    db.execute("create table blade_user(id bigint primary key,account varchar(45),name varchar(50),real_name varchar(50),phone varchar(20),email varchar(100),avatar varchar(1000),role_id varchar(100),is_deleted int)");
    db.execute("create table blade_friendship(id bigint primary key,owner_user_id bigint,friend_user_id bigint,friend_remark varchar(100),permission_mode varchar(30),status varchar(20),update_time timestamp)");
    db.execute("create table blade_friend_request(id bigint,applicant_user_id bigint,target_user_id bigint,request_status varchar(20))");
    db.execute("create table blade_smart_person_alias(id bigint,owner_user_id bigint,person_user_id bigint,alias_name varchar(100),source_message_id bigint,create_time timestamp,update_time timestamp)");
    db.update("insert into blade_user values(-1,'owner','本人','本人','','','','2099000000000000001',0),(2,'staff2','陈通','陈通','13800138000','friend@example.com','','2099000000000000001',0),(3,'staff3','其他好友','其他好友','','','','2099000000000000001',0),(4,'staff4','非好友','非好友','','','','2099000000000000001',0)");
    db.update("insert into blade_friendship values(1,-1,2,null,'MUTUAL','ACTIVE',now()),(2,2,-1,'他的备注','MUTUAL','ACTIVE',now()),(3,3,2,'其他人的备注','MUTUAL','ACTIVE',now()),(4,-1,3,null,'MUTUAL','ACTIVE',now())");
    var mapper=new ObjectMapper();var social=new SmartSocialService(db);
    var service=new SmartReminderService(db,mapper,null,null,null,null,social,null);
    social.saveRemark(-1L,2L," 通哥 ");
    check("通哥".equals(db.queryForObject("select friend_remark from blade_friendship where id=1",String.class)),"owner remark saved and trimmed");
    check("陈通".equals(db.queryForObject("select name from blade_user where id=2",String.class)),"real name unchanged");
    check("其他人的备注".equals(db.queryForObject("select friend_remark from blade_friendship where id=3",String.class))&&"他的备注".equals(db.queryForObject("select friend_remark from blade_friendship where id=2",String.class)),"reverse and other-owner remarks unchanged");
    for(Long person:Arrays.asList(4L,-1L,null)){try{social.saveRemark(-1L,person,"错误");throw new AssertionError("unauthorized remark");}catch(ServiceException expected){}}
    for(String bad:Arrays.asList("字".repeat(31),"换\n行",null)){try{social.saveRemark(-1L,2L,bad);throw new AssertionError("invalid remark");}catch(ServiceException expected){}}
    check(service.friendList().stream().anyMatch(row->"通哥".equals(row.get("friendRemark"))&&"friend@example.com".equals(row.get("email"))),"friend list returns owned remark and basic info");
    var context=SmartReminderService.class.getDeclaredMethod("friendContext",Long.class);context.setAccessible(true);
    String contextText=(String)context.invoke(service,-1L);
    check(contextText.contains("通哥")&&!contextText.contains("friend@example.com"),"AI context includes private remark without extra profile email");
    var resolver=SmartReminderService.class.getDeclaredMethod("resolvePerson",Long.class,String.class);resolver.setAccessible(true);
    check(((List<?>)resolver.invoke(service,-1L,"通哥")).size()==1,"AI resolves current owner remark");
    check(((List<?>)resolver.invoke(service,3L,"通哥")).isEmpty(),"AI cannot resolve another owner's remark");
    social.saveRemark(-1L,3L,"通哥");
    check(((List<?>)resolver.invoke(service,-1L,"通哥")).size()==2,"duplicate remark retains ambiguity instead of choosing first");
    social.rememberRemarks(-1L,mapper.readTree("[{\"personUserId\":\"2\",\"remark\":\"老陈\"}]"));
    check("老陈".equals(db.queryForObject("select friend_remark from blade_friendship where id=1",String.class)),"AI remark writes same field as UI");
    social.rememberRemarks(-1L,mapper.readTree("[{\"personUserId\":\"2\",\"remark\":\"\"}]"));
    check(db.queryForObject("select friend_remark from blade_friendship where id=1",String.class)==null,"clear remark restores original-name display");
    for(String key:List.of("staff2","13800138000","friend@example.com"))check(service.searchUsers(key).size()==1,"exact discovery "+key);
    for(String key:List.of("陈通","陈","staff","%","friend@","通哥"))check(service.searchUsers(key).isEmpty(),"no name/partial/remark discovery "+key);
    check(!service.searchUsers("staff2").get(0).containsKey("phone")&&!service.searchUsers("staff2").get(0).containsKey("email"),"discovery does not expose contact fields");
    try{service.searchUsers("x".repeat(101));throw new AssertionError("unbounded search");}catch(ServiceException expected){}
    db.update("update blade_friendship set status='REMOVED' where id=1");
    try{social.saveRemark(-1L,2L,"失效");throw new AssertionError("removed friend remark");}catch(ServiceException expected){}
    check(((List<?>)resolver.invoke(service,-1L,"老陈")).isEmpty(),"removed friend unavailable to AI");
  }
}
