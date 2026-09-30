import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springblade.common.filter.CookieSecureFilter;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.auth.handler.BackendCompatibilityExceptionHandler;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;
import static org.mockito.Mockito.*;

/** Exercise the real filter; only authentication results are stubbed. No production data. */
public class LoginCookieRegression {
    static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }
    static void run(String cookie, Object cookieUserId, boolean parserThrows, boolean authenticated,
                    int status, boolean clear) throws Exception {
        var request = new MockHttpServletRequest("POST", "/app/reminder/bootstrap");
        request.setSecure(true);
        if (cookie != null) request.setCookies(new Cookie("saber-access-token", cookie));
        if (authenticated) request.addHeader("Blade-Auth", "bearer current-token");
        var response = new MockHttpServletResponse();
        var user = new BladeUser(); user.setUserId(42L);
        var claims = mock(Claims.class);
        when(claims.get(TokenConstant.USER_ID)).thenReturn(cookieUserId);
        boolean[] reached = {false};
        try (var auth = mockStatic(AuthUtil.class)) {
            auth.when(AuthUtil::getUser).thenReturn(authenticated ? user : null);
            if (cookie != null) {
                if (parserThrows) auth.when(() -> AuthUtil.parseJWT(cookie)).thenThrow(new JwtException("expired"));
                else auth.when(() -> AuthUtil.parseJWT(cookie)).thenReturn("invalid".equals(cookie) ? null : claims);
            }
            new CookieSecureFilter().doFilter(request, response, (req, res) -> {
                reached[0] = true;
                // Stand-in for downstream authentication: a stale cookie never grants a session.
                if (!authenticated) ((MockHttpServletResponse) res).setStatus(401);
                check(req == request, "original credential is not rewritten");
            });
        }
        check(response.getStatus() == status, "expected status for " + cookie + "/" + cookieUserId);
        check(reached[0] == (!authenticated || status == 200), "downstream authentication preserved");
        Cookie removed = response.getCookie("saber-access-token");
        check(clear == (removed != null && removed.getMaxAge() == 0), "stale cookie cleanup");
        if (clear) check(removed.getSecure() && removed.isHttpOnly() && "/".equals(removed.getPath()), "cookie removal scope");
    }
    public static void main(String[] args) throws Exception {
        run(null, null, false, true, 200, false);
        run("valid", "42", false, true, 200, false);
        run("valid", 42L, false, true, 200, false);
        run("invalid", null, false, true, 200, true);
        run("expired", null, true, true, 200, true);
        run("missing-id", null, false, true, 200, true);
        run("empty-id", "", false, true, 200, true);
        run("different-user", "43", false, true, 401, false);
        run("invalid", null, false, false, 401, false);
        var result = new BackendCompatibilityExceptionHandler().handle(
            new CodeGenerationException(new IllegalStateException("internal class details")));
        check(result.getStatusCode().value() == 500, "compatibility failure keeps HTTP 500");
        check(!result.getBody().toString().contains("internal class details"), "internal failure details not exposed");
        check(Boolean.FALSE.equals(result.getBody().get("success")), "compatibility failure not reported as success");
        System.out.println("ALL LOGIN COOKIE REGRESSIONS PASSED");
    }
}
