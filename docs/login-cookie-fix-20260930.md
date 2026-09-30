# 登录后旧 Cookie 导致用户资料加载失败

## 证据与原因

2026-09-30 用户截图在「继续登录」弹窗上显示 `No compatible defineClass mechanism detected`。生产日志的失败接口实际是 `/app/reminder/bootstrap`，不是密码验证或签发令牌接口。

登录已取得新的 Blade-Auth 凭证，但部分客户端同时发送旧 `saber-access-token` Cookie。`CookieSecureFilter` 对旧 Cookie 调用 `AuthUtil.parseJWT` 后未检查空值，继续读取用户 ID，导致空指针。随后 Blade 的错误响应处理通过 `BladeBeanMap` / CGLIB 生成类，又因 Java 17 模块访问限制失败，把原始空指针遮住。

上线前使用临时 app_user 测试账号复现：不带 Cookie、携带当前 Cookie 的 bootstrap 均返回 200；同一账号同一有效令牌携带失效 Cookie 时，返回 500 和截图中相同的 CGLIB 文案。普通重复登录仍正常返回 `need_confirm_login`。

## 修复

- 对解析失败、过期、缺失用户 ID 的 Cookie 做空值保护并发送 Cookie 过期响应；保留原请求凭证，由后续认证继续校验。
- 有效 Cookie 指向不同账号时仍拒绝，未登录请求不因这项兼容逻辑获得权限。
- 两个 Java 17 Docker 启动入口加入 `--add-opens=java.base/java.lang=ALL-UNNAMED`；生产保留原镜像启动入口，仅在已有 JAVA_OPTS 中追加该参数。
- 增加特定 CGLIB 异常处理器，保留服务器堆栈日志和 HTTP 500，客户端只显示可理解的服务异常提示。登录确认处理器保持原样。

## 验证

`scripts/run-login-cookie-regression.ps1` 编译 Java 17 代码并运行真实过滤器回归。覆盖无 Cookie、相同用户字符串/数字 ID、解析返回 null、解析抛出异常、缺少/空 ID、不同账号、未认证，以及错误提示脱敏。

生产接口回归通过：普通登录、当前 Cookie、失效 Cookie 均正常加载正确测试用户；重复登录要求确认；确认登录后携带旧 Cookie 也正常；缺失/无效请求凭证仍被拒绝。

## 部署与回滚

生产镜像：`aimessage-backend:login-cookie-20260930`。
备份目录：`/opt/aimessage/backups/pre-login-cookie-20260930`。
发布目录：`/opt/aimessage/releases/login-cookie-20260930`。

`scripts/deploy-login-cookie.py` 校验服务器身份、当前配置和 JAR 哈希，仅替换 CookieSecureFilter 与 BackendCompatibilityExceptionHandler 两个类；逐项验证其余 JAR 内容未变化。发布只修改后台镜像、构建路径及 JAVA_OPTS，其余 Compose 配置不变。重启后健康检查通过，启动失败自动恢复旧后台与配置。可用脚本的 `rollback` 子命令恢复，不恢复数据库。

该修复在服务器生效，无需重新打包手机 APP。验证使用临时账号，不修改真实用户密码、角色、事件或提醒。
