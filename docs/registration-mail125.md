# 注册邮箱验证与动态 SMTP（APP 125）

## 使用

后台进入“智能提醒 → 注册邮箱验证”。配置 SMTP 主机、端口、加密方式、发信账号、密码/授权码、发件人邮箱和显示名称，保存后立即生效。密码加密保存、接口不回显；再次保存时密码留空保留原值。配置和测试接口仅管理员可访问，请求日志排除这些路径。

可先保持“启用验证码发送”为关闭，保存配置并发送测试邮件，确认收到后启用。发送测试使用已保存配置。未启用时注册验证会失败关闭，不允许免验证码注册；已有账号的登录和修改密码不受影响。

本次按用户要求先配置 QQ SMTP（587 / STARTTLS）用于测试。授权码只在部署时加密写入数据库，没有写入源码或文档。以后切回公司邮件只需在此页修改设置，不需要重新部署。

## Zimbra 账号与参数

1. 由邮件管理员登录 Zimbra 管理控制台（通常为 `https://mail.sinoaopt.com:7071`，以公司实际管理入口为准），通过“管理 → 账号 → 新建”或“添加账号”创建普通专用邮箱 `fmemo@sinoaopt.com`。已创建则无需重复创建，不需要管理员权限。
2. 为该邮箱设置密码；服务账号不应处于“下次登录必须修改密码”的待修改状态。如果公司启用额外认证，使用管理员规定的 SMTP 应用密码。
3. 在服务器 MTA 设置中确认 SMTP 身份认证启用，并使用 TLS 连接。实际端口同时取决于服务器监听、防火墙及代理。
4. 本次从应用服务器实测：`mail.sinoaopt.com:587`（STARTTLS）与 `:465`（SSL/TLS）均成功协商 TLS 1.3、证书校验正常，且通告支持 LOGIN/PLAIN 身份认证。尚未用该 Zimbra 邮箱密码完成发信认证。
5. 建议填主机 `mail.sinoaopt.com`（不带 https:// 和路径）、端口 `587`、STARTTLS、账号和发件人均为 `fmemo@sinoaopt.com`，名称“AI小醒”。输入密码后保存，向可查收的邮箱发送测试邮件。

参考：[Zimbra 管理控制台](https://wiki.zimbra.com/wiki/Administration_Console)、[创建账号](https://wiki.zimbra.com/wiki/Zimbra_Releases/8.7.0/Single_Server_Installation)、[端口](https://wiki.zimbra.com/wiki/Ports)、[SMTP 身份认证](https://wiki.zimbra.com/wiki/Zimbra_MTA)。不同 Zimbra 版本菜单名称可能略有区别。

## 注册行为

- 邮箱必填，手机号选填。输入停止约 500ms 或离开字段时查询唯一性，不修改正在输入的文本；旧请求结果不能覆盖新输入。
- `/app/account/check-contact` 只返回规范化后的值和是否可用；不返回账号资料。提交时保留数据库唯一索引与服务端复查，避免并发绕过。
- `/app/account/email-code` 发送 6 位随机码；有效期10分钟，60秒可重发，每邮箱每小时最多5封，每来源地址每小时30封，总量每小时300封。限额与挑战状态存数据库，重启不会清零。
- 验证码摘要使用服务端密钥 HMAC，不明文存储，也不在接口响应返回。只有 SMTP 提交成功的验证码可以使用；更换邮箱或重新发送后旧验证码不再适用。
- `/app/account/register` 必须提供 `emailCode`。连续输错5次需重新发送；失败次数不会随异常回滚。正确验证码消费与用户创建在同一事务中：仅注册成功才失效，用户创建失败时允许修正后重试。
- 注册成功仍自动登录，并随机使用现有头像库。

## 发布与验证

- APP versionCode 125，需要重新打包安装；旧版没有验证码字段，不能再完成新用户注册。iOS资源构建不是签名IPA或真机验证。
- 新增 `RegistrationMailSchema` 以非破坏方式初始化3张表；`sql/registration_mail_v125.sql` 添加后台菜单与管理员菜单授权。没有批量修改历史账号。
- 新增依赖 `org.eclipse.angus:jakarta.mail:2.0.3`；部署补丁包含该库和 Spring Boot classpath 索引。TLS 证书/主机校验开启，不允许明文 SMTP。
- 70项APP测试、Android资源及Kotlin编译、iOS资源编译与持续回调检查通过；网页生产构建通过。
- Java真实事务/H2回归覆盖：必填码、错误/过期码、邮箱绑定、尝试上限、一次性消费、创建失败回滚、同码并发、发送失败、持久化限流、占用检测、密码加密及不回显。原账号与账号建议回归通过。
- Playwright在390×844浏览器中验证：提交前占用提示、发送倒计时、修改邮箱清空验证码，注册按钮可见。使用模拟接口，没有创建真实账号。
- 生产验证码接口通过QQ SMTP先完成发件邮箱自身回环发送，又按用户指定发至 `49823778@qq.com`，接口均成功；最终到达收件箱由用户确认。匿名读取邮件配置返回401；缺少验证码的注册返回400。
- 镜像 `aimessage-backend:registration-mail-20260923-125`，网页 `/var/www/aimessage/releases/registration-mail-20260923-125/dist`，备份 `/opt/aimessage/backups/pre-registration-mail-20260923-125`。
