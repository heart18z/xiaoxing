package org.springblade.modules.auth.handler;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.support.Kv;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keep JVM implementation details in server logs, including errors from /error. */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice
public class BackendCompatibilityExceptionHandler {
    @ExceptionHandler(CodeGenerationException.class)
    public ResponseEntity<Kv> handle(CodeGenerationException exception) {
        log.error("Backend class generation failed", exception);
        return ResponseEntity.status(500).body(Kv.create()
            .set("code", 500).set("success", false)
            .set("msg", "服务暂时不可用，请稍后重试；若持续出现，请联系管理员"));
    }
}
