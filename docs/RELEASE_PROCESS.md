# 发布流程

1. 读取 `version.properties` 中的 `versionCode` 和 `versionName`，每次可交付更新都递增版本号。
2. 在 `CHANGELOG.md` 中添加同版本条目，然后运行单元测试、APK 构建、`git diff --check` 和模拟器视觉回归。
3. 检查待发布文件，确保不包含个人账号资料、会话令牌、私钥、本机路径或运行时缓存。
4. 将源码、测试和项目文档提交到 `main`，推送至 `https://github.com/Raveniume/Refuge-Pro`。
5. 创建对应版本的 GitHub Release，正文只列产品更新，并附上本版本 APK。
6. 使用 `adb install -r` 覆盖安装进行最终检查，保留应用数据和登录状态。
