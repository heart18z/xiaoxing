# xiaoxing · AI小醒

智能消息提醒系统，包含 uni-app x 手机 APP、网页管理端及 Java 后端。当前手机端开发入口为 `code/APP`。

## 目录

- `code/APP`：当前 uni-app x / UTS 工程，包含 iOS / Android 原生插件。
- `code/back`：Java 17 / Spring Boot 后端，包括提醒、好友二维码、图片解析、聊天任务及 APNs。
- `code/front`：Vue 3 网页与旧版 `/app` 页面，保留作为现有网页和迁移参考。
- `code/front/ios`：旧 Capacitor Xcode 工程；新版 APP 需使用 uni-app x 原生宿主。
- `scripts`：离线回归测试。
- `sql`：结构迁移，不包含业务数据与数据库备份。

## Mac 开发与 iOS 测试

```bash
git clone https://github.com/heart18z/xiaoxing.git
cd xiaoxing/code/APP
npm ci
npm run check
npm test
```

在 HBuilderX 导入 `code/APP`，保持当前 VDOM 模式。按照 [新版 APP 的 Xcode / TestFlight 操作说明](code/APP/IOS-TESTFLIGHT.md) 导出 iOS 资源、集成对应版本原生 SDK 与 UTS 插件，再使用原 Apple Team 自动签名、真机 Run、Archive 并上传 TestFlight。

Bundle ID 保持 `com.dfyj.xiaoxing`。当前准备版本为 `1.0.1 (100)`，上传前核对 App Store Connect 构建号未被占用。原生 API 为 `https://47.100.172.149/api`。

APP 语法检查和 39 项测试通过，Web / Android 编译已验证；**新 Xcode 宿主尚需集成，iOS 编译、签名及真机验收尚未完成，也未上传新版 TestFlight**。生产后端已更新至 `ios-readiness-20260920`。

更多内容见 [APP 开发说明](code/APP/DEVELOPMENT.md)、[接口约定](code/APP/README.md)。旧版 Capacitor 流程仅见 [历史 iOS 文档](docs/ios-testflight.md)，不要将旧流程当成新版 APP 打包入口。

## 配置与安全

这是脱敏源码，不是数据库或服务器完整镜像。Windows 原工作目录和线上运行配置未被本次上传替换。

- npm 私有包凭据：复制 `.npmrc.example`，从授权管理员取得令牌，保存在本机/密钥管理器。不要提交真实 `.npmrc`。
- Maven 私有依赖：按 `code/back/maven-setting.xml.example` 配置本机 Maven，使用组织授权。
- 后台 profile 配置以 `*.yml.example` 提供，运行前复制需要的 profile 并配置数据库、Redis、OAuth 等环境变量；`publication-audit.json` 列出已脱敏字段和变量名。
- 既有生产数据加密依赖原来的 token sign key，迁移时必须安全保留原值，不能随意生成新值覆盖。
- APNs `.p8` 只部署在服务器安全目录，不能放入 Git、前端资源或 App。
- 真实数据由已有服务器持有；在 Mac 编译 iOS 并使用公网 API，不需要复制整库到 Mac。
- 源码提交与服务部署分别管理；本轮生产后端发布状态见新版 APP 的 iOS 操作说明。

## 授权

项目包含 BladeX 商业组件。保留 `code/back/LICENSE` 与 `code/front/LICENSE` 的原授权声明；仓库应保持私有，仅对许可范围内的人员开放，不作为开源项目分发。
