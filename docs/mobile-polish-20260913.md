# 手机体验修复（2026-09-13）

## 已部署到公网后端

- 通知读取当前接收人自己的站内消息正文，不读取全局事件描述或其他接收人的任务。正文做简短预览，最长 400 个 Unicode 字符；点击仍进入原事件。iOS 系统预览设置仍由用户控制。
- “现在再提醒我”被 AI 转成已经过去几秒的 nextEvaluateTime 时，排入立即评估队列，保留原计划作为兜底；不绕过 AI 强行发提醒。
- 事件概述回退文本及已缓存概述中，日期时间的 T 改为空格，仅处理显示，不改存储时间或时区。

定位证据：事件 SR202609131444181104129 的 15:57:18 更新动作，nextEvaluateTime 为 15:57:00，其他修改字段为空。原逻辑仅接受未来时间，导致 evaluation_requested_at 仍为空、下次计划仍为 16:15。没有重放这条旧指令或伪造真实评估记录。

公网版本：`aimessage-backend:apns-polish-20260913-1620`。
JAR SHA-256：`b17f7e7a9ff8d48b5076d1bee236d4038f2d71cb577dbf5e3ddecebd5e5dd8ef`。
备份：`/opt/aimessage/backups/pre-apns-polish-20260913-1620`。
发布：`/opt/aimessage/releases/apns-polish-20260913-1620`。
数据库结构完全未变，MySQL/Redis 未重启，原设备绑定和 APNs 私钥挂载保留。只替换后端容器，未覆盖业务库。后端健康、重启次数 0。

## 需要新 TestFlight 构建的界面修改

- 思考输出气泡使用正常长回复相同的可用宽度，移除导致收缩的 inline-size containment。
- 原生 UIWindow、WKWebView 及网页根背景使用相同浅色，避免键盘调整视口时露出黑色承载背景。无法改变系统输入法本身的圆角/布局，须 iPhone 实测。
- 合并之前已在 Mac 手工修好的 XiaoxingViewController 启动入口和输入框 16px 防缩放规则。
- 事件详情的概述、时间和分支任务统一显示日期时间空格。
- 手机通知设置说明更新为显示正文，并提示 iOS 锁屏预览控制。

Mac 更新前先保存/备份未提交更改，尤其 SceneDelegate.swift 和 AppDelegate.swift。不要覆盖 Mac 上的签名配置、Info.plist、AppIcon 或 project.pbxproj；本次更新包不包含这些文件。
在 Mac 的 code/front 目录运行 `npm run ios:sync`、`npm run ios:open`。Xcode 中把 Build 增加到未使用的值（若最新是 1，则用 2），Version 可保持 1.0，Archive 上传。手机使用 TestFlight 更新，不必卸载。

## 验证

- Maven 生产打包成功；因原项目无关测试模板存在语法错误，使用 `-Dmaven.test.skip=true`，不能声称完整 Maven 测试通过。
- 独立 H2 回归：CreatorEvaluationRegression（含立即评估、保留计划、指定接收人）、PushRegression（含正文、空值回退、Unicode 截断）、TaskChangeDeliveryRegression、EventPrivacyRegression 通过，包含其 Stage1 基础回归。
- Native JS 和显示格式共 6 项测试通过。
- 使用 Playwright CLI 的真实浏览器、离线模拟 API/原生桥，检查 390px 和 320px 下思考/正常回复宽度一致、展开内容无横向溢出、输入框 16px、浅色根背景；日期 T 显示回归通过。截图位于 output/playwright/mobile-polish-*.png。
- 原生网页生产构建成功；公网 CORS/鉴权/网页 18 项检查通过。
- Windows 未编译 iOS SDK；键盘边角和新包界面必须在 Mac/iPhone 最终验收。修复后的真实新通知正文与立即评估仍需用户操作验收，未用旧 APNs 成功记录冒充新版本端到端结果。

验收建议：在事件对话明确说“现在再提醒我一次”，检查立即评估节点与决定；退出 APP 页面/锁屏后查看新通知正文；新 TestFlight 包中展开思考输出并聚焦输入框检查边角。
