# 实时活动扩展：Mac 编译与 Windows 云打包交接

当前主 APP 版本见 `code/APP/manifest.json`（本次 1.0.145 / 145）。扩展构建脚本读取该文件。

144 已打包。本次 145 修复实时活动 logo：新增编译后的 Asset Catalog，并保留 PNG 加载回退。Mac 请拉取最新 main 后重新构建，确认日志、扩展 Info.plist 和 ios-extension.json 均为 1.0.145 / 145。不能复用旧 144 扩展，也不要直接修改已签名扩展。Windows 当前保留的已编译扩展仍须由此次构建产物替换。

## Mac 同事操作

1. 拉取 `heart18z/xiaoxing` 的最新 `main`。安装完整 Xcode 和 Python 3，完成 Xcode 首次初始化，确认 `xcode-select -p` 指向完整 Xcode 的 Developer 目录。
2. 在原 Apple Developer Team 下为 `com.dfyj.xiaoxing.LiveActivity` 准备扩展专用描述文件。它的 Team、分发方式和允许签名证书须与 Windows 云打包主 APP 所用材料匹配；不能拿主 APP 的描述文件代替。
3. 在仓库根目录执行：

   ```sh
   python3 code/APP/native-ios/LiveActivity/build-extension.py \
     --profile "/实际路径/LiveActivity.mobileprovision"
   ```

4. 成功后，把下面三个产物按原相对目录结构压缩成 ZIP，私下交付给打包同事：

   ```text
   code/APP/
   ├── uni_modules/xiaoxing-native/utssdk/app-ios/Plugins/
   │   └── XiaoxingLiveActivityExtension.appex/    # 整个目录及内部所有文件
   └── nativeResources/ios/
       ├── ios-extension.json
       └── xiaoxing-live.mobileprovision
   ```

   `.appex` 在 Finder 看似单个文件，实际是 bundle 目录；务必完整压缩，包含可执行文件、Info.plist、Assets.car、live-activity-avatar.png 和签名目录等，不能只复制 Swift 源码或内部可执行文件。
5. 附上构建日志、所用 Git 提交号、Xcode 版本，以及主 APP/扩展版本号。若编译失败，请交付原始报错，不要手工造一个空扩展目录。此前接收的是 144 编译产物；本次 145 仍需新产物、云签名 IPA 和真机 logo 验证。

## Windows 接收与云打包

1. 先拉取同一提交的代码，再把 ZIP 内三个产物放到上面的对应位置。HBuilderX 打开 `code/APP` 工程，核对 manifest 与扩展 Info.plist、ios-extension.json 的版本一致。
2. 继续使用原主 APP 的签名证书、密码和主 APP 描述文件云打包；扩展描述文件由 ios-extension.json 引用。HBuilderX 至少需要支持 iOS Extension 的 4.61 版本，建议使用项目已验证的版本。
3. 解开最终 IPA，核对 `Payload/*.app/PlugIns/XiaoxingLiveActivityExtension.appex/` 存在。构建脚本的本地 ad-hoc 签名不等于正式发布签名，仍需云打包完成签名。
4. 安装到 iOS 17.2+ 真机，登录并开启系统实时活动，使用未来 60 分钟内有明确时刻、本人为接收人且进行中的任务验证开始、更新、改期、结束。服务器 APNs 配置、签名环境和系统开关仍需匹配。

## 后续版本

- 普通 APP 页面或服务端规则改变、扩展源码及 ActivityAttributes 数据结构未变时，通常可复用已验证的扩展二进制。
- 主 APP 版本递增时，同步 `ios-extension.json` 中的 `plists.CFBundleVersion` 与 `plists.CFBundleShortVersionString`，云打包后检查 IPA 内最终版本。当前修改 manifest 不会自动更新此 JSON；重新运行构建脚本也可以完成版本同步。
- 修改扩展界面、Swift 逻辑、共享 ActivityAttributes，或需要适配新的 SDK 发布要求时，在 Mac 重新构建。
- 描述文件/证书到期或分发方式变化时更新对应签名材料并重新云打包。
- 三个生成产物均不进入 Git；只拉取仓库不会得到它们，务必另行留存。已有主 APP 签名材料无需让同事重复发送，私钥和密码不要提交仓库。

## 图二紧凑样式：需要再次构建扩展

锁屏卡片改为小醒头像与状态栏、左侧事件内容、右侧倒计时的紧凑布局，无底部操作按钮。浅蓝紫渐变、细边框与分隔线在 SwiftUI 中绘制，点击整张卡片继续打开对应事件。构建脚本新增图片资源阶段，从 `code/APP/static/avatars/assistant/A3.png` 打包头像；交接源码时需包含该文件。

这次修改了 Swift 界面和原生资源，已接收的 `e40bb18` 扩展二进制仍是旧界面。必须重新执行上面的 Mac 构建命令，再替换完整 `.appex`、云打包配置和描述文件。Windows 只能验证构建脚本及资源配置，Swift 编译、实际锁屏显示和苹果上传需在 Mac/真机及新 IPA 中验证。提醒数据结构保持不变，副标题只显示日期、时刻与提醒类型，避免将他人发起的事项误标为自己发起。

参考：[DCloud iOS Extension 云打包](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#iosextension)。

## 2026-11-02：修复上传校验 90502

arm64 扩展必须在 Info.plist 的 UIRequiredDeviceCapabilities 中声明 arm64。构建脚本现已在签名前写入该数组，并同步到生成的 ios-extension.json.plists。已生成的同版本扩展可以通过云打包配置合并此字段，无需仅为该字段重新编译 Swift。务必重新云打包完成签名，再检查最终 IPA 内扩展 Info.plist；不要直接修改已签名 IPA。该修改通过本地配置合并检查，但最终上传仍待重新打包验证。

## 2026-11-02：90898 必须重新编译

后续上传确认旧扩展的 Mach-O LC_MAIN 指向普通 `_main`，缺少扩展入口。之前的签名完整性验证不覆盖此发布要求，旧产物不能继续发布。现构建脚本改为 Xcode app-extension 目标、显式 `_NSExtensionMain` 链接入口，并在交付前运行 `verify-extension.py`。必须在 Mac 重建后替换完整扩展，不能仅重新云打包旧二进制。当前仅完成 Windows 语法、工程生成及旧包拒绝检查；新脚本的 Mac 构建、云打包、苹果校验和真机测试仍待完成。
