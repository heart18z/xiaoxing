import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.config.RegistrationMailSchema;
import org.springblade.modules.smartreminder.controller.AppAccountController.Registration;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.system.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import jakarta.validation.Validation;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real JDBC transactions and validation, fake SMTP delivery only. No external mail or users. */
public class RegistrationMailRegression {
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);System.out.println("PASS "+message);}
    static void rejected(Runnable task,String expected){try{task.run();throw new AssertionError("Expected rejection: "+expected);}catch(org.springblade.core.log.exception.ServiceException e){check(e.getMessage().contains(expected),"reject "+expected);}}
    static Registration registration(String account,String email,String code){var r=new Registration();r.setAccount(account);r.setName("注册测试");r.setPassword("TestPassword9");r.setEmail(email);r.setPhone("");r.setEmailCode(code);return r;}
    static class Mail extends RegistrationMailService {
        Map<String,String> delivered=new HashMap<>();boolean fail;
        Mail(JdbcTemplate db){super(db,new SecretCodec("fixture-secret-only"));}
        @Override public void sendCode(String email,String code){if(fail)throw new org.springblade.core.log.exception.ServiceException("模拟发送失败");delivered.put(email,code);}
    }
    public static void main(String[] args)throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:mail125;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
        JdbcTemplate db=new JdbcTemplate(source){@Override public <T>T queryForObject(String sql,Class<T> type){if(sql.contains("information_schema.statistics"))return type.cast(2);return super.queryForObject(sql,type);}};
        new RegistrationMailSchema(db).migrate();new RegistrationMailSchema(db).migrate();
        db.execute("create table blade_user(id bigint primary key,tenant_id varchar(12),account varchar(45),phone varchar(45),email varchar(45),is_deleted int,app_unique_phone varchar(45) generated always as (case when is_deleted=0 then nullif(trim(phone),'') else null end),app_unique_email varchar(45) generated always as (case when is_deleted=0 then nullif(lower(trim(email)),'') else null end),unique(tenant_id,account),unique(tenant_id,app_unique_email),unique(tenant_id,app_unique_phone))");
        db.execute("create table blade_role(id bigint,tenant_id varchar(12),role_alias varchar(40),is_deleted int)");db.update("insert into blade_role values(1,'000000','app_user',0)");
        var manager=new DataSourceTransactionManager(source);var mail=new Mail(db);var verify=new EmailVerificationService(db,mail,manager,"fixture-key");
        var settings=new RegistrationMailService.Settings();settings.setPassword("fixture-smtp-secret");mail.save(settings);
        rejected(()->verify.send("one@example.com","127.0.0.1"),"尚未启用");settings.setEnabled(true);mail.save(settings);
        check(!mail.detail().toString().contains("fixture-smtp-secret")&&!mail.detail().containsKey("password_cipher"),"settings never disclose password");
        String encrypted=db.queryForObject("select password_cipher from blade_registration_mail where id=1",String.class);
        check(!encrypted.contains("fixture-smtp-secret"),"SMTP password encrypted at rest");settings.setPassword("");mail.save(settings);
        check(encrypted.equals(db.queryForObject("select password_cipher from blade_registration_mail where id=1",String.class)),"blank password retains existing secret");
        var bad=registration("missing","one@example.com",null);check(!Validation.buildDefaultValidatorFactory().getValidator().validate(bad).isEmpty(),"missing code rejected by endpoint validation");
        AtomicLong ids=new AtomicLong();AtomicBoolean failInsert=new AtomicBoolean();
        IUserService users=(IUserService)Proxy.newProxyInstance(IUserService.class.getClassLoader(),new Class[]{IUserService.class},(proxy,method,values)->{
            if(!method.getName().equals("submit"))throw new UnsupportedOperationException();
            if(failInsert.get())throw new IllegalStateException("fixture insert failure");
            User u=(User)values[0];return db.update("insert into blade_user(id,tenant_id,account,phone,email,is_deleted) values(?,'000000',?,?,?,0)",ids.incrementAndGet(),u.getAccount(),u.getPhone(),u.getEmail())==1;
        });
        var factory=new ProxyFactory(new AppAccountService(db,users,verify));factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        AppAccountService accounts=(AppAccountService)factory.getProxy();
        rejected(()->accounts.register(registration("nocode","one@example.com",null)),"6位");
        verify.send("ONE@example.com ","127.0.0.1");String first=mail.delivered.get("one@example.com");
        rejected(()->verify.send("one@example.com","127.0.0.2"),"60秒");
        rejected(()->accounts.register(registration("changed","two@example.com",first)),"先获取");
        String wrong=first.equals("000000")?"000001":"000000";
        for(int i=0;i<5;i++)rejected(()->accounts.register(registration("wrongcode","one@example.com",wrong)),i==4?"错误次数":"不正确");
        check(db.queryForObject("select attempts from blade_registration_code where email='one@example.com'",Integer.class)==5,"wrong attempts commit despite rejected registration");
        rejected(()->accounts.register(registration("locked","one@example.com",first)),"错误次数");
        db.update("update blade_registration_rate set started_at=0");verify.send("one@example.com","127.0.0.1");String code=mail.delivered.get("one@example.com");
        rejected(()->accounts.register(registration("oldcode","one@example.com",wrong.equals(code)?first:wrong)),"不正确");
        failInsert.set(true);try{accounts.register(registration("retry","one@example.com",code));throw new AssertionError();}catch(IllegalStateException expected){}
        check(db.queryForObject("select used from blade_registration_code where email='one@example.com'",Integer.class)==0,"failed account creation rolls code consumption back");failInsert.set(false);
        var r=registration("valid_one"," ONE@example.com ",code);r.setPhone("13800138000");accounts.register(r);
        check(db.queryForObject("select count(*) from blade_user",Integer.class)==1,"verified registration inserts one account");
        check(Boolean.FALSE.equals(verify.availability("email","ONE@EXAMPLE.COM").get("available")),"email uniqueness normalized before submit");
        check(Boolean.FALSE.equals(verify.availability("phone","13800138000").get("available")),"phone uniqueness detected before submit");
        rejected(()->new org.springframework.transaction.support.TransactionTemplate(manager).execute(s->{verify.consume("one@example.com",code);return null;}),"失效");
        verify.send("expired@example.com","127.0.0.1");db.update("update blade_registration_code set expires_at=0 where email='expired@example.com'");
        rejected(()->accounts.register(registration("expired_user","expired@example.com",mail.delivered.get("expired@example.com"))),"失效");
        mail.fail=true;rejected(()->verify.send("failed@example.com","127.0.0.1"),"发送失败");
        check(db.queryForObject("select count(*) from blade_registration_code where email='failed@example.com'",Integer.class)==0,"failed delivery cannot produce usable code");mail.fail=false;
        verify.send("race@example.com","127.0.0.1");String race=mail.delivered.get("race@example.com");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var jobs=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++){int n=i;jobs.add(pool.submit(()->{start.await();try{accounts.register(registration("race_"+n,"race@example.com",race));return true;}catch(RuntimeException e){return false;}}));}start.countDown();check(jobs.get(0).get()!=jobs.get(1).get(),"concurrent use of same code creates exactly one user");}finally{pool.shutdownNow();}
        var restarted=new EmailVerificationService(db,mail,manager,"fixture-key");rejected(()->restarted.send("failed@example.com","127.0.0.1"),"60秒");
        check(db.queryForObject("select count(*) from blade_user",Integer.class)==2,"unverified attempts never inserted accounts");
    }
}
