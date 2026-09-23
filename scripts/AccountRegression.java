import org.springblade.modules.smartreminder.controller.AppAccountController.*;
import org.springblade.modules.smartreminder.service.AppAccountService;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springblade.core.tool.utils.DigestUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Offline H2 + actual service/Bean Validation. Never contacts production. */
public class AccountRegression {
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("PASS "+message);}
  static Registration valid(String account){var r=new Registration();r.setAccount(account);r.setName("测试用户");r.setPassword("FixturePassword9");r.setEmail("fixture@example.com");r.setPhone("");r.setEmailCode("123456");return r;}
  public static void main(String[] args)throws Exception {
    Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
    for(String account:new String[]{"abc","x".repeat(33),"_abcd","人员号123","a b c"})check(!validator.validate(valid(account)).isEmpty(),"invalid account rejected");
    check(validator.validate(valid("1234")).isEmpty(),"minimum account accepted");
    check(validator.validate(valid("x".repeat(32))).isEmpty(),"maximum account accepted");
    var blank=valid("blank");blank.setEmail("");check(!validator.validate(blank).isEmpty(),"email required");
    var input=valid("staff_123");input.setName(" ");check(!validator.validate(input).isEmpty(),"blank name rejected");
    input=valid("staff_123");input.setPassword("short");check(!validator.validate(input).isEmpty(),"short password rejected");
    input=valid("staff_123");input.setPhone("123");check(!validator.validate(input).isEmpty(),"invalid optional phone rejected");
    input=valid("staff_123");input.setEmail("bad");check(!validator.validate(input).isEmpty(),"invalid email rejected");
    var source=new DriverManagerDataSource("jdbc:h2:mem:accounts;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE","sa","");
    int[] contactIndexCount={2};
    JdbcTemplate db=new JdbcTemplate(source){
      @Override public <T>T queryForObject(String sql,Class<T> type){
        // H2 has a different metadata catalog; only emulate the MySQL index-presence probe.
        if(sql.contains("information_schema.statistics"))return type.cast(Integer.valueOf(sql.contains("uk_app_phone_tenant")?contactIndexCount[0]:2));
        return super.queryForObject(sql,type);
      }
    };
    db.execute("create table blade_user(id bigint primary key,tenant_id varchar(12),account varchar(45),password varchar(45),role_id varchar(100),status int,is_deleted int,phone varchar(45),email varchar(45),app_unique_phone varchar(45) generated always as (case when is_deleted=0 then nullif(trim(phone),'') else null end),app_unique_email varchar(45) generated always as (case when is_deleted=0 then nullif(lower(trim(email)),'') else null end),last_change_password_time timestamp,update_time timestamp,unique(tenant_id,account),unique(tenant_id,app_unique_phone),unique(tenant_id,app_unique_email))");
    db.execute("create table blade_role(id bigint,tenant_id varchar(12),role_alias varchar(50),is_deleted int)");
    db.update("insert into blade_role values(10,'000000','app_user',0)");
    AtomicLong ids=new AtomicLong();
    IUserService users=(IUserService)Proxy.newProxyInstance(IUserService.class.getClassLoader(),new Class[]{IUserService.class},(proxy,method,values)->{
      if(!method.getName().equals("submit"))throw new UnsupportedOperationException(method.getName());
      User u=(User)values[0];check(u.getAvatar()!=null && u.getAvatar().matches("/avatars/user/B([1-9]|1[0-3])[.]png"),"new user receives library avatar");check("10".equals(u.getRoleId())&&"000000".equals(u.getTenantId()),"server controls role and tenant");
      return db.update("insert into blade_user(id,tenant_id,account,password,role_id,phone,email,status,is_deleted) values(?,?,?,?,?,?,?,1,0)",ids.incrementAndGet(),u.getTenantId(),u.getAccount(),DigestUtil.encrypt(u.getPassword()),u.getRoleId(),u.getPhone(),u.getEmail())==1;
    });
    var service=new AppAccountService(db,users,new org.springblade.modules.smartreminder.service.EmailVerificationService(db,null,new DataSourceTransactionManager(source),"fixture-key"){ @Override public void consume(String email,String code){} });
    contactIndexCount[0]=0;
    try{service.register(valid("before_migration"));throw new AssertionError("signup without unique contact protection");}catch(org.springblade.core.log.exception.ServiceException expected){check(expected.getMessage().contains("唯一性保护"),"registration fails closed until contact indexes installed");}
    contactIndexCount[0]=2;
    var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
    Callable<Boolean> signup=()->{start.await();try{tx.execute(status->{service.register(valid("same_user"));return true;});return true;}catch(RuntimeException expected){return false;}};
    var first=pool.submit(signup);var second=pool.submit(signup);start.countDown();
    check(first.get()!=second.get(),"concurrent signup produces exactly one success");pool.shutdown();
    check(db.queryForObject("select count(*) from blade_user",Integer.class)==1,"duplicate was not inserted");
    try{service.register(valid("SAME_USER"));throw new AssertionError("case duplicate accepted");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS case-insensitive duplicate rejected");}
    Long id=db.queryForObject("select id from blade_user",Long.class);
    var collision=valid("different");collision.setEmail(" FIXTURE@EXAMPLE.COM ");
    try{service.register(collision);throw new AssertionError("duplicate email accepted");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS normalized duplicate email rejected");}
    var phoneOnly=valid("phone_only");phoneOnly.setEmail("");phoneOnly.setPhone("13800138000");
    check(!validator.validate(phoneOnly).isEmpty(),"phone cannot replace required email");
    try{service.register(phoneOnly);throw new AssertionError("service accepted phone-only registration");}catch(org.springblade.core.log.exception.ServiceException expected){check(expected.getMessage().contains("邮箱"),"service requires email too");}
    phoneOnly.setEmail("phone@example.com");service.register(phoneOnly);
    var phoneDuplicate=valid("phone_other");phoneDuplicate.setEmail("other@example.com");phoneDuplicate.setPhone("13800138000");
    try{service.register(phoneDuplicate);throw new AssertionError("duplicate phone accepted");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS duplicate phone rejected even with distinct email");}
    var racers=Executors.newFixedThreadPool(2);var go=new CountDownLatch(1);
    var accountNumber=new AtomicLong();
    Callable<Boolean> sameContact=()->{go.await();var r=valid("race_"+accountNumber.incrementAndGet());r.setEmail("race@example.com");try{tx.execute(status->{service.register(r);return true;});return true;}catch(RuntimeException expected){return false;}};
    var a=racers.submit(sameContact);var b=racers.submit(sameContact);go.countDown();check(a.get()!=b.get(),"different accounts concurrent same contact produce one success");racers.shutdown();
    try{db.update("update blade_user set email='FIXTURE@example.com' where account='phone_only'");throw new AssertionError("profile/admin update bypassed unique contact");}catch(org.springframework.dao.DuplicateKeyException expected){System.out.println("PASS database also rejects duplicate contact edits");}
    var password=new PasswordChange();password.setOldPassword("wrong");password.setPassword("NewFixture9");password.setConfirmation("NewFixture9");
    try{service.changePassword(id,password);throw new AssertionError("wrong old password accepted");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS wrong old password rejected");}
    password.setOldPassword("FixturePassword9");password.setConfirmation("mismatch");
    try{service.changePassword(id,password);throw new AssertionError("mismatch accepted");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS confirmation mismatch rejected");}
    password.setConfirmation("NewFixture9");service.changePassword(id,password);
    check(DigestUtil.encrypt("NewFixture9").equals(db.queryForObject("select password from blade_user where id=?",String.class,id)),"new password uses login-compatible hash");
    for(int i=0;i<8;i++)service.limit("password:fixture",8);
    try{service.limit("password:fixture",8);throw new AssertionError("rate limit missing");}catch(org.springblade.core.log.exception.ServiceException expected){System.out.println("PASS repeated password guesses throttled");}
  }
}
