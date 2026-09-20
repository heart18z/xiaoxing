# 手机体验优化（2026-09-14）

2026-09-14 已按用户要求部署后端及普通网页到阿里云供验收；未进行 Git 推送或 TestFlight 发布。部署及回滚记录见 `output/mobile-upgrade-deployment-20260914.md`。已安装的原生 App 仍需重新打包更新。

## 功能

- 登录页自主注册：人员号 4–32 位（字母、数字、下划线、短横线，首位字母或数字），姓名最多20字符，密码8–64字符，手机号/邮箱选填。按需求预填默认密码，建议用户主动更换。服务端固定租户000000和唯一的app_user角色，不接受客户端权限字段。
- 我的→重置密码：核对原密码、两次新密码；成功后退出并重新登录。不是管理员重置他人密码，也不宣称撤销所有设备会话。
- 退出登录二次确认；检查通知显示过程和检查结果，包括已绑定设备及网络失败。
- 事件/好友申请每10秒静默刷新；隐藏、离开页面停止；失败保留原内容，不重复弹加载层。
- 思考面板最大可见高度230px→138px，原有宽度不变，可滚动查看。
- 语音改成输入框内动画。iOS使用Speech框架实时回填，保留原草稿，不自动发送；麦克风/语音权限、取消、中断和超时均有处理。系统识别可能使用Apple服务器；网页端仍为录完后调用现有转写接口，不伪装为流式。手机识别最长60秒，网页最长90秒，文件最大20MB。
- 键盘改成单一布局控制：Capacitor Keyboard.resize=none，keyboardWillShow立即驱动页面高度变化，避免原插件延迟原生resize造成输入框跳入。设置表单使用minmax(0,1fr)，16px输入字体，防横向撑开及聚焦自动放大，保留用户主动缩放能力。

## 服务端上线顺序（必须）

1. 备份数据库。先单独执行 `sql/app-account-unique.sql` 第一条查重SELECT。
2. 如果存在重复账号，停止迁移，人工确认如何处理；脚本不会删除用户。包含软删除用户，防止旧身份被重新注册。
3. 只有查重无结果时执行唯一索引ALTER，一次即可。索引按原表不区分大小写排序规则处理，拦截并发注册及管理端重复写入。
4. 部署后端代码及配置变更：secure.skip-url仅增加`/app/account/register`；日志跳过`/app/account/**`以免记录密码；XSS跳过这两个JSON账号接口，避免密码特殊字符被改写。不要覆盖现有环境密钥配置。
5. 注册接口在缺少唯一索引时主动拒绝注册。内置每实例10分钟注册30次/来源IP、修改密码8次/用户保护；反向代理环境还应设置可信客户端IP与网关限流，不能盲信客户端X-Forwarded-For。规模扩大后需要共享限流。

## Mac重新打包

同步本次前端源码、`capacitor.config.json`、`ios/App/App/AppDelegate.swift`，合并Info.plist中新增加的`NSSpeechRecognitionUsageDescription`，保留你在Mac上设置的图标、签名、团队、版本号。SceneDelegate必须继续使用`XiaoxingViewController()`。

在前端目录执行`npm run ios:sync`，提高Build编号后Archive并上传TestFlight。单独更新网页资源不能给旧安装包增加Swift语音桥接和语音权限。不要上传.p8、签名私钥、真实环境配置、构建输出或整个用户目录。

## 验证

- 前端：`node --test scripts/account.test.mjs scripts/silent-refresh.test.mjs scripts/native.test.mjs scripts/mobile-display.test.mjs`（在code/front下）。
- 后端生产包：`mvn -B -Dmaven.test.skip=true package`。仓库原有测试模板无法编译，因此另用`AccountRegression.java`独立验证真实服务和Bean Validation；H2隔离库，无生产账号/推送/模型调用。
- 浏览器：`scripts/mobile-upgrade-browser-check.js`通过Playwright CLI运行，使用原生桥接和API模拟；必须服务于dist-native，而不是普通开发index.html。覆盖注册重复错误、两次密码、取消退出、静默刷新、窄屏设置、通知反馈、语音部分结果替换、键盘willShow布局。
- Windows不能运行Xcode，也不能证明真实iPhone Speech服务和键盘动画效果。发布前在真机检查权限拒绝/允许、边说边出字、取消不丢草稿、切后台停录、蓝牙/电话中断、键盘开合及横竖屏。旧语音模型配置错误仍可能影响网页端文件转写。

Apple Speech参考：[Apple官方文档](https://developer.apple.com/documentation/speech/sfspeechrecognizer)

本地验收结果：前端11项Node测试通过；H2账号测试通过（含并发唯一、大小写重复、校验和密码哈希）；Playwright 9组交互检查通过，320/390px设置页无横向溢出、思考区138px、无页面运行错误；前后端生产构建通过。浏览器截图位于`output/playwright/upgrade-*.png`，不包含真实账号或线上调用。
