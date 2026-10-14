# APP 1.0.131 / 131 — 好友、登录状态和事件交互

## 本次调整

- 好友资料中的姓名使用非空 name / realName，备注单独保存在 friend_remark；空字符串不再挡住本名。
- 将业务响应“认证信息错误或无效”及停用账号响应识别为认证失效。有效刷新令牌仍可恢复；刷新失败清空登录状态并回登录页，并发和后续请求不重复重启页面。
- 提醒抽屉使用淡蓝到浅绿背景、半透明白色卡片、蓝绿紫暖色分组。支持向左拖动收起，短距离回弹；手势方向锁定，不干扰上下滚动，滑动后抑制卡片误点。
- 事件列表支持左滑切到“我收到的”、右滑切到“我发起的”。
- 发起事件返回按分支顺序排列的接收人头像（最多 4 个），保留总人数并在超过 4 人时显示省略号；收到事件返回发起人头像。人员 ID 保持字符串，使用批量窗口查询限制缩略图数量。
- 好友/权限申请在事务内生成 FRIEND_REQUEST 消息和推送队列。iOS APNs、Android 后台接收支持该类型，点击进入“我的 → 申请待通过”，包括已经在“我的”页面的情况。处理申请后标记消息已读，取消尚未发送的推送。跳转检查接收人账号。

## 服务端发布

2026-10-14 发布 `aimessage-backend:social131-20261014`。

- 发布目录：`/opt/aimessage/releases/social131-20261014`
- 配置备份：`/opt/aimessage/backups/pre-social131-20261014`
- 精确替换 SmartReminderService、PushOutbox、PushSchema、PushDeliveryWorker 及服务内部类，保留其他 JAR 条目和生产配置。
- Java 编译保留 `-parameters`；保留原有 JVM 兼容参数。
- 兼容迁移：push delivery 的 event_id 允许 NULL，好友申请不伪造事件 ID。旧版事件通知继续使用事件 ID。回滚镜像时可保留该可空列。

## 验证

- APP Node 回归 86 项全部通过；项目 Vue/UTS/路由检查通过。
- HBuilderX 5.24 Android 资源生成、全部页面及共享/原生 Kotlin 编译通过；通知游标测试覆盖好友申请、去重和账号隔离。
- iOS 资源生成及生成桥接检查通过。本环境无 Xcode，未完成签名 IPA / 真机系统通知验证。
- H2 Social131Regression：好友申请与通知同事务、重复申请、姓名空值、本名、接收人范围、头像数量及超出 JS 安全整数的 ID。
- H2 PushRegression：旧 outbox 可空迁移、无事件的好友通知、点击载荷、已读取消、账号隔离、原有 APNs 重试流程。
- FriendRemarkRegression 通过，确认修改备注不改本名或其他人的备注。
- Playwright：402px/320px 页面、两方向切换、纵向不误切、抽屉侧滑、好友本名、认证失效清凭证回登录、申请入口跨页面/同页面跳转。
- 生产临时 APP 账号验证：真实登录、好友申请、chat/sync 的好友消息、重复拦截、接受申请、已读状态、备注本名、收发事件头像、数据库迁移。测试账号没有绑定手机推送，不发送真实设备通知或邮件；测试数据已清理。

后台已更新。手机需通过 HBuilderX 云打包安装 **1.0.131（131）**；本地编译检查不生成可安装 APK/IPA。

## 回归命令

```powershell
node --test code/APP/tests/*.test.mjs
node code/APP/tests/check-project.mjs
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-social131-regression.ps1
```

后端脚本复用 stage1 的本地依赖 classpath 和 H2，以及已有 target/classes / account130 编译依赖；原生检查需先用 HBuilderX 生成对应平台资源。浏览器测试顺序见 APP tests/visual/README.md。
