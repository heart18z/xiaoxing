import org.springblade.modules.smartreminder.config.SmartReminderWebConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

public class NativeCorsRegression {
  static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("PASS "+name);}
  static MockHttpServletResponse request(String origin,String path,String method,boolean preflight)throws Exception{
    var req=new MockHttpServletRequest(method,path);req.setServletPath(path);req.setServerName("api.example");req.setServerPort(443);req.setScheme("https");
    req.addHeader("Origin",origin);
    if(preflight){req.addHeader("Access-Control-Request-Method","POST");req.addHeader("Access-Control-Request-Headers","authorization,blade-auth,blade-requested-with,content-type,tenant-id,dept-id,role-id,captcha-key,captcha-code,confirm");}
    var res=new MockHttpServletResponse();new SmartReminderWebConfig().nativeCorsFilter().getFilter().doFilter(req,res,new MockFilterChain());return res;
  }
  public static void main(String[]args)throws Exception{
    var env=new org.springframework.core.env.StandardEnvironment();
    env.getPropertySources().replace("systemEnvironment",new org.springframework.core.env.SystemEnvironmentPropertySource("systemEnvironment",java.util.Map.of("SMARTREMINDER_PUSH_ENABLED","true","SMARTREMINDER_PUSH_PRODUCTIONKEYID","RAR492G6T7","SMARTREMINDER_PUSH_PRODUCTIONKEYPATH","/run/secrets/test.p8")));
    var props=org.springframework.boot.context.properties.bind.Binder.get(env).bind("smart-reminder.push",org.springblade.modules.smartreminder.push.PushProperties.class).get();
    check(props.isEnabled() && props.getProductionKeyPath().equals("/run/secrets/test.p8"),"documented environment variables bind to push properties");
    var login=request("capacitor://localhost","/blade-auth/oauth/token","OPTIONS",true);
    check(login.getStatus()==200 && "capacitor://localhost".equals(login.getHeader("Access-Control-Allow-Origin")),"native login preflight allows real login headers");
    check(login.getHeader("Access-Control-Allow-Credentials")==null,"native CORS does not expose browser cookies");
    check(request("capacitor://localhost","/app/push/register","OPTIONS",true).getStatus()==200,"native device binding preflight allowed");
    check(request("capacitor://evil","/app/push/register","OPTIONS",true).getStatus()==403,"other native origins rejected");
    check(request("capacitor://localhost","/blade-system/menu/list","OPTIONS",true).getStatus()==403,"admin API not added to native CORS surface");
    check(request("http://localhost:2888","/app/reminder/bootstrap","POST",false).getStatus()==200,"existing web/proxy requests retain prior CORS behavior");
    System.out.println("ALL NATIVE CORS REGRESSIONS PASSED");
  }
}
