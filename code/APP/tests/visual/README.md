# 页面与交互检查

这些脚本通过 Playwright CLI 运行，在浏览器中拦截当前 API 地址。只在独立测试浏览器会话使用，必须先执行 `replica-fixture.js`，再执行其他脚本。不要在真实登录会话中单独执行 `verify-actions.js`。

先由 HBuilderX 启动 Web 预览。下面使用本机预览端口 5174；端口不同需修改脚本中的地址。在 APP 目录运行：

```powershell
npx --yes --package @playwright/cli playwright-cli -s=visual open http://localhost:5174
npx --yes --package @playwright/cli playwright-cli -s=visual run-code --filename tests/visual/replica-fixture.js
npx --yes --package @playwright/cli playwright-cli -s=visual run-code --filename tests/visual/verify-actions.js
npx --yes --package @playwright/cli playwright-cli -s=visual run-code --filename tests/visual/replica-states.js
# 截图流程最后退出模拟登录，所以先重新装载 fixture。
npx --yes --package @playwright/cli playwright-cli -s=visual run-code --filename tests/visual/replica-fixture.js
npx --yes --package @playwright/cli playwright-cli -s=visual run-code --filename tests/visual/responsive.js
```

- `replica-fixture.js`：本地测试账号与业务响应，不会把截图中的示例内容写入产品代码。
- `replica-states.js`：17 张对应截图，以及模型下拉所有选项未被裁切/遮挡的检查。
- `verify-actions.js`：资料必填校验、资料/备注/权限/模型提交参数、清除上下文的取消和确认。
- `responsive.js`：320 / 402 / 768px 下的五个登录后页面，无横向溢出或脚本异常。
- `chat-depth.js`：长历史首次/切回定位最新、回到最新按钮、四阶段等待动画、终态失败草稿恢复与顶部提示、提醒分组卡片。先载入 fixture，等待构建完成再运行，避免构建期间静态资源被清空。
- `login-chat-refinement.js`：内联思考展开/收起、六类消息与六类候选状态、忘记密码弹窗，以及重复登录取消/确认请求头。先运行 fixture，再执行 `playwright-cli -s=visual run-code --filename tests/visual/login-chat-refinement.js`。截图为 `output/playwright/refinement-*.png`。

截图写到忽略提交的 `output/playwright/replica-*.png`。截图脚本在 Web 中补入 62px/34px 安全区作同尺寸对照；运行代码使用系统安全区，不包含模拟状态栏。

- `contracts.js`：先加载 replica-fixture，核对后端 summary、头像 nickname、好友/申请头像、回车与换行；接口均拦截，不写真实用户数据。
