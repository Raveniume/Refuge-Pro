# Refuge Pro

Refuge Pro 是一个面向《Star Citizen》玩家的原生 Android 工具，用于查看 RSI 账户机库、回购与升级记录、商店目录、舰船与装备资料，并在改船页面进行本地化的配置规划。项目以现有 RefugeNext 的信息结构和数据边界为背景，使用 Kotlin 与 Jetpack Compose 重建交互层，保持数据可追踪、离线可用和页面操作连续。

当前版本：**0.2.1**（`versionCode 3`）

## 功能范围

### 机库

- 读取 RSI 账户的舰船、礼包、装备和历史项目。
- 展示舰船图片、制造商、价值、已付金额、保险、入库时间和项目详情。
- 支持回购、赠送、回收和升级入口；需要账号确认的操作会在提交前再次要求密码。
- 已拥有的 CCU 在升级页的二级页面中展示，避免主列表被操作项挤满。

### 商店与升级

- 商店目录按本地缓存优先展示，后台同步 RSI 实时价格、折扣、制造商和图片。
- 升级目录从 RSI 官方升级接口读取，并保留最近一次有效快照，网络不可用时仍可选择舰船和 CCU。
- 折扣角标只统计当前目录中实际处于折扣状态的商品。
- 页面切换不会清空已有内容；只有用户主动下拉刷新时才显示刷新反馈。

### 终端与 Wiki

- 内置舰船、组件和装备资料，包含制造商、规格、用途和图片。
- Wiki 信息按 Star Citizen Tools 的条目结构组织，支持从舰船、武器、护盾和组件进入详情。
- 常用筛选项、排序和搜索保持与旧版 RefugeNext 的行为一致。

### 原生改船

- 以 Erkul Games Calculator 的舰船选择、挂点换装和性能总览为参考，使用 Compose 原生控件实现。
- 支持 LIVE/PTU 数据模式、舰船选择、组件选择、启用状态、电力分配、护盾、DPS、散热和性能汇总。
- 计算页面优先使用本地目录与上次快照，打开后在后台刷新云端数据；刷新完成后原位替换结果。
- 方案保存与调用放在右上角菜单中，状态总览和电源分配器保持独立模块，便于小屏滚动操作。

### 账户与状态

- 登录会话和本地缓存保存在 Android 应用私有存储中，覆盖安装不会清除它们。
- 头像状态先更新本地界面，再尝试同步 RSI Spectrum；网络失败时保留待同步标记并在下次认证后重试。
- 浅色模式使用纯白基底，深色模式使用纯黑基底；文字、图标和边界在两种模式下都保持可读。

## 设计与交互约束

项目统一采用原生 Compose 组件，视觉基线遵循 Apple Human Interface Guidelines。液态玻璃控件参考 [compose-hig](https://github.com/ienground/compose-hig) 和 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)，具体验收规则记录在 [`DESIGN_GUIDELINES.md`](DESIGN_GUIDELINES.md)。其中包括：

- 根导航固定为“机库、商店、终端、我的”。
- 顶部导航、底部导航和弹窗使用一致的高度、间距、连续圆角和触控区域。
- BottomSheet 在当前页面上原地展开，底层内容在动画期间保持可见；按钮悬浮在内容上方，不绘制独立遮罩条。
- 弹窗返回使用圆形图标按钮，按钮圆角与弹窗顶角保持同一连续圆角几何。
- 页面先展示本地缓存，再进行后台更新；普通页面切换不显示全屏加载图标。
- 图标、选择器、筛选器和状态反馈使用原生语义和可访问描述，避免无意义的自动生成说明文字。

## 数据来源与隐私

应用只在用户主动登录后访问 RSI、Spectrum 和相关公开资料服务。账号令牌、密码、Cookie 和缓存写入 Android 应用私有存储，不写入仓库、不打包进 APK 资源，也不会在日志中输出。仓库中的测试邮箱、测试密码和示例令牌均为合成夹具，不能用于登录。

本仓库包含用于离线首屏和视觉回归的公开舰船资料、翻译数据与示例图片。真实账户数据只在运行时从 RSI 获取，并按账户隔离缓存。提交问题或日志时，请先删除个人邮箱、头像地址、Cookie、令牌和账户截图。

## 构建与安装

环境要求：JDK 17、Android SDK 37，以及可联网的 Gradle 构建环境。

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

生成文件：`app/build/outputs/apk/debug/app-debug.apk`

覆盖安装并保留应用数据：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.refuge.next.compose/com.refuge.next.MainActivity
```

## 版本与发布

版本号集中在 [`version.properties`](version.properties)：

```properties
versionCode=3
versionName=0.2.1
```

每次可交付更新都必须递增版本号，并在 [`CHANGELOG.md`](CHANGELOG.md) 添加同版本条目。发布前运行单元测试、APK 构建、`git diff --check` 和模拟器视觉回归；发布流程详见 [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md)。

GitHub 仓库：[Raveniume/Refuge-Pro](https://github.com/Raveniume/Refuge-Pro)

## 参考项目

- [RefugeNext](https://github.com/summerkirakira/RefugeNext)：信息结构、旧版筛选项和资料展示参考。
- [Erkul Games Calculator](https://erkul.games/calculator)：舰船改装和性能计算参考。
- [compose-hig](https://github.com/ienground/compose-hig)：Compose HIG 组件和 BottomSheet 动画参考。
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)：液态玻璃材质、控件反馈和光学层参考。
- [Star Citizen Tools](https://starcitizen.tools/)：终端 Wiki 条目和公开规格参考。

