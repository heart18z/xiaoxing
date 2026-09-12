# AI小醒 iOS 测试版交接

## 本期技术与范围

- Capacitor 8.5.1 + 现有 Vue 3 `/app` 页面，iOS 原生 WKWebView、Keychain、通知权限和推送点击事件。
- 页面资源随安装包发布，**没有使用远程 `server.url` 套壳**。业务 API 为 `https://www.chentong.xyz/api`，在 `code/front/.env.native` 配置；不是内网地址。
- Bundle ID：`com.dfyj.xiaoxing`；Team ID：`7U8S8PWU2W`；应用名：AI小醒。
- 直接对接 APNs，不依赖极光或 Firebase。Swift Package Manager 管理 iOS 依赖。
- 本期不包含 Android、Live Activities、静默推送、角标计数、离线业务编辑、商店正式发布。
- 这次开发没有改动线上部署；安装包需要配套新版后台，旧后台没有设备注册接口。

## 已实现的行为

登录后请求系统通知权限，获取 device token 并绑定当前用户。设置页面提供权限状态、再次检查和跳转系统设置入口。前台收到通知时刷新聊天和未读信息，点击通知进入所属事件；不同账号不能通过旧通知进入原账号事件。

登录 token、refresh token、userInfo、安装标识和解绑凭据只写入本机 Keychain，不写普通 localStorage/cookie。`.p8` **只放服务器**，不放 iOS 工程、网页资源、代码库或聊天中。

退出登录将解绑请求持久化，可在登录凭据过期后凭随机安装密钥重试。账号切换会更换服务端 binding ID，旧队列不会转发给新账号。离线退出后，已经交给 APNs 的通知无法撤回；因此锁屏只显示通用提醒，不含任务正文、人名或其他参与人的信息。

业务仍然由现有 AI 评估决定是否发消息、发给谁、内容是什么。仅当业务已经写入未读提醒/异常询问/反馈/事件分配消息时，在同一数据库事务写入推送队列；没有新增“只要改期就强制通知”的规则。后台工作线程每 5 秒处理队列，网络发送不占用创建事件请求；已读、解绑、账号失效的消息会跳过。429/临时网络故障退避重试，410 失效 token 停用，推送有效期 1 小时。站内消息仍长期保留，推送不是业务消息的唯一副本。

## Windows 上可以做什么

可以开发页面、Swift 源码、后台、SQL 迁移、生成 iOS 工程、构建网页资源、同步插件并执行离线回归。不能在 Windows 上编译 iOS SDK、完成苹果签名或验证真实系统通知。

Xcode 不只是导出 IPA，还提供 iOS SDK/Swift 编译器、模拟器、真机调试、签名、Archive 和上传流程。当前 Capacitor 8 官方要求 Node.js 22+、Xcode 26+；Mac 的系统版本也必须兼容所安装的 Xcode。

## Mac 操作

1. 安装 Xcode 26 或更新的兼容版本，首次打开并安装 iOS 平台组件、接受许可。在 Xcode → Settings → Accounts 登录组织内 Apple 开发者账号。
2. 将**当前最新项目**放到 Mac。可用团队私有代码库或安全拷贝；不要复制 Windows `node_modules`，不要把服务器备份、数据库导出、`.p8`、生产 `.env` 放进分发包。另保留组织授权的 npm 私有依赖访问配置，本项目的 `@saber/*` 包需要它。
3. 在项目 `code/front` 目录执行：

```bash
node --version
npx --yes pnpm@9.3.0 install --frozen-lockfile
npm run ios:sync
npm run ios:open
```

`ios:sync` 会重建 `dist-native` 并重新生成适合这台 Mac 的 Swift Package 路径；不要只打开 Windows 同步过的工程而跳过安装和同步。

4. Xcode 中选择 App target → Signing & Capabilities：核对 Team、Bundle Identifier、Automatically manage signing、Push Notifications。工程已有 `aps-environment` entitlement；不要为普通可见提醒开启不需要的后台模式。
5. 连接并信任 iPhone；如 Xcode 要求，开启 iPhone 开发者模式。先 Run 验证界面、登录、权限弹窗、麦克风和键盘布局。
6. 要上传 TestFlight，选择 Any iOS Device → Product → Archive → Distribute App → App Store Connect → Upload。每次上传递增 `CURRENT_PROJECT_VERSION`（Build）。等待 App Store Connect 构建处理，填写真实的测试说明、测试账号和出口合规问题，再添加测试员。

### 推送环境不可混用

| 安装方式 | 工程配置 | APNs 环境 | 后台密钥 |
|---|---|---|---|
| 常规 Xcode Debug 真机运行 | Debug | Sandbox | 另外申请/配置支持 Sandbox 的 key |
| TestFlight / App Store 分发 | Release | Production | 已下载的 Production key |

目前提供的信息只有 Production Key ID `RAR492G6T7`。没有 Sandbox key 时，仍可开发和调试页面，但 Debug 真机的远程推送不能用 Production key 替代。第一轮真实 Production 推送应通过 TestFlight 验证。

## 后台配置（上线前执行，不含私钥内容）

后端启动会**新增** `blade_smart_push_device`、`blade_smart_push_delivery` 两张表，不覆盖原有用户、聊天或事件。先按现有发布流程备份数据库，部署新版 JAR。默认 `enabled=false`，未配置 APNs 时网页业务继续工作。

将 `.p8` 通过安全方式放到服务器专用密钥目录，例如 `/opt/aimessage/secrets/apns-production.p8`，仅服务账号可读；容器使用只读 volume 挂载为 `/run/secrets/apns-production.p8`。不要将文件放在 Nginx 静态目录。配置环境变量：

```text
SMARTREMINDER_PUSH_ENABLED=true
SMARTREMINDER_PUSH_TEAMID=7U8S8PWU2W
SMARTREMINDER_PUSH_BUNDLEID=com.dfyj.xiaoxing
SMARTREMINDER_PUSH_PRODUCTIONKEYID=RAR492G6T7
SMARTREMINDER_PUSH_PRODUCTIONKEYPATH=/run/secrets/apns-production.p8
# 只有另行申请 Sandbox key 后才配置以下两项
# SMARTREMINDER_PUSH_SANDBOXKEYID=你的SandboxKeyID
# SMARTREMINDER_PUSH_SANDBOXKEYPATH=/run/secrets/apns-sandbox.p8
```

环境变量名按 Spring Boot 规则移除属性中的连字符，只将层级分隔点改为下划线。也可以挂载额外 YAML 并用 `SPRING_CONFIG_ADDITIONAL_LOCATION` 加载：

```yaml
smart-reminder:
  push:
    enabled: true
    team-id: 7U8S8PWU2W
    bundle-id: com.dfyj.xiaoxing
    production-key-id: RAR492G6T7
    production-key-path: /run/secrets/apns-production.p8
```

服务需可出站访问 Apple APNs HTTPS/HTTP2 443。Java 17 使用系统受信任证书，不关闭 TLS 校验。CORS 只为 `capacitor://localhost` 放行实际需要的接口和请求头；原网页请求保持原有处理。

设备注册 `POST /api/app/push/register` 必须是已登录 APP 用户，服务端取当前用户 ID，不接受客户端指定接收用户。解绑 `POST /api/app/push/revoke` 使用安装随机密钥哈希与 binding ID 校验，仅该接口允许 OAuth 过期后的解绑。

## 必须在 Mac / 真机完成的验收

- Xcode 构建、签名和 entitlement 校验；本次 Windows 未执行 Swift/iOS 编译。
- 同意/拒绝/重新开启通知权限，设置页状态正确。
- TestFlight 安装后登录；后台开关与 Production key 配置生效；锁屏、前台、后台都验证真实 APNs 通知。
- 点击通知进入正确事件；登录其他账号后点击旧通知不能进入原账号事件。
- 退出、离线退出后重连、切换账号、重装、token 变化的绑定行为。
- 语音录制权限、照片/文件上传、键盘遮挡、刘海和底部安全区、长事件详情滚动。
- iOS 专注模式、网络中断、权限关闭都会影响呈现；APNs 接受请求也不等同用户已看到。
- 原生包 API 到公网需要配置好 CORS；网页能打开并不能单独证明 Capacitor 跨域已通过。

首次图标仍为 Capacitor 工程默认占位图。上传测试前建议替换为正式 1024×1024 不透明应用图标，补齐测试说明、隐私说明与实际权限用途。正式上架另行检查隐私标签、账号删除等要求；不要为了跳过出口合规问卷直接声称应用没有使用加密。

## 离线验证入口

本次 Windows 已通过网页生产构建、原生网页构建及 `cap sync ios`、Maven 打包、下列离线回归，以及 Playwright 模拟桥接的通知设置/滚动/跳转/解绑检查。iOS 配置 XML 已检查；**没有执行 Xcode 编译、苹果签名或真实 APNs 发送**。设置页检查截图在 `output/playwright/native-settings.png`，仅代表模拟环境。

- `scripts/PushRegression.java`：事务回滚、设备绑定、跨账号隔离、重试、失效 token、ES256 签名。
- `scripts/NativeCorsRegression.java`：原生预检和浏览器兼容性。
- `npm run test:native`：通知路由输入校验与本地资源配置。
- `scripts/native-browser-check.js`：Playwright CLI 的模拟 native bridge 页面检查，不是真机推送测试。
- 现有 CreatorEvaluation / TaskChangeDelivery / EventPrivacy / ChatContext 回归用于确认没有改动 AI 业务规则。

参考：[Capacitor 环境要求](https://capacitorjs.com/docs/getting-started/environment-setup)、[iOS 开发](https://capacitorjs.com/docs/ios)、[Push Notifications 插件](https://capacitorjs.com/docs/apis/push-notifications)、[Apple APNs 请求](https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns)。
