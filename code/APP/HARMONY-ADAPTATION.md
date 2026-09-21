# 鸿蒙适配现状与实施顺序

当前项目已配置鸿蒙桌面图标，但尚未完成鸿蒙功能适配或生成经过验证的鸿蒙安装包。

## 代码缺口

| 范围 | 当前实现 | 鸿蒙需要的工作 |
| --- | --- | --- |
| 页面与业务 | 共用 uvue / UTS，登录、好友、事件、聊天接口已存在 | 编译到鸿蒙并回归布局、键盘、滚动、流式聊天及网络行为 |
| 登录基础能力 | `xiaoxing-native/utssdk` 只有 iOS、Android、Web 实现 | 添加 `app-harmony`，实现安全持久化、密码加密使用的安全随机数及原生调用接口；不能用普通明文缓存代替安全存储 |
| 语音 | 聊天页面分别实现 APP-IOS、APP-ANDROID 和 WEB 分支 | 添加鸿蒙录音/识别、权限、取消和资源清理分支 |
| 文件、相册、扫码 | 文件依赖自定义原生插件；图片和扫码使用 uni API | 实现鸿蒙文件选择，核对媒体和扫码模块、权限、取消返回与上传 |
| 系统推送 | 自定义推送和后端投递目前为 APNs；Android 插件未实现同等系统推送 | 接入鸿蒙推送通道，扩展后端设备注册、投递和通知点击跳转；不能直接复用 APNs 令牌 |
| 本机闹铃 | iOS AlarmKit | 独立评估鸿蒙提醒 API、权限和后台限制，实现可支持的行为并明确展示能力状态 |
| 发布 | app-harmony 目前仅配置图标 | 华为开发者应用、bundleName、签名证书/描述文件、模块权限及应用市场发布配置 |

## 实施顺序

1. 安装与 HBuilderX 配套的 DevEco Studio / HarmonyOS SDK，在华为开发者平台创建鸿蒙应用并配置调试签名。
2. 先实现原生插件的安全存储与安全随机数，让登录、退出和基础页面在鸿蒙真机跑通。
3. 完成聊天流式输出、附件、相册、扫码和语音；回归拒绝授权、取消操作、后台切换及登录失效。
4. 补齐鸿蒙推送与后端通道，再验证本机提醒的可用能力和准确时间。
5. 使用 HBuilderX 的 App-Harmony 本地打包流程生成发布产物，完成真机验证与华为应用市场提交。

当前范围只完成适配审查，未把未实现的鸿蒙原生能力声明为可用。页面可以复用并不代表 APK 可以直接变成鸿蒙原生包。

官方参考：

- [uni-app x 鸿蒙开发指南](https://doc.dcloud.net.cn/uni-app-x/app-harmony/)：本地开发/打包及环境要求。
- [UTS for HarmonyOS](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-for-harmony.html)：通过 `app-harmony` 插件调用 ArkTS API。
- [鸿蒙 Push 模块](https://doc.dcloud.net.cn/uni-app-x/native/modules/harmony/push.html)：可评估的统一推送接入方案。
