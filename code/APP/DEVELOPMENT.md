# AI小醒 uni-app x 开发说明

工程目录为 `code/APP`，HBuilderX 5.24，DCloud AppID 为 `__UNI__93FE60B`。接口契约保留在 [README.md](README.md)。原后端和 `front` 未作迁移性修改。

## 启动

1. 在 HBuilderX 中导入此目录，等待 uni-app x、UTS Android 运行扩展安装完成。
2. `运行 → 运行到浏览器` 可检查页面；`发行 → 网站` 生成本地 Web 产物。Web 只是开发预览，推送、闹铃、文档选择、录音使用手机原生能力。
3. Android 使用 Android 6.0+ 设备运行；iOS 原生插件须使用包含本插件的自定义基座或正式包，不能以普通 Web 预览代替验证。
4. 默认 API 为 `config/environment.uts` 中的现有 HTTPS 地址。局域网调试需按当前机器网络改地址，并确保设备能访问服务器。

本地代码检查：

```powershell
npm ci --ignore-scripts
npm run check
npm test
```

HBuilderX CLI（本机安装目录）：

```powershell
& 'D:/HBuilder X/cli.exe' launch web --project $PWD.Path --compile true
& 'D:/HBuilder X/cli.exe' publish web --project $PWD.Path --webHosting false
& 'D:/HBuilder X/cli.exe' launch app-android --project $PWD.Path --compile true
```

`publish web --webHosting false` 仅生成 `unpackage/dist/build/web`，不上传。`npm run check` 检查 Vue/UTS 语法、路由与 JSON，不能替代 HBuilderX 原生编译。

## 实现对应

| 模块 | 新工程位置 | 实现 |
|---|---|---|
| 登录/注册 | pages/login、AccountForm | SM2 mode 0 登录、验证码、记住账号、注册建议账号、密码显隐 |
| AI 聊天 | pages/chat、services/chat | 附件、语音转写、持久化 requestId、POST SSE/状态轮询、恢复等待、停止与清除上下文 |
| 事件 | pages/events、pages/event-detail | 发起/接收、搜索、分支、时间轴、对话查看、停止事件 |
| 提醒抽屉 | ReminderDrawer、DateRange | 分类、日期范围、分支过滤后事件去重 |
| 我的 | pages/me、AvatarPicker | 资料、头像、好友搜索/申请/备注/权限/解除 |
| 设置 | pages/settings、ModelSettings | 修改密码、语言、通知状态、系统/个人模型、额外 JSON 校验 |
| iOS 原生 | uni_modules/xiaoxing-native | Keychain、直接 APNs、文件选择、原有 AlarmKit 逻辑迁移 |
| Android 原生 | 同上 app-android | Keystore AES-GCM 安全存储、系统文档选择器；普通业务页面兼容 |

图片沿用旧工程，头像许可文件保留在 `static/avatars`。历史消息保持原文，语言选择影响界面文案与后端偏好。Markdown 使用经过转义的基础富文本渲染（换行、加粗、行内代码），不执行消息中的 HTML。

## 任务与账号隔离

发送前持久化内容、附件 ID 和 requestId。断网/进入后台只停止观察；恢复先查任务状态，复用原 requestId。只有确定成功/失败/取消才清除等待；UNKNOWN 和鉴权异常保留供用户核对。SSE snapshot 替换思考内容，RUNNING 继续观察，不作为最终正文。

原生账号数据保存于 Keychain/Keystore；Web 预览仅用 sessionStorage。切换账号清空界面缓存，待处理任务按 userId 分开。推送注销使用持久化安装凭据和 bindingId，失败留在队列重试。

## iOS 签名与升级验收

包名 `com.dfyj.xiaoxing`；沿用原 Apple Team 和授权，才能验证覆盖安装、Keychain 与闹铃记录延续。插件读取旧 Capacitor 安装凭据和推送绑定，首次进入新版要求重新登录并撤销旧绑定。AlarmKit 继续使用旧记录键和账号归属键。

`config/environment.uts` 的推送环境、插件 `Info.plist` 的 `APNsEnvironment`、`UTS.entitlements` 的 `aps-environment` 以及实际签名必须一致。目前配置为 production；开发签名须一起调整为 development。APNs p8、服务端密钥不进入客户端。

AlarmKit 需要 iOS 26+ 及带相应 SDK 的编译环境。只为自己接收的 ACTIVE 分支设置未来闹铃；显式空分支时间不得回退到事件时间。系统授权失败应保留普通提醒能力。

Windows 上未完成 Swift/iOS 签名构建与真机验收。需在支持 iOS 构建的环境逐项验证：

- APNs 首次授权、拒绝后开启、前台/后台/杀进程点击、账号切换不串号。
- AlarmKit 设置/更新/取消、退出登录、覆盖安装、系统修改后的同步。
- 相机/相册/录音权限、文档选择取消、大文件拒绝、附件失败重试。
- 断网发送、弱网、后台恢复、refresh token 失效、重复点击确认与冲突再确认。

Android 未实现独立的远程推送通道或系统精确闹铃，界面明确标为不支持；复用 iOS APNs 不能实现 Android 通知。

## 验证记录

### 2026-09-18 登录与聊天细化

第三轮：顶部 TopTip 动画反馈替代系统 toast；聊天进场/切回使用显式底部定位和真实视口测量；确定 FAILED 恢复草稿并清除等待栏，未知结果仍保留幂等恢复。新增 ReplyProgress 对齐原 /app/chat 四阶段与圆环、扫光。提醒列表固定标签尺寸并约束标题，通知卡片补齐内框，候选闹铃改复选框。26 项测试、Web/Android 构建与浏览器关键流程通过。内网模型 401 经用户切换默认模型后，真实候选确认创建及详情验证成功，测试数据已清理。发布记录见 ../../deploy/LAN-CHAT-20260918.md。

追加修复：真实浏览器发现同 IP 旧版 Cookie 跨端口干扰新版 `Blade-Auth`，导致 OAuth 200 后 bootstrap/refresh 401。内网 4003 专用代理已隔离旧版认证 Cookie；保留旧 Cookie 的实际重复登录流程、成功/失败 toast 验收通过。新令牌初始化失败不再刷新或跳转清除错误提示。登录弹窗精简，公共 Popup 加入 180ms 开关过渡，登录面板加入 240ms 进场效果。25 项测试与 Web/Android 编译通过，部署详情见 ../../deploy/LAN-20260918.md。

登录补入品牌装饰线，忘记密码改用共享弹层。重复登录识别 OAuth `need_confirm_login` / `601`，展示二次确认；取消不重试，只有明确继续才发送 `confirm: true`。临时密码仅在页面内存中保存。

聊天思考改为消息气泡内展开、限高滚动，流式期间自动跟随并显示闪动状态。按原 front 的提醒、反馈、异常、冲突、分配与系统消息，以及候选确认/取消/过期、长期跟进、时间冲突分支调整卡片和按钮，保留真实接口交互。

22 项 Node 测试、19 个页面/组件与 21 个 UTS 模块语法检查通过。Web 浏览器覆盖内联思考、消息卡片、登录确认请求头及 320/402/768px 页面宽度；Web 和 Android 经 HBuilderX 编译。真实内网临时账号验证重复登录挑战、取消时原会话有效、确认后登录成功，验证账号已清理。iOS 真机视觉与授权仍需设备验收。

### 2026-09-17 截图复刻修订

根据提供的 17 张原版截图重新调整了公共框架、背景、底部导航、事件卡片、详情/时间轴/对话、聊天输入区、提醒抽屉、好友页面、资料及权限弹层、设置选择器、登录和注册。新增 `UiIcon`、`SelectField`，扩展 `Popup` 的底部面板和事件对话布局。沿用 uni-app x 原生基础控件与兼容的共享组件；未引入原版 Element Plus 的 Web 依赖。

线条图标由原 `front` 的 SVG 路径生成 PNG，供原生与 Web 共用。生成脚本为 `tests/build-icons.mjs`（依赖 devDependency `@resvg/resvg-js`）；运行时无需这个依赖。原素材、AppID、后端接口及原生插件保持原有配置。

本地视觉对照页：[17 个页面状态对照](output/visual-review.html)。右侧为 402×874 Web 实际渲染，安全区由测试脚本补入；未伪造系统状态栏。Windows 字体、模拟头像及原生权限状态与 iOS 截图不同，尚未完成 iOS 真机像素级验收。

交互检查使用完全隔离的 API 响应，验证资料必填校验、资料/备注保存、权限方向、系统模型 ID、清除上下文取消和确认；没有修改生产数据。可重复执行的流程在 [tests/visual/README.md](tests/visual/README.md)。

本轮最终验证：Android 于 16:24、Web 于 16:25 编译成功，未出现新增 CSS 错误或警告。18 个页面/组件、21 个 UTS 模块通过语法检查；19 项回归测试通过。17 个截图状态及模型菜单遮挡检查通过，320/402/768px 的 15 个页面组合没有横向溢出或脚本异常。构建日志保存在 `output/android-redesign-final.log`、`output/web-redesign.log`。

模型下拉需让 `system-fields` 和其父容器允许内容溢出，避免选项被原生默认裁切。提醒标题采用原生 `lines: 2`，Web 条件编译采用 `-webkit-line-clamp`，参考 [DCloud text 说明](https://doc.dcloud.net.cn/uni-app-x/component/text.html) 与 [overflow 说明](https://doc.dcloud.net.cn/uni-app-x/css/overflow)。

2026-09-17：19 项 Node 回归测试通过，覆盖 SM3/SM2 互通、分片 UTF-8/SSE、任务幂等与账号切换、后台暂停、UNKNOWN/401 保留、北京时间、提醒分组、闹铃归属与空时间规则、无效日历日期拒绝、空存储、并发 token 刷新、刷新网络异常保留会话、迟到响应隔离、multipart 请求头。

Web 发行编译通过。浏览器使用本地模拟响应检查六个主页面与弹层；截图位于忽略提交的 `output/playwright/`。没有使用真实账号创建事件或修改生产资料。真实服务器联调、原生授权和签名发布仍应按上列步骤验收。

HBuilderX 5.24 验证：Android 全部 6 个页面及 Kotlin 原生插件已编译成功；iOS 单插件编译命令返回成功（不代表完整 Swift 签名安装包验收）。Android 文件选择器使用系统 Fragment，编译器有 deprecated 提示，功能需在设备上验证。Web 在 320/390/768 宽度检查无横向溢出，最新源码预览无运行错误；提醒抽屉和日期弹窗已检查。
