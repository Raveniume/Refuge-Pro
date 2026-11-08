# 发布流程

1. 读取 `version.properties` 中的 `versionCode` 和 `versionName`。只有准备创建正式 Release 时才递增版本号；日常视觉修复保持当前版本号。
2. 在 `CHANGELOG.md` 中添加同版本条目，然后运行 `./gradlew testDebugUnitTest assembleReview`、`git diff --check` 和模拟器视觉回归。`review` APK 使用本机 debug 签名，适合覆盖安装和交互验收。
3. 检查待发布文件，确保不包含个人账号资料、会话令牌、私钥、本机路径或运行时缓存。
4. 将源码、测试、工作流和项目文档提交到 `main`，推送至 `https://github.com/Raveniume/Refuge-Pro`。
5. 配置 GitHub Actions secrets：`KEYSTORE_BASE64`、`KEY_STORE_PASSWORD`、`KEY_PASSWORD` 和 `KEY_ALIAS`。Release workflow 会拒绝在缺少任一 secret 时构建或上传未签名 APK。
6. 为版本创建 `v<versionName>` 标签并推送。`.github/workflows/release.yml` 会运行 `testDebugUnitTest assembleRelease`，从 `CHANGELOG.md` 提取该版本正文，并将 `app-release.apk` 发布到 GitHub Release。
7. 安装前核验 APK 包名、versionCode 和证书。设备与正式包证书一致时，使用 `adb install -r` 覆盖安装进行最终检查。共享模拟器若仍是旧 debug 证书，禁止卸载或清数据来伪造覆盖通过；安装同签名 review 验证保留会话，并分别核对前后正式 Release 的证书和递增版本号。在验收文档和交付中注明未在该设备执行正式签名覆盖，不将签名核验等同于实机覆盖测试。
