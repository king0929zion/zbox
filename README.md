# zbox 消息工具箱

消息工具箱（MessageGuardian）是一款极简的安卓工具箱应用，目前首个功能聚焦于**消息防撤回**：

- 监听用户选定的聊天/社交应用通知，自动保存消息内容
- 即使对方撤回，在应用中依然可以以米白主题的动画消息气泡查看
- 支持多应用选择、通知监听权限引导

## 构建与运行

1. 使用 Android Studio Flamingo 以上版本，直接导入根目录工程
2. 连接设备/模拟器后运行 `app` 模块即可体验

> ⚠️ 项目未在本地执行 Gradle 任务；建议首次在 CI 或 Android Studio 内同步依赖（官方镜像源）。

## GitHub Actions 自动签名打包

仓库已配置 `.github/workflows/android.yml`，在 `push`/`pull_request` 至 `main` 时会：

1. 配置 Temurin JDK 17 与 Gradle 缓存
2. 从仓库 Secrets 还原签名文件
3. 执行 `./gradlew assembleRelease`
4. 上传 `app/build/outputs/apk/release/*.apk` 为构建产物

### 需要的 Secrets

| Secret 名称 | 说明 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 使用 `base64 -w0 your_keystore.jks`（macOS/Linux）或 `certutil -encode`（Windows）得到的 Keystore Base64 字符串 |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore 密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | 密钥密码 |

> 工作流会将 keystore 写入 `app/signing-release.jks`，Gradle 通过环境变量 `SIGNING_*` 自动注入签名配置。

## 权限说明

- `android.permission.POST_NOTIFICATIONS`: 用于申请通知权限（Android 13+）
- 通知监听服务：用于捕捉并保存消息。请在系统“通知使用权”中启用“消息工具箱”。

## 目录结构

```
zbox/
├─ app/
│  ├─ src/main/
│  │  ├─ java/com/example/messageguardian
│  │  └─ res
├─ gradle/
│  └─ wrapper
└─ .github/workflows/android.yml
```

欢迎后续扩展更多工具箱能力。
