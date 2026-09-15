# 防止页面双击误放大

事件列表此前只有底部 Tab 设置了 `touch-action: manipulation`。现在移动页面壳及其所有后代（包含嵌套滚动容器）均设置该属性；原生包额外覆盖整个文档，包含挂到 body 下的弹窗与登录页。

保留拖动滚动、双指缩放和文本选择。不添加全局 touchend preventDefault，不锁定 viewport 缩放比例。保留原生输入框最小 16px 的防聚焦自动放大规则。

行为依据：<https://webkit.org/blog/5610/more-responsive-tapping-on-ios/>。

验证：

- 17 项 Node 回归测试通过，包含新增 `code/front/scripts/mobile-touch.test.mjs`。
- `npm run build:native` 成功，日志 `output/mobile-touch-native.log`。
- Playwright CLI 运行 `scripts/mobile-touch-browser.js`，离线 API 与原生桥模拟；检查事件列表、搜索输入和弹窗的计算样式，验证滚动、筛选、清空和打开弹窗，未发现页面错误。
- 截图 `output/playwright/mobile-touch-events.png` 已检查。

桌面浏览器双击不等同于 iOS 原生双击手势，尚需 iPhone 真机验收：在标题、卡片空白和搜索区域快速双击，确认页面不误放大；检查正常滑动、输入、弹窗与双指缩放。修改不会主动重置用户已经放大的旧页面。

本次未部署、未提交 Git。原生包使用本地前端资源，需重新构建、同步 iOS 资源并打包 TestFlight 后验收；无后端及数据库变更。
