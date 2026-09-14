# 验收反馈修复：资料、密码入口、语音与提示

## 修改

- 重置密码移入“我的 → 设置 → 账号安全”，保留验证原密码和两次新密码；不再在个人资料卡下单独悬浮。
- 编辑资料支持姓名（必填，最多20字符）、手机号（选填，11位）、邮箱（选填，最多45字符）。前后端校验，选填项可清空；不接受修改账号、角色或他人ID。保存姓名同步name/real_name，省略头像或联系方式时保留原值，兼容旧APP。
- App路由的Element Plus提示统一增加安全区偏移，覆盖登录页及teleport到body的提示，同时保留多条提示堆叠布局；不影响管理后台。
- 网页录音的AudioContext在点击同步创建/恢复，避免异步加载造成iOS用户激活丢失；使用真实PCM音量驱动波形，无音频采集超时明确提示，全零录音不上传。完成/取消/错误释放麦克风，不自动发送识别文字、不清空草稿。转写失败只在输入框显示一次。

## 语音通道

- 旧系统默认paraformer-realtime-v2通道由阿里云服务器直接测试中文WAV、MP3，两次HTTP200、text长度0；与原界面空识别报错一致。
- 经用户明确指定，更换为 `https://api.siliconflow.cn/v1/audio/transcriptions`，模型 `FunAudioLLM/SenseVoiceSmall`。按[官方multipart接口文档](https://docs.siliconflow.cn/docs/api/audio-transcriptions-post)发送file和model。
- 新通道在服务器测试WAV/MP3均返回中文“你好，请明天下午3点提醒我 参加项目会议。”。
- 密钥通过无回显输入，经SSH stdin传递，沿用系统AES-GCM加密存储；源代码及本记录无密钥。仅修改原系统默认SPEECH配置，不改对话模型或个人自定义配置。
- 旧加密配置备份：`/opt/aimessage/backups/speech-before-siliconflow-20260914.json`。如需回退须确认当前配置未被管理员再次修改，恢复该行字段；不恢复整个数据库。
- 新接口是录音完成后转写，不承诺网页逐字流式；原生iOS已有Speech实时桥接仍需新安装包和真机验收。

## 验证

- 前端Node测试12项通过；后端ProfileRegression隔离H2测试10项通过，含他人用户不变、旧头像客户端兼容、选填清空及错误校验。
- 后端生产JAR、网页生产包和原生网页包构建成功。仓库历史测试模板不能编译，Maven使用maven.test.skip，另运行上述独立测试。
- Playwright模拟原生/API交互10组通过（资料回显/保存、提示偏移、密码确认、静默刷新、原生语音与键盘回归）。浏览器录音5组通过：真实PCM信号动画、识别草稿回填、静音拒绝、释放麦克风、错误单次显示；没有使用真实用户或真实麦克风。
- 真机麦克风、iOS灵动岛及键盘体验仍由用户更新安装包后确认。

## 线上发布

- 已发布至 https://www.chentong.xyz/app/login；后端healthy、重启次数0。普通网页刷新生效；原生App本地资源需重新打包更新。
- 发布：`/opt/aimessage/releases/profile-polish-20260914-1530`；备份：`/opt/aimessage/backups/pre-profile-polish-20260914-1530`。
- 原镜像为 `aimessage-backend:mobile-upgrade-20260914-1430`，新镜像为 `aimessage-backend:profile-polish-20260914-1530`。7份内嵌配置一致，保留运行时配置和APNs挂载；数据库仅核验已有账号索引，无新增表字段，发布时21个用户保留。
- JAR SHA256：`da83e43b293b8e137318641ccdad63b5fcba57d7be9876afa4c7b30e337f8bb4`；网页压缩包：`8f8a816e6fbb98c42f1ebf09f968b632bf2bf4931aa5b31981cb75709f6e1c05`。
- 公网HTML SHA256与本地一致：`73d48d29cde004fc85a352e2ec7e45ba60061dcfdde95630d9d2932228f90292`，4个入口JS/CSS资源返回200。账号/资料/语音保护及预检8项、既有公网CORS回归18项全部通过。没有真实用户注册、密码修改或测试推送。
- 应用回滚：确认没有后续部署或compose修改后，服务器执行 `python3 /opt/aimessage/public-profile-polish-deploy.py rollback`，再检查健康状态；只恢复旧后端和网页，不恢复数据库，不撤销已成功验证的SenseVoice配置。
- 未执行Git提交/推送或TestFlight发布。
