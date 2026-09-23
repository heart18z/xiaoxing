# APP 129：根据真实 iOS 崩溃报告修正绘制风险

## 已确认的证据

报告：`UNI93FE60B-2026-09-23-201405.ips`。

- App：`com.dfyj.xiaoxing`，版本 `1.0.1 (125)`，TestFlight 安装。
- 系统：iOS 26.6 (23G71)，机型标识 `iPhone18,3`。
- 进程启动：19:06:49；退出：20:14:05。约运行 67 分钟，报告本身没有记录完整的前后台切换历史。
- 主线程，`EXC_CRASH / SIGABRT`，`SIGNAL 6 / Abort trap: 6`。
- 主线程堆栈包含 `NSAssertionHandler` → `_UIGraphicsBeginImageContextWithOptions` → `DCUniComponent(Display) _displayBlock` → `_willDisplayLayer:` → `UniLayer display` → CoreAnimation 提交事务。
- `DCUniComponent` 属于 `DCloudUniappRuntime.framework`。没有自定义通知、语音、闹铃或流式 Swift 方法位于异常发生链中。
- `vmSummary` 记录 `CG raster data 2.0G / 660 regions`。这是虚拟内存区域统计，不能将它当作实际常驻内存，也不能单凭此认定系统 OOM。

结论：这份报告确认的是 **uni-app 原生组件绘制时创建位图上下文触发断言，未捕获的原生异常终止 App**。不是 watchdog 的终止签名，也不是 JetsamEvent 报告。128 版的通知并发与网络恢复修复属于独立防护，不能宣称已解决这条堆栈。

缺失信息：报告没有断言描述、图层尺寸或具体组件标识。因此零尺寸、异常尺寸和内存分配失败仍是待验证的分支，不能把其中一种写成已证实根因。

## 针对绘制开销的缓解

- 折叠的思考面板原先仅把外壳高度设为 0，内部长文本及原生滚动视图仍存在。现在折叠或离开页面时卸载内部视图，展开时从保留的数据恢复；不删除历史内容。
- 思考扫光仅在页面可见时挂载，明确高度为 20px，减少依赖临时父容器尺寸的绘制。
- iOS 长消息气泡改为统一圆角并移除阴影；用户气泡保留蓝色背景，去掉该气泡的运行时渐变。Android/Web 原有气泡样式保持。
- 键盘打开时卸载底部 Tab 栏，不再将带圆角及子渐变的栏压成 0 高度。

这些是业务层降低原生绘制压力与零尺寸绘制风险的缓解措施，不是对 DCloud 二进制内部位图接口的修补。没有通过私有方法替换或吞掉 Objective-C 异常掩盖问题。

## 验证及后续

- 新增组件渲染测试：包含 45,000 字的历史思考，折叠/隐藏时不存在原生文本和 scroll-view，重新展开后保留完整内容。
- 原有持续流式与隐藏后返回测试继续覆盖文字可见性。
- 全部 78 项 Node 回归测试、Vue/UTS 语法检查、双端资源构建、Android 生成 Kotlin 检查通过；iOS 保持流式回调检查通过。iOS 生成样式另核对圆角和渐变，避免条件编译遗漏。
- 版本 `1.0.129 (129)` 包含 127、128 的修改。需重新云打包，并在 iPhone 上进行长聊天、键盘切换、后台放置后返回的复测；本机资源构建不能代替真机验证。
- 若新包仍出现相同原生绘制堆栈，应附对应构建日志、dSYM 和最小复现交由 DCloud 排查位图尺寸/创建失败的处理。不能仅根据通用版本更新记录承诺 SDK 升级一定解决。

Apple 已弃用该位图上下文 API，建议使用 `UIGraphicsImageRenderer`，但这次调用位于 DCloud 运行库，应用的 Swift 插件不能直接改动该内部调用：[Apple API 文档](https://developer.apple.com/documentation/uikit/uigraphicsbeginimagecontextwithoptions(_:_:_:))。
