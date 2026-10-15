# AI小醒统一下载页

入口：`https://47.100.172.149/download/`，适配手机浏览器与电脑端。使用用户提供的背景图，复刻胶囊导航、主标题、三个平台按钮、提示气泡与产品介绍。二维码弹窗提供复制网址和保存图片；后台同样显示、导出二维码。

## 后台管理

在原「智能提醒 → APP版本管理」上方新增「统一下载页」配置，原 APK 上传、草稿、发布、下架、删除行为保留。

- 统一下载页网址：实际部署的 HTTPS 网址，是二维码编码内容。修改网址不会自动购买域名或配置 DNS；更换域名后先确保该地址可访问。
- Android：独立开关，自动下载版本代码最高的已发布 APK。
- iOS：独立开关，可选 App Store 官方链接或 TestFlight 公开邀请链接；两种链接分别保存，切换方式不会丢失另一种链接。
- 鸿蒙 APK 兼容版：独立开关，面向支持 APK 的 HarmonyOS 4.x 及更早版本，使用同一个最新已发布 APK。
- 原生鸿蒙：独立开关，面向 HarmonyOS 5 及以上 / NEXT，通过配置的华为应用市场链接下载。启用前必须提供链接。
- 关闭的平台仍保留按钮，置灰且不可点击。鸿蒙另有总开关，关闭后同时停用兼容版与原生版入口，但保留配置。没有已发布 APK、网络不可用或微信内下载被限制时均有对应说明。

默认开放 Android 与鸿蒙 APK 兼容版，iOS 和原生鸿蒙关闭，商店链接留空。不会猜测或创建未经提供的 App Store、TestFlight、AppGallery 地址。

用户点击「鸿蒙」后选择系统类别。H5 用户代理无法可靠判断全部鸿蒙机型/版本，不能只凭“华为品牌”把 HarmonyOS NEXT 导向 APK。华为当前也支持部分 APK 通过卓易通等服务安装，兼容范围取决于该服务；这里按要求把原生鸿蒙导向应用市场，不承诺本 APK 在这些容器中的兼容性。

官方参考：[HarmonyOS 5 及以上应用下载安装介绍](https://consumer.huawei.com/cn/support/content/zh-cn16061787/)、[应用下载直达服务](https://developer.huawei.com/consumer/cn/agconnect/huawei-AppGalleryDownload/)。

## 接口与权限

- `POST /blade-smart/admin/releases/download-page`：管理员读取配置。
- `POST /blade-smart/admin/releases/download-page/save`：管理员保存配置。
- `GET /app/download/config`：公开平台配置和已发布版本的必要元数据。
- `GET /app/download/android.apk`：无需登录，实时解析当前最新已发布 APK。仅 Android 开启或“鸿蒙总开关 + APK 兼容版”同时开启时开放；草稿或全部下架时禁止下载。
- `GET /app/download/qr`：公开 PNG，600×600，编码后台配置的统一下载页网址。

现有 APP 更新接口仍保留原角色认证。仅三个明确的公开路由加入认证放行列表，管理员路由不放行。保存链接只接受 HTTPS；苹果/鸿蒙商店域名有精确白名单，不接受伪造子域名、用户名密码、脚本协议、非标准端口。TestFlight 校验 `/join/邀请码` 格式。公开页面使用 `textContent` 显示更新说明，跳转再次校验官方域名。二维码在服务器生成，不使用外部二维码服务。

新增表 `blade_app_download_page`，仅一条配置（id=1），幂等创建；没有修改用户、事件、邮件配置或原版本数据。

## 验证与发布

- `scripts/run-download-page-regression.ps1`：20 项下载配置、链接、权限开关、发布解析、二维码解码检查；原 `AppReleaseRegression` 的 15 项上传/草稿/发布/下架/删除检查继续通过。
- `code/front/scripts/download-page-browser.js`：使用 Playwright CLI 检查真实静态页面及真实后台 Vue 组件，路由用本地夹具；验证 APK 下载、平台关闭、两种 iOS 跳转、原生鸿蒙商店、网络重试、320/390px 无横向溢出、后台必填和保存字段。
- 管理后台生产构建通过。App 原生源码与版本号不变，无需为此下载页重新云打包。
- `scripts/deploy-download-page.py`：保留生产 JAR 的其余类与配置，仅增加下载页类、明确放行列表和配置保存的 XSS 例外；旧网页静态块保留，以兼容已打开的管理后台。后端健康后切换网页目录，可同时回退网页与后端，新增配置表保留。

截图位于 `output/playwright/download-desktop.png`、`download-mobile-390.png`、`download-mobile-320.png`、`download-qr.png`、`download-admin.png`。浏览器夹具中的链接和版本仅用于验证，生产使用数据库中的实际已发布版本。

## 生产验证结果

2026-10-15 已上线 `https://47.100.172.149/download/`。匿名访问页面、配置及二维码成功；匿名 APK 下载的文件大小和 SHA-256 与已发布版本一致，APK 内含完整 AndroidManifest 和 dex。当前数据库最高已发布版本为 1.0.2（125），本次没有上传或替换 APK。管理员配置接口及原 APP 版本接口仍阻止匿名访问。真实 HTTPS 页面通过电脑、390px 手机布局和二维码弹窗检查；实体手机浏览器尚未实测。

首次发布因服务器磁盘不足自动回退；清理 Docker 未使用构建缓存后恢复约 7 GB 空间，重新发布成功，后端健康。发布脚本现检查发布前磁盘余量与现有服务健康，用户数据和现用/回退镜像保留。
