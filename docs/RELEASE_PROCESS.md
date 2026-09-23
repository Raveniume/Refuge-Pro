# 发布流程

1. 从 `version.properties` 读取当前 `versionCode` 和 `versionName`，每次可交付更新都递增版本号；补丁修复递增最后一位，功能发布递增中间位。
2. 先完成单元测试、APK 构建和模拟器视觉回归，再在 `CHANGELOG.md` 添加同版本条目。
3. 提交源代码、测试和设计准则到 `main`，推送到 `https://github.com/Raveniume/refuge-pro`。
4. 创建同名 GitHub Release（例如 `v0.2.0`），上传 `app/build/outputs/apk/debug/app-debug.apk`，并在 Release 说明中引用对应变更日志。
5. 后续 APK 安装使用 `adb install -r`，保持应用数据、登录会话和本地缓存不变。
