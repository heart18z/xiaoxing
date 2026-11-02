# 实时活动扩展：Mac 编译与 Windows 云打包交接

当前主 APP 版本见 `code/APP/manifest.json`（本次 1.0.143 / 143）。扩展构建脚本读取该文件。

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

   `.appex` 在 Finder 看似单个文件，实际是 bundle 目录；务必完整压缩，包含可执行文件、Info.plist 和签名目录等，不能只复制 Swift 源码或内部可执行文件。
5. 附上构建日志、所用 Git 提交号、Xcode 版本，以及主 APP/扩展版本号。若编译失败，请交付原始报错，不要手工造一个空扩展目录。本脚本尚待首次 Mac 编译、云签名及真机验证。

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

参考：[DCloud iOS Extension 云打包](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html#iosextension)。
