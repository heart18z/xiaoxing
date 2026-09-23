# APP 128：iOS 后台恢复闪退排查

后续已取得 125 版真实报告，确认这次异常在 DCloud 原生绘制层。详见 [129 版报告分析](mobile129-ios-render-crash.md)。下文保留为取得报告前的代码审计记录，不能作为本次崩溃的最终归因。

用户反馈：App 在后台放置一段时间后重新打开，偶发退出。未取得对应 `.ips` 崩溃报告，也未在 iPhone 上复现，因此本次改动是已发现问题的防护修复，不能宣称确定了闪退根因。

## 已发现并修正

1. `XiaoxingBridge.swift` 的通知代理回调没有统一线程，直接修改 `tapped` 字典，而 `push.tap` 在主队列读取并清空它。现在通知回调、代理转发与状态读写均进入主队列，避免并发读写 Swift 集合。
2. 聊天页隐藏时虽然清理计时器，但尚未完成的网络请求和响应式监听仍能重新安排原生 DOM 测量/滚动。现在调度入口、延迟回调、nextTick 回调和键盘回调均检查页面及 App 可见状态；隐藏期间不操作原生页面元素，恢复后由 onShow 重新布局。
3. HTTP success 回调里的 JSON 解析原来没有异常保护。断网恢复、代理或网关返回 HTML、空值、错误 JSON 时，可能抛出未处理异常并使 Promise 一直不结束。现在统一返回可处理的 ApiError，保留有效登录，允许重试；HTTP 状态仍保留。
4. 自动恢复会话与进入聊天页的 bootstrap 调用补齐异步错误处理。

不改变账号鉴权规则、已接受的聊天任务、系统通知权限或 iOS 闹铃授权。

## 验证与边界

- Node 回归测试 77/77 通过，新增异常网络响应与隐藏页面延迟布局的行为测试。
- 使用 HBuilderX 5.24 进行双端资源构建，并检查 Android 生成 Kotlin 和 iOS 持久流回调。
- Windows 本机不能完成 UIKit/Swift 原生链接或 iPhone 闪退复现；资源构建不能代替签名云打包及真机测试。
- 版本为 `1.0.128` / `128`，包含 127 的响铃入口和切页思考优化。

## 需要的设备证据

在 iPhone「设置 → 隐私与安全性 → 分析与改进 → 分析数据」中找到闪退时间附近的小醒进程报告（可能以应用可执行文件名或 `__UNI__93FE60B` 命名），分享完整 `.ips`，同时提供机型、系统版本、App 版本及发生时间。如果只有 `JetsamEvent`，提供同一时间的那份报告。

先根据 Exception Type、Termination Reason 与崩溃线程判断属于原生异常、内存压力退出还是 watchdog 超时。涉及代码地址时，需匹配该构建的 dSYM/云打包符号才能定位准确行号。

参考：[Apple 崩溃与设备日志诊断](https://developer.apple.com/documentation/xcode/diagnosing-issues-using-crash-reports-and-device-logs)、[获取崩溃报告](https://developer.apple.com/documentation/xcode/acquiring-crash-reports-and-diagnostic-logs)。
