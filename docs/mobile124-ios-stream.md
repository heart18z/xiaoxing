# iOS 思考流式接收回归修复（APP 124）

## 原因及证据

此前新增的 iOS `XiaoxingStream` 将所有 SSE 帧通过普通 `nativeCall` 回调发送。HBuilderX 5.24 编译出的这个代理为 `keepAlive: false`，而服务端首先发送 `ready` 帧，导致回调在第一帧后即被回收。后续思考增量无法抵达页面，客户端直到观察超时或连接结束后才查询最终结果。

DCloud [UTS 回调生命周期说明](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#keepalive)说明，4.25 起 iOS 导出方法的回调默认只触发一次；持续回调需显式适配。Android uni-app x 不在该默认回收规则的影响范围内，因而表现不同。

实际编译中，仅将监听命名为 `onStreamEvent(callback: NativeResult)` 仍产生 `keepAlive: false`。最终使用 `@UTSJS.keepAlive`，并直接检查编译产物，不能只依赖 JavaScript 模拟或方法命名判断。

## 修复

- iOS 使用单独的、显式持续保留的 `onStreamEvent` 监听，每个应用运行周期只注册一次。`stream.watch` 的普通回调只确认启动，增量通过该监听交付。
- 每次观察/重连分配不同连接 ID；关闭旧连接后，其延迟事件不会混入新连接或新账号的对话。取消时释放当前接收闭包。
- 原生观察 4 秒没有新数据时，每 2 秒尝试只读查询已有任务状态，恢复尚在生成的思考。已有新 SSE 数据时不使用较早的查询覆盖；结束/暂停时停止恢复定时器。该机制不重新提交消息或重复创建事件。
- 保留真实思考数据和现有正文提交后显示流程，不修改正文动画速度，不使用虚构思考内容掩盖接收故障。
- APP versionCode 为 124；本次无服务端、模型配置或数据库变更。

## 验证方式

- `node tests/check-project.mjs`：Vue/UTS 通用语法检查（UTS 专用注解由原生编译验证）。
- `node --test tests/*.test.mjs`：70 项测试；新增模拟 iOS 单次启动回调、连续思考、连接重建、旧数据丢弃，以及静默期间恢复正在生成的思考。
- HBuilderX 5.24 Android 资源构建 + `tests/check-android-generated.ps1`：生成 Kotlin 与原生实现类型检查。曾发现局部递归箭头函数的 Kotlin 生成问题，已改为命名函数。
- HBuilderX 5.24 iOS 资源构建 + `node tests/check-ios-generated.mjs`：直接断言 `onStreamEventByJs` 为 `keepAlive: true`，普通 `nativeCallByJs` 仍为 `false`。

Windows 资源构建不等于 Xcode 云打包或 iPhone 真机验证。本次没有生成签名 IPA，也没有声称已在手机验证。需重新云打包并通过 TestFlight 安装 124，旧安装包不会获得原生桥接修复。手机验证重点：首条连接消息后仍持续出现思考；长回复开始前能展开查看；切后台再恢复不重复创建事件。
