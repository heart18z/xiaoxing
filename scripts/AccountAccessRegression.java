import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.config.RegistrationMailSchema;
import org.springblade.modules.auth.exception.NeedConfirmLoginException;
import org.springblade.modules.auth.utils.OAuth2TokenIssueHelper;
import org.springblade.core.oauth2.handler.AuthorizationHandler;
import org.springblade.core.oauth2.service.*;
import org.springblade.core.oauth2.provider.*;
import org.springblade.core.jwt.JwtUtil;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;

/** Real transactional SQL; delivery, OAuth token signing and Redis are isolated test doubles. */
public class AccountAccessRegression {
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
    static void rejected(Runnable work){try{work.run();throw new AssertionError("Expected rejection");}catch(org.springblade.core.log.exception.ServiceException expected){}}
    public static int findInSet(String value,String set){return Arrays.asList(set.split(",")).contains(value)?1:0;}
    static class Mail extends RegistrationMailService {
        Map<String,String> codes=new HashMap<>();
        Mail(JdbcTemplate db){super(db,new SecretCodec("fixture-secret"));}
        @Override public void requireEnabled(){}
        @Override public void sendActionCode(String email,String code,String purpose){codes.put(purpose+":"+email,code);}
        @Override public void sendCode(String email,String code){codes.put("REGISTER:"+email,code);}
    }
    public static void main(String[] args)throws Exception {
        check(org.springblade.modules.smartreminder.controller.SmartReminderController.class.getMethod("messages",int.class).getParameters()[0].isNamePresent(),"controller request parameter metadata retained");
        var source=new DriverManagerDataSource("jdbc:h2:mem:account130;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        var db=new JdbcTemplate(source);var manager=new DataSourceTransactionManager(source);
        new RegistrationMailSchema(db).migrate();
        db.execute("create alias find_in_set for 'AccountAccessRegression.findInSet'");
        db.execute("create table blade_user(id bigint primary key,tenant_id varchar(12),account varchar(45),password varchar(100),role_id varchar(50),status int,is_deleted int,phone varchar(45),email varchar(45),name varchar(50),real_name varchar(50),avatar varchar(100),last_change_password_time timestamp,update_time timestamp,app_unique_email varchar(45) generated always as(case when is_deleted=0 then lower(trim(email)) else null end),unique(tenant_id,app_unique_email))");
        db.execute("create table blade_role(id bigint,tenant_id varchar(12),role_alias varchar(40),is_deleted int)");db.update("insert into blade_role values(1,'000000','app_user',0)");
        db.update("insert into blade_user(id,tenant_id,account,email,phone,password,role_id,status,is_deleted) values(11,'000000','fixture_user','one@example.com','13800138000',?,'1',1,0),(22,'000000','other_user','other@example.com',null,?,'1',1,0)",DigestUtil.encrypt("OldPassword9"),DigestUtil.encrypt("OtherPassword9"));
        db.execute("create table blade_client(client_id varchar(40))");db.update("insert into blade_client values('saber'),('other')");
        var mail=new Mail(db);var verify=new EmailVerificationService(db,mail,manager,"fixture-sign-key");
        var users=mock(OAuth2UserService.class);var clients=mock(OAuth2ClientService.class);var auth=mock(AuthorizationHandler.class);var tokens=mock(OAuth2TokenIssueHelper.class);
        var client=mock(OAuth2Client.class);when(clients.loadByClientId(eq("saber"),any())).thenReturn(client);when(clients.validateClient(client,"saber","fixture-secret")).thenReturn(true);when(clients.validateGranter(client,"password")).thenReturn(true);
        var user=mock(OAuth2User.class);when(user.getAuthorities()).thenReturn(List.of("app_user"));when(users.loadByUsername(eq("fixture_user"),any())).thenReturn(user);when(users.validateUser(user)).thenReturn(true);
        when(auth.preValidation(any())).thenReturn(new OAuth2Validation());when(auth.authValidation(any(),any())).thenReturn(new OAuth2Validation());when(tokens.issueToken(eq(user),any())).thenReturn(Kv.create().set("access_token","fixture-token"));
        var request=new MockHttpServletRequest();request.addHeader("Authorization","Basic "+Base64.getEncoder().encodeToString("saber:fixture-secret".getBytes()));request.addHeader("Tenant-Id","attacker-tenant");RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var target=new AccountAccessService(db,verify,users,clients,auth,tokens);
        var factory=new ProxyFactory(target);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));var service=(AccountAccessService)factory.getProxy();
        service.send("ONE@example.com","LOGIN","127.0.0.1");String login=mail.codes.get("LOGIN:one@example.com");
        rejected(()->service.reset("one@example.com",login,"NewPassword9","NewPassword9"));check(db.queryForObject("select used from blade_account_code where email='LOGIN:one@example.com'",Integer.class)==0,"login code cannot reset password");
        String wrong=login.equals("000000")?"000001":"000000";
        rejected(()->service.login("one@example.com",wrong));check(db.queryForObject("select attempts from blade_account_code where email='LOGIN:one@example.com'",Integer.class)==1,"invalid attempts persist through rollback boundary");
        when(auth.authValidation(any(),any())).thenThrow(new NeedConfirmLoginException());
        try{service.login("one@example.com",login);throw new AssertionError();}catch(NeedConfirmLoginException expected){}
        check(db.queryForObject("select used from blade_account_code where email='LOGIN:one@example.com'",Integer.class)==0,"confirmation challenge preserves code");
        doAnswer(inv->{OAuth2Request r=inv.getArgument(1);check("000000".equals(r.getTenantId())&&"fixture_user".equals(r.getUsername())&&Boolean.TRUE.equals(r.isPassword()),"canonical identity and validation pipeline");return new OAuth2Validation();}).when(auth).authValidation(any(),any());
        check("fixture-token".equals(service.login("one@example.com",login).get("access_token")),"valid code issues login token");rejected(()->service.login("one@example.com",login));
        db.update("update blade_registration_rate set started_at=0");service.send("one@example.com","RESET","127.0.0.1");String reset=mail.codes.get("RESET:one@example.com");
        rejected(()->service.reset("one@example.com",reset,"NewPassword9","Mismatch9"));
        try(var jwt=mockStatic(JwtUtil.class)) {
            service.reset("one@example.com",reset,"NewPassword9","NewPassword9");
            check(DigestUtil.encrypt("NewPassword9").equals(db.queryForObject("select password from blade_user where id=11",String.class)),"verified reset updates password");
            jwt.verify(()->JwtUtil.removeAccessToken("000000","saber","11",null));jwt.verify(()->JwtUtil.removeRefreshToken("000000","saber","11",null));
            check(true,"reset revokes access and refresh indices");
            rejected(()->service.reset("one@example.com",reset,"AnotherPassword9","AnotherPassword9"));
        }
        var ai=new AiConfigService(db,new SecretCodec("fixture"));
        db.execute("create table blade_smart_user_preference(user_id bigint primary key,language varchar(10),llm_config_id bigint,llm_personal varchar(100),update_time timestamp)");db.update("insert into blade_smart_user_preference values(11,'zh-cn',999,'retained-secret',now())");
        ai.saveLanguage(11L,"en-us");check("en-us".equals(db.queryForObject("select language from blade_smart_user_preference where user_id=11",String.class))&&db.queryForObject("select llm_config_id from blade_smart_user_preference where user_id=11",Long.class)==999,"language saves with unavailable model and retains settings");
        db.execute("create table blade_smart_event(id bigint primary key,creator_user_id bigint,event_status varchar(20),stop_time timestamp,stop_reason varchar(100),update_time timestamp)");db.update("insert into blade_smart_event values(1,11,'ACTIVE',null,null,null),(2,22,'ACTIVE',null,null,null)");
        db.execute("create table blade_smart_event_branch(id bigint primary key,event_id bigint,recipient_user_id bigint,branch_status varchar(20),stop_time timestamp,stop_reason varchar(100),next_evaluate_time timestamp,evaluate_lock int,evaluation_token varchar(100),update_time timestamp)");db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,branch_status) values(1,1,22,'ACTIVE'),(2,2,11,'ACTIVE'),(3,2,22,'ACTIVE')");
        for(String table:List.of("blade_smart_push_device","blade_smart_push_delivery","blade_smart_chat_message","blade_smart_candidate")){db.execute("create table "+table+"(user_id bigint)");db.update("insert into "+table+" values(11),(22)");}
        db.execute("create table blade_friendship(owner_user_id bigint,friend_user_id bigint)");db.execute("create table blade_friend_request(applicant_user_id bigint,target_user_id bigint)");db.execute("create table blade_smart_person_alias(owner_user_id bigint,person_user_id bigint)");
        db.execute("create table blade_app_chat_job(user_id bigint,job_status varchar(20),created_at timestamp)");
        db.update("insert into blade_app_chat_job values(11,'RUNNING',now())");
        rejected(()->service.close(11L,"NewPassword9",true));db.update("delete from blade_app_chat_job");
        check(true,"in-flight chat blocks closure before any destructive mutation");
        rejected(()->service.close(11L,"NewPassword9",false));rejected(()->service.close(11L,"WrongPassword9",true));
        check(db.queryForObject("select is_deleted from blade_user where id=11",Integer.class)==0,"cancelled or unauthenticated closure leaves account intact");
        try(var jwt=mockStatic(JwtUtil.class)){service.close(11L,"NewPassword9",true);}
        check(db.queryForObject("select is_deleted from blade_user where id=11",Integer.class)==1&&db.queryForObject("select app_unique_email from blade_user where id=11",String.class)==null,"closure disables identity and releases email");
        check(db.queryForObject("select count(*) from blade_smart_push_device where user_id=11",Integer.class)==0&&db.queryForObject("select count(*) from blade_smart_push_device where user_id=22",Integer.class)==1,"closure removes only own push bindings");
        check("STOPPED".equals(db.queryForObject("select branch_status from blade_smart_event_branch where id=2",String.class))&&"ACTIVE".equals(db.queryForObject("select branch_status from blade_smart_event_branch where id=3",String.class)),"closure stops own reminders and preserves other recipients");rejected(()->service.account("one@example.com"));
        var reminder=new SmartReminderService(db,new ObjectMapper(),null,null,null,null,null,null,null);var method=SmartReminderService.class.getDeclaredMethod("stringifyJsonIds",com.fasterxml.jackson.databind.JsonNode.class);method.setAccessible(true);
        var payload=new ObjectMapper().readTree("{\"fileIds\":[210625145938612225,\"210625145938612226\"],\"eventId\":210625145938612227,\"count\":3}");method.invoke(reminder,payload);
        check(payload.path("fileIds").get(0).isTextual()&&"210625145938612225".equals(payload.path("fileIds").get(0).asText())&&payload.path("count").isNumber(),"history preserves 64-bit image IDs without converting counts");
        RequestContextHolder.resetRequestAttributes();System.out.println("ALL ACCOUNT ACCESS REGRESSIONS PASSED");
    }
}
