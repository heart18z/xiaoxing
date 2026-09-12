# xiaoxing · AI小醒

智能消息提醒系统。包含当前网页前后端和第一版 iOS Capacitor 工程，可在 Mac / Xcode 继续开发。

## 目录

- `code/front`：Vue 3 网页，移动页面为 `/app`；Capacitor 8.5.1。
- `code/front/ios/App/App.xcodeproj`：iOS 工程，Swift Package Manager。
- `code/back`：Java 17 / Spring Boot 后台，包括直接 APNs 推送与设备绑定。
- `scripts`：离线回归测试；不包含生产探测或写库脚本。
- `sql`：提醒模块结构迁移；不含业务数据和完整数据库备份。
- [iOS / TestFlight 操作说明](docs/ios-testflight.md)。

## Mac 开始开发

安装 Node.js 22+ 和兼容 Mac 系统的 Xcode 26+，首次打开 Xcode 完成 iOS 组件安装，并登录组织 Apple 开发者账号。

```bash
git clone https://github.com/heart18z/xiaoxing.git
cd xiaoxing/code/front
cp .npmrc.example .npmrc
# 在本机安全配置 NPM_BLADE_TOKEN；它是 BladeX 私有包授权，不是 GitHub 密码。
npx --yes pnpm@9.3.0 install --frozen-lockfile
npm run ios:sync
npm run ios:open
```

不要从 Windows 拷贝 `node_modules`，也不要跳过 `ios:sync`；同步会按 Mac 环境重新生成 Swift Package 路径。

Bundle ID `com.dfyj.xiaoxing`，Team ID `7U8S8PWU2W`。原生资源打包到 App，API 入口在 `.env.native` 中配置为 `https://www.chentong.xyz`。

首轮代码已通过 Windows 前后端构建、离线回归与模拟浏览器检查；**尚未通过 Xcode 编译、苹果签名或真实手机 APNs 验证**。当前使用默认工程图标；测试上架前再替换正式图标。Production 推送通过 TestFlight 验证；常规 Debug 真机推送需要另配 Sandbox key。

## 配置与安全

这是脱敏源码，不是数据库或服务器完整镜像。Windows 原工作目录和线上运行配置未被本次上传替换。

- npm 私有包凭据：复制 `.npmrc.example`，从授权管理员取得令牌，保存在本机/密钥管理器。不要提交真实 `.npmrc`。
- Maven 私有依赖：按 `code/back/maven-setting.xml.example` 配置本机 Maven，使用组织授权。
- 后台 profile 配置以 `*.yml.example` 提供，运行前复制需要的 profile 并配置数据库、Redis、OAuth 等环境变量；`publication-audit.json` 列出已脱敏字段和变量名。
- 既有生产数据加密依赖原来的 token sign key，迁移时必须安全保留原值，不能随意生成新值覆盖。
- APNs `.p8` 只部署在服务器安全目录，不能放入 Git、前端资源或 App。
- 真实数据由已有服务器持有；在 Mac 编译 iOS 并使用公网 API，不需要复制整库到 Mac。
- 本次提交不等同于部署新版后台。通知设备接口和 APNs 服务还需按交接文档上线配置。

## 授权

项目包含 BladeX 商业组件。保留 `code/back/LICENSE` 与 `code/front/LICENSE` 的原授权声明；仓库应保持私有，仅对许可范围内的人员开放，不作为开源项目分发。
