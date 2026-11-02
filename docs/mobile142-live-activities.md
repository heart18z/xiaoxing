# 小醒临近事项与实时活动

## 2026-11-02 原生交接接入

已接收 `XiaoxingLiveActivity-1.0.143-143-Windows.zip`，将完整扩展、ios-extension.json 和扩展描述文件接入本地 APP 工程，并合入构建脚本元数据修正。11 个交付文件 SHA-256 全部通过，扩展为 arm64 / iPhoneOS，主 APP、扩展、云配置均为 1.0.143 / 143。Swift 界面及共享数据结构与本地源码一致（仅换行符差异）。同事提供的日志记录 Mac 编译及本地 ad-hoc 签名检查通过；Windows 未独立执行 codesign。描述文件为 App Store 分发，2027-10-15 到期，需匹配原 Team 和云打包证书。尚未验证云签名 IPA、TestFlight 上传和 iPhone 实机展示。以下 11 月 1 日缺少产物的描述为接入前记录。

## 2026-11-01 打包核对

当前工作区只有扩展源码，没有 `XiaoxingLiveActivityExtension.appex`、扩展描述文件或 `nativeResources/ios/ios-extension.json`。直接使用 HBuilderX 云打包不会自动编译并嵌入这个 WidgetKit 扩展，因此已安装的 142 包不能仅通过网页后台打开实时活动。普通消息推送正常不代表实时活动已具备发布条件。

服务端逻辑已随 `weekly142-20261031` 部署；以下原生发布步骤仍须完成。当前 APP 源码版本为 143，编译扩展时会读取当前 manifest，避免主 APP 与扩展版本不一致。

实际操作顺序：在 Apple Developer 中为同一 Team 的 `com.dfyj.xiaoxing.LiveActivity` 准备与主 APP 发布方式一致的扩展描述文件 → 在 Mac/Xcode 运行下方构建脚本 → 将生成的插件 `Plugins` 目录及 `nativeResources/ios` 文件保留在本次打包工程中 → HBuilderX 云打包新 IPA → 安装后登录并检查系统对小醒的“实时活动”开关。服务端无需再给用户分配后台角色权限。

真机验证可使用自己执行且仍在进行中的事项，例如“今天某时某分提醒我测试”，目标时间在未来 60 分钟内；自己创建和别人发给自己的提醒都可以。只有日期、没有明确时刻的事项不进入实时活动。当前自动后台开始实现要求 iOS 17.2 或以上。构建完成后仍需检查 IPA 是否包含 `Payload/*.app/PlugIns/XiaoxingLiveActivityExtension.appex`，并验证锁屏、灵动岛、结束及改期行为；本机 Windows 无法完成 Swift 扩展签名和 iPhone 验收。

## 已确认的产品规则

- 本人仍需执行的 ACTIVE 分支，包含自己创建、好友或群组成员发来的任务。发起人身份不影响入选；发给别人的任务不会出现在自己的卡片中。
- 有明确发生时间或截止时间，提前 60 分钟进入展示窗口；窗口内新收到的任务可直接入选。
- 明确要求立即处理且没有其他时间的任务，以创建时间进入短窗口。只有日期、没有具体时刻的事项不自动进入；不以 AI 下次评估时间、默认填充时刻作为用户时间。
- 到点后保留最多 30 分钟。已读、回复“收到”不会停止；业务分支完成、停止、父事件结束或改到窗口外时结束展示。展示过期不修改任务状态。
- 每台设备默认一张卡片。首次选取时立即处理优先、其次目标时间最近、最后稳定 ID 排序；已有卡片仍符合条件时保留，避免同时段任务来回跳。
- 用户关闭后，本设备同一分支、同一目标时间、同一类型的窗口不重开。改期生成新窗口；服务端重启不会重复开始已记录窗口。
- 暂不提供“主动关注”入口。原有普通提醒通知和本机闹铃的调度保持独立。

## 实现

后端 `LiveActivityPlans` 只查询当前接收人的有效分支，使用分支独立时间（显式清空时不能回退到父事件）。显示只投影 `task_content`，不回退到包含其他接收人信息的总摘要。现有数据未提供可靠的全天事件标记，当前保守要求任务文字或原时间描述有时刻证据；日期只有“今天/10月31日”不纳入。

新增接口：

- `POST /app/push/live/snapshot`：OAuth 与 APP 角色校验，返回当前用户可展示的事项及服务端时间；供 Android 后台服务使用。
- `POST /app/push/live/token`：接收 ActivityKit START/UPDATE/ENABLED/DISMISSED。使用随机 installationSecret 的哈希校验、当前有效 bindingId、用户归属和设备启用状态鉴权；仅这个接口免 OAuth，以便系统后台唤醒时上传令牌。不能读取业务信息，不能提交任意用户的活动，也不能给未由服务端发起的活动附加令牌。

新增 `blade_smart_live_device` / `blade_smart_live_activity` 表，启动时幂等创建；不改旧接口返回或要求旧 APP 上报新字段。ActivityKit 专用令牌加密保存，与普通 APNs 设备令牌分开。工作器约每 15 秒核对任务，发送 `start/update/end`，使用 `.push-type.liveactivity` topic；更新仅在实际内容/到点状态变化时发送。持久化开始记录、设备租约和版本化绑定防止重复开始和跨账号串发；不确定是否已送达的 start 不盲目重发。已结束的窗口保留七天，令牌结束后清空。

## 平台行为

### iOS

新增 `XiaoxingReminderAttributes.swift`、原生桥接、WidgetKit 锁屏与灵动岛界面。自动后台开始使用 iOS 17.2+ 的 push-to-start；旧系统、缺少扩展、系统关闭实时活动时保留已有普通通知。倒计时由系统绘制。标题标为隐私敏感，点击通过 `xiaoxing://live` 打开对应事件，账号必须匹配，事件详情继续接受服务端鉴权。

必须完成的原生发布步骤（资源构建不能替代）：

1. 在原 Apple Team 下创建扩展 App ID `com.dfyj.xiaoxing.LiveActivity`，准备对应的 Provisioning Profile；主 APP 继续使用 `com.dfyj.xiaoxing` 和原推送环境。
2. 在安装 Xcode 的 Mac，运行：

   ```sh
   python3 code/APP/native-ios/LiveActivity/build-extension.py --profile /absolute/path/LiveActivity.mobileprovision
   ```

   脚本先编译实际 arm64 SwiftUI/WidgetKit 扩展，成功后才写入插件 `Plugins/XiaoxingLiveActivityExtension.appex` 和 `nativeResources/ios/ios-extension.json`，以及复制扩展描述文件。扩展版本读取 APP manifest。未编译时不放置一个指向不存在扩展的配置，避免破坏普通云打包。
3. 在 HBuilderX 制作含此扩展的自定义基座或正式 IPA，主 APP 与扩展采用同 Team、匹配环境的签名；版本递增时同步扩展配置版本；可重新执行构建脚本，或在扩展源码未变时复用已验证二进制并更新 ios-extension.json。具体交接清单见 [Mac 编译交接](../code/APP/native-ios/LiveActivity/HANDOFF.md)。
4. Windows 当前只能验证 UTS、Android 编译和 iOS 资源生成；Swift 扩展编译、云签名 IPA、真机后台启动与展示尚未验证。

### Android

复用现有后台接收服务，拉取授权快照，在单独低打扰通知渠道展示动态锁屏卡片、倒计时和事件入口。普通提醒不伪装成符合 Live Updates 条件的 ongoing 活动；这一版不是厂商胶囊/灵动岛适配。手动关闭按设备持久化记录，过期本地清理，服务停止/登录过期/切换账号也清理。系统通知权限、通知渠道、后台服务存活和厂商电源策略仍影响到达；不承诺被强制停止后仍准时展示。

## 验证与验收

- `scripts/LiveActivityRegression.java` 使用真实 SQL、H2 和假 APNs，覆盖一小时前/后边界、明确时刻/日期、截止时间、立即事项、自己/他人任务、授权范围、清空分支时间、重复开始、正文更新、到点、完成、改期、关闭、发送不确定性、迟到令牌和切换账号。
- APP 测试覆盖实时活动链接的账号约束、超长整数 ID 与恶意/畸形路径；Android 生成代码及原生 Kotlin 使用完整编译检查。
- 真机还需验证：锁屏/解锁、灵动岛各尺寸、系统禁用活动、手动划掉、APP 后台/被终止、弱网、语言切换、改期、完成、退出/重新登录。不要用 H5/front 截图作为原生卡片验收。
- 本轮未提交 GitHub、未部署服务端、未发布安装包。没有创建或修改生产提醒。

官方参考：[Apple ActivityKit 推送](https://developer.apple.com/documentation/activitykit/starting-and-updating-live-activities-with-activitykit-push-notifications)、[DCloud iOS Extension 打包](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#iosextension)、[Android Live Updates 条件](https://developer.android.com/develop/ui/views/notifications/live-update)。
# Android 图二样式适配（2026-11-02）

安卓的动态提醒通知现在使用 `XiaoxingLiveCard.kt` 与插件 `app-android/res` 内的原生 RemoteViews。展开状态采用小醒头像、状态、左侧事件标题/时间、右侧倒计时、浅蓝紫渐变背景，无完成或稍后提醒按钮。折叠状态使用 48dp 紧凑布局，保留头像、标题、时间和右侧倒计时，适应系统通知高度限制。整张通知继续跳转对应事件，并保留原有私密内容设置、账号检查和划掉后不再显示的规则。

Android 8+ 使用系统倒计时，倒计时卡片在目标时刻超时；进程存活时本地切换到“已到时间”卡片，随后由服务端快照继续更新。进程被终止时先让旧倒计时退出，避免过零倒数。Android 6/7 显示绝对提醒时间，Android 6 使用紧凑布局。手机厂商仍决定外部通知框架、是否默认折叠及锁屏是否隐藏内容；这仍是普通动态提醒通知，不是 Android 官方 promoted Live Updates。

本次已完成全部插件 Kotlin 文件编译，以及 AAPT2 布局和图片资源编译、链接。尚无连接设备，未验证真机通知显示；需重新云打包安卓 APK 或制作包含新资源的自定义基座，不能只更新页面资源。iOS 扩展及服务端无需为本次安卓样式重新构建。

