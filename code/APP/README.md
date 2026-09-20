# AI小醒 · uni-app x 前端重写对接

更新：2026-09-17。后端基准：Git `main` 提交 `649753b`，阿里云版本 `chat-live-20260916`。
本期只重写 App 前端；暂停“工具调用后生成流式正文”的后端改造。本文不含登录密码、API 密钥或私钥内容。

工程已按以下对接规范实现。运行方式、平台限制及验收记录见 [DEVELOPMENT.md](./DEVELOPMENT.md)。本文保留原接口与验收约定。

## 1. 目录与边界

```text
code/
  back/   现有 Java 后端，继续复用
  front/  管理后台；迁移期保留原 /app 页面，作为样式和业务参考
  APP/    新 uni-app x 工程（.uvue + UTS，开发与验证说明见 DEVELOPMENT.md）
```

新 App 建议划分 `pages/`、`components/`、`api/`、`store/`、`static/`、`uni_modules/`。使用 `.uvue` 页面、UTS 逻辑；不是把原 Vue/Element Plus 页面放进 WebView。原有 DOM、CSS、Axios、Capacitor 调用不能直接照搬，图片和业务规则可以复用。[DCloud 页面规范](https://doc.dcloud.net.cn/uni-app-x/vue/index.html)

App 只连接现有 HTTP API，不直连数据库、Redis、模型供应商或 APNs 服务端。先完成迁移验收，再单独清理 `front` 中旧 App 代码，当前不要删除。

## 2. 复刻范围：6 个主页面 + 配套弹层

原页面统一位于 `code/front/src/page/smart-reminder/`，以下文件名相对此目录。

| 新页面 | 原页面 | 必须保留的功能 |
|---|---|---|
| 登录 | `login.vue` | 记住上次账号、密码眼睛、注册弹窗、生成可修改账号、手机号/邮箱至少一项且唯一；忘记密码目前仅提示联系管理员 |
| AI 对话 | `chat.vue` | 文本/图片/附件/语音、思考折叠、消息类型卡片、候选事件确认、停止生成、清除上下文、历史同步、息屏恢复 |
| 我的事件 | `events.vue` | 我发起的/我收到的、活动/全部筛选、搜索、数量、最新进展、详情跳转 |
| 事件详情 | `event-detail.vue` | 按当前用户角色展示时间/任务、接收人分支、进展和时间线、查看对话、停止事件、本机闹铃状态与时间 |
| 我的 | `me.vue` | 头像/AI头像、个人资料、好友及申请、现有好友搜索、添加好友、单个“管理”按钮、备注、权限申请、退出确认 |
| 设置 | `settings.vue` | 修改密码、通知权限、界面语言、LLM/语音系统模型选择及个人配置 |

底部保持 **事件 / AI助手 / 我的** 三个 Tab；事件图标保留数字，AI 图标复用 `code/front/public/images/ai-assistant-tab.png`。背景图也在该 `images/` 目录。

必须一起复刻的组件：

- 提醒侧栏 `ReminderDrawer.vue` + 中文日期范围 `ReminderDateRange.vue`：全部/我发起的/我收到的；今日待提醒、今日已提醒、明日待提醒、历史创建未到期；列表独立滚动，箭头进入详情。分组逻辑参考 `reminderGroups.mjs`，不要凭创建日期重新推断提醒状态。
- 注册、个人资料、好友“管理”（基本资料/备注/权限/删除）、新增好友、头像选择、事件关联对话弹窗；对话弹窗提前固定合理高度，避免加载后突然拉长。
- 统一 App 提示、确认框、加载/空态；刘海安全区、键盘弹出后最新消息可见、Tab 点击反馈和页面切换。
- 搜索现有好友可用姓名、备注；添加好友只用账号/手机号/邮箱，不能用姓名全库搜索。好友备注和聊天别称都是当前用户私有、后端持久化的数据，不能仅存本地。

## 3. 接口约定与入口

生产 `baseURL = https://47.100.172.149/api`；内网联调 `http://192.168.15.50:4002/api`（HTTP 仅联调，iOS 需单独核对网络安全限制，不以关闭全部 TLS 校验解决问题）。接口路径统一不再重复加 `/api`。

- 基础请求头：`Authorization: Basic <现有客户端凭据编码>`、`Blade-Requested-With: BladeHttpRequest`；登录后加 `Blade-Auth: bearer <access_token>`。客户端 ID 为 `saber`，默认租户 `000000`。配置定义见 `front/src/config/website.js`，不把服务端模型密钥当成客户端凭据。
- **登录密码不是直接传明文，也不是 MD5**：现有实现为 SM2 加密（`front/src/utils/sm2.js`，模式参数 `0`）。需用可在 UTS 运行的等价实现，并核对服务端公钥/密文格式。登录参考 `front/src/api/user.js`、`front/src/store/modules/user.js`。
- 登录：`POST /blade-auth/oauth/token`，参数 `tenantId,username,password,grant_type=password,scope=all,type=account`，头 `Tenant-Id: 000000`；若后台开启验证码则按旧登录分支对接。刷新使用同入口 `grant_type=refresh_token`。OAuth 返回 token 字段，不能按普通业务 `data` 包装解析。
- 业务接口通常返回 `{code:200,success:true,data:...}`，同时检查 HTTP 状态和业务 `code`；401 刷新一次，失败回登录；不要将所有错误统称“网络中断”。有效期以服务端 `expires_in` 为准，安全存储 token/refresh token，不仅依赖 Cookie。
- 所有用户/事件/分支/候选 ID 按字符串保存传递，避免长整型精度丢失；服务端业务时间按北京时间解释，显示时不得自行套用手机时区造成改期。

下表**全部 POST**；`Q` 为 URL 参数，`J` 为 JSON，`F` 为 multipart。业务路径默认前缀 `/app/reminder/`；完整参数以 `front/src/api/smartReminder.js` 和后端 `SmartReminderDtos.java` 为准。

| 功能 | 接口与入参 |
|---|---|
| 注册/推荐账号/改密码 | `/app/account/register` J `{account,name,password,phone,email}`；`/app/account/suggest-account`；`/app/account/password` J `{oldPassword,password,confirmation}`。这组密码按现有账户接口传原字符串，仅走 HTTPS，不套用 OAuth 的 SM2 密文 |
| 初始化/资料 | `bootstrap`；`profile/update` J `{name,phone,email,avatar,aiAvatar}`（按需提交） |
| 聊天与恢复 | `chat/jobs/submit` J `{requestId,content,fileIds}`；`chat/jobs/status`、`chat/jobs/watch` J `{requestId}`；`chat/stop` J `{requestId}` |
| 历史/已读/清上下文 | `chat/messages` Q `limit`；`chat/sync` Q `limit,revision`；`chat/read` J `{messageIds}`；`chat/context/clear` |
| 文件/语音 | `files/upload` F `file`；`audio/transcriptions` F `file`；头像为 `/blade-resource/oss/endpoint/put-file` F `file` |
| 确认事件卡 | `candidate/confirm` J `{candidateId,acceptConflicts:false}`；出现冲突询问后，由用户明确确认才传 `true` |
| 事件 | `events` Q `type=sent/received,status`；`events/counts` Q `type`；`events/drawer` Q `type=sent/received`；“全部”由前端合并两类，复用旧分组规则 |
| 详情/对话/停止 | `event/detail` Q `eventId`；`event/conversation` Q `eventId,participantUserId`；`event/stop` J `{eventId,branchId?,reason?}` |
| 好友 | `friends/list`、`friends/requests`；`friends/search` Q `keyword`；`friends/request` J `{targetUserId,permissionMode,message}`；`friends/reply` J `{requestId,accept}` |
| 备注/删除 | `friends/remark` J `{targetUserId,remark}`；`friends/remove` J `{targetUserId}`。权限变更复用 `friends/request`，不是客户端直接改权限 |
| 设置 | `settings/models`；`settings/models/save` J，字段和模式见 `SmartReminderDtos.ModelPreferenceRequest` |

好友权限枚举：`I_CAN_REMIND` 我可提醒对方；`THEY_CAN_REMIND` 对方可提醒我；`MUTUAL` 互相提醒。列表返回值按当前登录用户视角展示。

### 当前聊天协议：不要按尚未实现的新方案对接

1. 发送前持久化消息和 `requestId`（16–80 位字母、数字或短横线）。同一消息重试必须复用原 ID；先查状态，`NOT_FOUND` 才提交。
2. 状态：`QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED/UNKNOWN`；正常排队不显示顶部横幅。息屏或网络断开只停止观察，**不调用停止接口、不重新执行业务**。
3. `watch` 为带鉴权的 **POST SSE**：`ready`、`snapshot{reasoning}`、`result{data}`。思考快照替换而非追加；`result` 也可能是 `RUNNING`（约 25 秒观察周期结束），此时继续观察，不当成业务完成。
4. `SUCCEEDED` 的 `result` 含正文与卡片结果。**当前思考实时返回，正文仍是最终完整结果；新的工具调用+正文 token 流式尚未实施。** 保留可扩展事件解析层即可，本期不要私自改变后端协议。
5. SSE 按 UTF-8 连续解码、空行分帧，支持半个字符/半条 JSON 跨网络块；不得将单个 chunk 当完整消息。失败回退 `status`，刷新历史后按 ID 去重。

uni-app x 的 `RequestTask.onChunkReceived` 官方标注 App iOS/Android 自 HBuilderX 4.71 支持；选择支持该能力的版本并真机验收 POST SSE，不沿用浏览器 `fetch/ReadableStream`。[DCloud 请求与流式说明](https://doc.dcloud.net.cn/uni-app-x/api/request.html)

## 4. 原生能力迁移（不能只复刻页面）

| 能力 | 对接要求 |
|---|---|
| 录音/照片/文件 | 原生录音，确认即停录并上传后台转写；取消即丢弃；失败回输入框，保留草稿；权限声明齐全，真机验证拍照不闪退 |
| 安全存储 | 用 Keychain 等安全存储替代 `front/src/native/secureState.js` 的 Capacitor 桥；保存 token、安装 ID/随机安装密钥、待处理消息、待解绑记录 |
| APNs | 保留现有**直接 APNs**后端，不默认换成 uni-push；若取得的是推送厂商 clientId，不能当 Apple device token 注册。用 UTS/iOS 原生接入真实 device token、通知权限和点击回调 |
| 推送接口 | `POST /app/push/register` J `{installationId,installationSecret,token,environment,bundleId,language,appVersion}`；`revoke` J `{installationId,installationSecret,bindingId}`。登录绑定、切账号重绑、退出解绑失败持久重试；点击旧账号通知不得串号 |
| 系统闹铃 | 将现有 iOS AlarmKit 能力封装成 UTS 插件；仅 iOS 26+ 且用户授权后设置本机闹铃，低版本明确提示不支持。按本人接收分支时间设置；不是写入苹果“时钟”列表。参考 `front/src/native/alarms.js`、`alarmPlan.mjs`、`AlarmControl.vue` 与 `front/ios/` 原生源码 |

UTS 插件支持封装原生平台能力，需重新适配原 Capacitor 插件入口；不能假定换框架后推送/闹铃自动可用。[DCloud UTS 插件说明](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html)

如继续作为原 App 更新，保持 Bundle ID `com.dfyj.xiaoxing`、Apple Team `7U8S8PWU2W`，并核对签名、Keychain 访问组、推送 entitlement、旧闹铃记录迁移；换新 Bundle ID 需要单独配置服务端并重新绑定设备。

## 5. 服务器、数据库与 Nginx 交接

阿里云配置于 **2026-09-17 只读核验**；内网依据 09-11 最后发布记录和本地 dev 配置，SSH 无可用凭据，未核验现有容器。两库独立，09-11 曾手动同步快照，**不是实时复制，禁止为开发直接覆盖生产库**。

| 项目 | 阿里云生产 | 内网联调（现状待复核） |
|---|---|---|
| SSH/SFTP | `ssh root@47.100.172.149`，22，密码由管理员安全提供 | 历史部署账号/路径指向 `rooter`；`ssh rooter@192.168.15.50`，22，凭据待提供 |
| 网页入口 | `https://47.100.172.149/app/login`；管理后台 `/login`，首页 `/wel/index` | `http://192.168.15.50:4002/app/login`；管理后台同端口 |
| 部署根 | `/opt/aimessage`；`compose.yaml`、私有 `.env` | `/home/rooter/aimessage` |
| 当前/最后记录版本 | `releases/chat-live-20260916`；镜像 `aimessage-backend:chat-live-20260916` | `releases/event-dialogs-20260911-104800/docker-compose.yml`（历史） |
| 后端 | 容器 `aimessage-backend:8080`，宿主 `127.0.0.1:18080` | 容器 `backend:8080`，由 web 代理访问（历史） |
| MySQL | MySQL 8.4；容器 `mysql:3306`；宿主 `47.100.172.149:15432`；库 `ai_message_reminder`；应用用户 `app` | dev 配置 `192.168.15.50:3306/ai_message_reminder`，用户 `root`；09-11 记录 MySQL 8.2 |
| Redis | 容器 `redis:6379`；宿主 `47.100.172.149:16380`；应用 DB `2` | dev 配置 `192.168.15.50:6379`，DB `2` |
| 附件 | Docker 卷 `aimessage_uploads` → 后端 `/app/uploads` | `/home/rooter/aimessage/shared/uploads` → `/app/uploads`（历史） |
| Web/代理 | **宿主机 Nginx**：`/var/www/aimessage/current` → `/opt/aimessage/releases/chat-live-20260916/web/dist`；`/api/` 去前缀转发 `127.0.0.1:18080` | 最后发布为 **Node 代理，不是 Nginx**：`aimessage-web`，宿主 `4002` → 容器 `3000`；`/api/` 转发 `backend:8080` |

生产密码不在本文：应用 DB/Redis 连接由 `/opt/aimessage/.env` 配合 Compose 注入；数据库管理员凭据由服务器管理员管理。阿里云 MySQL/Redis 当前容器映射监听 `0.0.0.0`，不等于公网防火墙已放行；管理连接优先 SSH 隧道，不新增公网开放规则：

```bash
ssh -N -L 13306:127.0.0.1:15432 -L 16379:127.0.0.1:16380 root@47.100.172.149
# 数据库客户端连接 127.0.0.1:13306；Redis 连接 127.0.0.1:16379，均使用单独取得的凭据。
```

Nginx 生产站点：`/etc/nginx/sites-available/xiaoxing-ip-https`（IP）、`chentong.xyz`（域名）、`default`；由 `sites-enabled` 软链启用。域名 `www.chentong.xyz` 保留，App 主连接使用 HTTPS IP。API 超时 300 秒，观察接口返回 `X-Accel-Buffering: no`；迁移不改代理、不缓存 SSE。

IP TLS 证书目录 `/etc/letsencrypt-ip/live/47.100.172.149/`，文件 `fullchain.pem` / `privkey.pem`；自动续期 `xiaoxing-ip-cert-renew.timer` 已核验 active，短期证书必须保持续期。域名证书目录 `/etc/letsencrypt/live/www.chentong.xyz/`，不要混用证书。

发布流程：备份 → 构建并校验配置 → 确认活动聊天任务为 0 → 更新 JAR/容器 → 等 healthy → 原子切换静态目录 → 检查入口/鉴权。参考 `scripts/deploy-chat-live.py` 及依赖脚本；**下次发布必须使用新版本标签，不能重跑旧标签覆盖备份**。当前回滚备份 `/opt/aimessage/backups/pre-chat-live-20260916`；应用回滚不自动恢复数据库。新 App 通过安装包发布，不能靠替换 Nginx 静态文件更新原生页面。

## 6. APNs 密钥位置（已核验）

| 项目 | 值 |
|---|---|
| 阿里云宿主私钥 | `/opt/aimessage/secrets/apns-production.p8`，文件存在 |
| 后端容器路径 | `/run/secrets/apns-production.p8`，只读挂载 |
| Key ID / Team ID | `RAR492G6T7` / `7U8S8PWU2W`（标识，不是私钥） |
| Bundle ID / 开关 | `com.dfyj.xiaoxing` / `SMARTREMINDER_PUSH_ENABLED=true` |
| 配置位置 | `/opt/aimessage/compose.yaml` 的 `SMARTREMINDER_PUSH_*` 与只读挂载；其他私有运行配置在 `.env` |
| Sandbox / 内网 | 本次未发现生产 Compose 配置 Sandbox key；内网 APNs 私钥位置未核验，不能假定存在 |

`.p8` 只供后端向 Apple 签名发送，**不能复制进 `code/APP`、前端静态目录或 Git**。App 不需要这份私钥；App 签名证书/描述文件与 APNs 私钥不是同一物品。TestFlight 使用 Production；Debug 推送需单独核对 Sandbox 能力，不能混用环境。

## 7. 交付顺序与验收

先完成“登录/SM2/安全存储 → 聊天任务+SSE → 事件 → 我的/好友/设置”，再接原生录音、照片、APNs、AlarmKit。首轮用测试账号和测试事件，不能拿生产用户批量改数据。

验收至少覆盖：6 页面与弹层视觉；键盘/安全区/滚动；息屏重进同一任务无重复创建；思考快照不重复；发起人与接收人时间/权限正确；好友私有备注；退出后推送解绑；拍照录音权限；低版本闹铃降级。iOS 15 等最低系统支持范围须按所选 HBuilderX/插件版本真机确认，不能由“改用 uni-app x”推定兼容。
