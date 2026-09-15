# 公网 IP HTTPS 与新版发布（2026-09-15）

## HTTPS

- IP：`47.100.172.149`，入口 `https://47.100.172.149/app/login`。
- Let's Encrypt 正式 RSA IP 证书，SAN 为 IP Address，不以域名证书替代，不跳过证书校验。
- 当前链：叶子 → YR2 → Root YR（交叉签名）→ ISRG Root X1。公网 curl 校验通过，本机 TLS 1.2 / 无 SNI 的 IP 校验通过。
- Certbot 5.4.0 独立虚拟环境 `/opt/aimessage/certbot-ip`，配置 `/etc/letsencrypt-ip`，不替换原有域名 Certbot 1.21.0 和 `certbot.timer`。
- IP 证书短期有效，`xiaoxing-ip-cert-renew.timer` 每 6 小时检查、最多随机延迟 15 分钟；续签成功后 `nginx -t` 再 reload。
- HTTP-01 webroot：`/var/www/letsencrypt-ip`，独立于应用发布目录。HTTP IP 保留验证路径，其余请求跳转 HTTPS。
- 独立站点 `/etc/nginx/sites-available/xiaoxing-ip-https`，443 默认入口为 IP 证书；域名 SNI 仍使用原域名站点。保留原 App 精确 Origin CORS，不扩大认证白名单。
- Nginx 备份：`/opt/aimessage/ip-https-20260915/nginx-before`。证书、私钥、账户凭据保留服务器，不放入源码。
- staging 签发、正式签发、续签 dry-run（含部署 hook）、systemd 实际执行均通过。

## App 配置

- `.env.native` 与原生 API fallback 改为 HTTPS IP。网页版仍使用同源 `/api`，从 IP 打开即连接 IP，不引入跨源 Cookie 或混合内容。
- iOS 17+ 明确声明单个 IP 的 ATS 配置，使用空字典保留安全默认值；没有 `NSAllowsArbitraryLoads`、没有 HTTP 放行、没有忽略 TLS 错误。
- `build:native`、`build:prod` 通过；`cap copy ios` 已同步打包资源。33 项 Node 测试及后台聊天/侧栏 H2 回归通过。
- Windows 不能签名构建 iOS 安装包；已安装的旧 App 不会因服务器部署而自动切换地址，需重新构建并通过 TestFlight 更新。iOS 15/17/18/26 仍需真机验证，不能以桌面测试替代。

## 应用部署

- Release：`/opt/aimessage/releases/drawer-ip-20260915`。
- Backup：`/opt/aimessage/backups/pre-drawer-ip-20260915`，包括发布前完整数据库快照、Compose 与原发布位置。
- 无重复手机号/邮箱组；增加联系方式唯一索引、账号每日序列表、聊天后台任务表；不删除、覆盖或回填已有用户和对话。
- 后端配置比对仅放行注册账号建议接口的两条公开路由配置，其余 7 个嵌入配置逐一核对。
- JAR SHA256：`261f32b7ac4b46121a4fe8a2490d8878cd1f0d92aec45c54b855c37232970990`。
- Web tar SHA256：`b7cd7da6eb4eedf2d96a666e45104baf99da53d812ce04547af260b183d26aec`。
- 回滚只切换应用版本；不倒灌数据库，避免覆盖发布后新增用户数据。证书属于独立配置，不随应用回滚。
- 已切换后端镜像 `aimessage-backend:drawer-ip-20260915`，健康状态 healthy、重启次数 0；Web current 指向本次发布。
- 公网 HTTPS 登录页及注册弹窗 Playwright 验证通过，默认账号生成正常，无 JS 错误或资源失败；未提交注册、未创建测试账号或用户事件。
- 无效注册返回 400，未授权聊天任务状态 / 侧栏接口返回 401；未绕过登录检查。首次启动期间有短暂 502，健康后再发布新前端，最终检查正常。
- 发布后域名 SNI 本机检查 200，IP 公网校验通过，续签 timer 和 Nginx 均 active。

参考：
- https://letsencrypt.org/2026/03/11/shorter-certs-certbot
- https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsexceptiondomains
