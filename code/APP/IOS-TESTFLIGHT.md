# APP 工程首次迁移到 TestFlight

核对日期：2026-09-20。此文档为操作步骤，不表示已生成 IPA 或上传 TestFlight。

## 已核对的本地配置

- 当前工程：code/APP，uni-app x / UTS。截图中旧工程是 code/front 的 Capacitor iOS 工程。
- Apple Bundle ID：com.dfyj.xiaoxing，与旧工程相同。
- 旧工程本地 Apple Team：7U8S8PWU2W；签名时以实际 App Store Connect 应用所属团队为准。
- DCloud AppID：__UNI__93FE60B。它与 Apple Bundle ID 是两套标识，不能互相替换。
- manifest.json 当前 versionName=1.0.1、versionCode=100（本轮准备值，上传前核对100未被占用）。
- 用户截图旧版本 Version=1.0、Build=6；App Store Connect 实际最新版本/构建号尚未登录核对。
- config/environment.uts 原生接口：https://47.100.172.149/api。用户已选择正式服务器。
- 插件最低系统配置 iOS 15.0。AlarmKit 功能另需 iOS 26+，完整构建需兼容当前 Apple 上传要求的 SDK。
- 直接 APNs 配置为 production，UTS.entitlements 中 aps-environment 同为 production。
- 已有相机、相册、麦克风、语音识别、AlarmKit 用途描述及原生插件；仍需在正式 IPA 中验证合并结果。
- 桌面图标统一使用 static/avatars/assistant/A3.png 的小狗图案。iOS 使用 static/app-icon.png（1024×1024、RGB 无透明通道）；安卓使用 static/app-icons/ 的 72/96/144/192 尺寸；鸿蒙已配置同图前景及蓝色背景。当前原图为 139×139，打包图标按尺寸缩放生成，未增加原图细节。

## 先完成正式后端核对

近期后端二维码接口、本人匹配、聊天卡片等修复先部署在内网。
不能用原生客户端指向正式服务器来替代后端发布。
需通过 SSH 核对 /opt/aimessage 下当前运行镜像、数据库结构及发布配置；比对内网已验证 JAR 后做备份、按需迁移、仅切换后端并执行健康和兼容性验证。
不要复制内网数据库、用户、模型密钥，或把内网 external push=false 配置覆盖到正式服务。
2026-09-20 已使用用户提供的连接方式完成服务器核对及数据库/运行配置备份；生产发布状态见文末记录。

## 当前采用：Xcode 本地打包与上传

用户继续使用原来的 Xcode 工作流。新的 APP 是 uni-app x 原生工程，必须先准备对应的 iOS 原生宿主；仓库里保留的 `code/front/ios` 是旧 Capacitor 工程，不能直接用于新版 APP。

1. Mac 安装 HBuilderX，与已验证的 5.24 VDOM 编译版本及对应 iOS 原生 SDK 保持一致；使用满足 Apple 上传要求、包含 iOS 26 SDK 的 Xcode。
2. 在 HBuilderX 导入 `code/APP`，执行 `npm ci` 安装依赖，核对 `manifest.json` 与 `config/environment.uts`。
3. 选择“发行 → 原生 App 本地打包 → 生成本地打包 App 资源”，导出 iOS 资源。
4. 按 DCloud iOS 原生 SDK 文档建立宿主 Xcode 工程，集成生成的资源、所需模块和 `uni_modules/xiaoxing-native` 插件。核对插件的 Info.plist、UTS.entitlements、资源和原生代码已合并；不能仅替换旧工程的 public 文件夹。
5. 在新宿主的 Signing & Capabilities 选择原 Apple Team、Bundle ID `com.dfyj.xiaoxing`，使用 Xcode 自动签名。版本与本项目保持一致，上传前核对构建号 100 可用；核对推送与 AlarmKit 能力以及实际签名 entitlement。
6. 先连接 iPhone 用 Run 编译验证。开发签名测试需匹配 development APNs 环境；TestFlight 发行恢复 production，并检查最终签名产物。
7. 真机通过后选择发行设备目标，执行 Product → Archive，在 Organizer 中选择 Distribute App → App Store Connect → Upload。处理完成后，在原应用 TestFlight 分配构建给测试组。

当前仓库提供 APP 源码及原生插件，**尚未提供已集成并验证的新 Xcode 宿主工程，也没有生成签名 IPA**。自动签名路线无需为云打包另行导出 .p12。

## 可选：HBuilderX 云打包 + Transporter

1. 在 HBuilderX 导入 code/APP 工程；Mac 上操作时先同步最新源码并安装 package-lock.json 对应的 npm 依赖。保持当前项目的 VDOM 模式，不在此轮顺带切换渲染模式。
2. 登录有权访问上述 DCloud AppID 的账号，打开 manifest.json。若该 DCloud AppID 不属于当前账号，先在账号下关联/申请可用 DCloud AppID；Apple Bundle ID 仍保持 com.dfyj.xiaoxing。核对名称、图标、iOS Bundle ID、支持设备、权限描述。沿用旧应用已支持的设备范围。
3. 在 App Store Connect 核对已有版本和最高构建号，再设置版本。若当前仍是截图中的 1.0 (6)，可使用 1.0.1 (100)；100 必须未上传过且满足递增要求。版本在 manifest 中配置，不在旧 Xcode 工程中修改。
4. 准备同一 Apple Team 的 Apple Distribution 签名证书（含私钥的 .p12 和其导出密码），以及与 com.dfyj.xiaoxing 及该证书匹配的 App Store Connect 分发描述文件（.mobileprovision）。若之前 Xcode 自动签名，可能需要先从 Mac 钥匙串导出证书及私钥，并在开发者后台生成匹配的分发描述文件。证书及密码不要提交到仓库。
5. HBuilderX 选择“发行 → App-Android/iOS-云打包”，选择 iOS，填写包名、证书、密码和 Profile。生成正式发行包，不勾选自定义调试基座。保留本工程 uni_modules/xiaoxing-native；不要复制旧 Capacitor public 文件夹作为新包内容。
6. 编译环境需满足 Apple 当期要求，目前 iOS 上传构建要求 Xcode 26+。检查云打包日志与实际 SDK；如果插件编译报错，先解决完整 iOS 编译问题，Android 成功不能代替此检查。
7. 下载生成的 .ipa。在 Mac 安装并登录 Apple Transporter，添加 IPA，验证并上传。苹果处理完成后，在原有应用的 TestFlight 中选择新构建，补充实际适用的出口合规资料和测试说明，分配内部测试组。
8. iPhone 打开 TestFlight 更新原应用。新旧包名和团队相同，验证覆盖安装；本项目迁移逻辑要求重新登录，不以登录态自动继承作为通过标准。

## 真机首轮验证

- 启动、登录、三个 Tab、刘海/灵动岛及键盘布局。
- 发起人和接收人的事件对话弹窗关闭；顶部提示显示与消失后均不遮挡操作。
- 图片上传并结合内容创建提醒；扫码查好友。
- 创建、确认、修改、反馈、后台恢复，确认正式接口正常。
- 相机/相册/麦克风权限的首次授权和拒绝分支。
- APNs 授权、后台收到通知、点击进入事件、退出切换账号。
- iOS 26+ AlarmKit 授权、设置/修改/取消；系统闹铃与普通站内提醒分别检查。

## 官方操作参考

- DCloud 云打包：https://doc.dcloud.net.cn/uni-app-x/tutorial/app-package.html
- DCloud iOS 原生 SDK：https://doc.dcloud.net.cn/uni-app-x/native/use/ios.html
- Apple 上传构建与 Transporter：https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds

## 2026-09-20 本轮准备结果

- 正式后端已发布 aimessage-backend:ios-readiness-20260920，健康检查通过。
- HTTPS IP和域名正式API均验证通过；数据库结构、模型配置、运行环境、原网页路径保持一致。
- 备份：/opt/aimessage/backups/pre-ios-readiness-20260920。首次切换因环境变量数组排序校验误报自动回滚；改为键值映射严格比较后重新发布成功，没有改动变量值。
- JAR SHA256：0db07039e521c1e843d78097de8800f3910ab57a3e62f5ec70618301bcac4f34。
- APP versionName=1.0.1、versionCode=100；发行图标已复用旧应用；项目语法检查通过。
- 没有生成签名 IPA 或上传 TestFlight；下一步在 Mac 集成新版 Xcode 宿主，用原 Apple 开发者账号签名并真机验证。
