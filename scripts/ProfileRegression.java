import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ProfileUpdateRequest;
import org.springblade.modules.smartreminder.support.ProfileUpdateRules;
import org.springblade.core.log.exception.ServiceException;

/** Offline: unauthenticated AuthUtil viewer -1 is a fixture only; HTTP controller still enforces AppRoleGuard. */
public class ProfileRegression {
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static void main(String[] args){
    var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:profile;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","sa",""));
    db.execute("create table blade_user(id bigint primary key,account varchar(45),name varchar(50),real_name varchar(50),phone varchar(20),email varchar(45),avatar varchar(1000),update_user bigint,update_time timestamp,is_deleted int)");
    db.execute("create table blade_smart_user_preference(user_id bigint,ai_avatar varchar(1000))");
    db.update("insert into blade_user values(-1,'fixture','原姓名','原姓名','13800138000','old@example.com','/avatars/original.png',-1,now(),0),(2,'other','其他用户','其他用户','','','',2,now(),0)");
    var social=new SmartSocialService(db);
    var service=new SmartReminderService(db,new ObjectMapper(),null,null,null,null,social,null);
    var input=new ProfileUpdateRequest();input.setName(" 新姓名 ");input.setPhone("13900139000");input.setEmail("new@example.com");
    var result=service.updateProfile(input);
    check("新姓名".equals(result.get("name"))&&"新姓名".equals(result.get("realName")),"name updates both display and real name");
    check("new@example.com".equals(result.get("email"))&&"13900139000".equals(result.get("phone")),"contacts saved and returned");
    check("/avatars/original.png".equals(result.get("avatar")),"omitted avatar preserved");
    check("其他用户".equals(db.queryForObject("select name from blade_user where id=2",String.class)),"other user not modified");
    var legacy=new ProfileUpdateRequest();legacy.setNickname("旧版昵称");legacy.setAvatar("/avatars/updated.png");result=service.updateProfile(legacy);
    check("new@example.com".equals(result.get("email"))&&"13900139000".equals(result.get("phone"))&&"新姓名".equals(result.get("realName")),"legacy avatar client preserves contact fields and real name");
    input.setPhone("");input.setEmail("");result=service.updateProfile(input);
    check("".equals(result.get("email"))&&"".equals(result.get("phone")),"explicit blank clears optional fields");
    for(int n=0;n<4;n++){
      var bad=new ProfileUpdateRequest();bad.setName(n==0?" ":n==1?"人".repeat(21):"姓名");bad.setPhone(n==2?"123":null);bad.setEmail(n==3?"bad":null);
      try{ProfileUpdateRules.validate(bad);throw new AssertionError("invalid profile accepted");}catch(ServiceException expected){System.out.println("PASS invalid profile rejected "+n);}
    }
  }
}
