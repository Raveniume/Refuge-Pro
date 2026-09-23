<div align="right"><a href="#refuge-pro">简体中文</a> · <a href="#refuge-pro-en">English</a></div>

<div align="center">

# Refuge Pro

**《Star Citizen》账户、机库、商店与舰船配置工具**

[![Latest release](https://img.shields.io/github/v/release/Raveniume/Refuge-Pro?display_name=tag&style=for-the-badge)](https://github.com/Raveniume/Refuge-Pro/releases/latest)
[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Raveniume/Refuge-Pro/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://github.com/Raveniume/Refuge-Pro)

[下载最新 APK](https://github.com/Raveniume/Refuge-Pro/releases/latest) · [查看更新日志](CHANGELOG.md) · [报告问题](https://github.com/Raveniume/Refuge-Pro/issues)

</div>

Refuge Pro 将 RSI 账户机库、商店和升级目录、舰船资料以及原生改船计算器放在一个 Android 应用中。界面提供中文体验，常用公开目录可离线浏览；登录后的账户内容会按账户保存在本机，并在网络可用时更新。

## 功能一览

### 机库与账户

- 查看 RSI 账户中的舰船、礼包、装备、回购项目和历史记录。
- 浏览舰船图片、制造商、价值、已付金额、保险、入库时间和条目详情。
- 通过机库入口查看升级选项；已拥有的 CCU 收纳在独立的二级页面。
- 登录状态和账户资料保存在应用私有空间，更新安装时保留现有数据。

### 商店与升级

- 浏览商店商品、舰船、制造商、图片、价格和折扣信息。
- 优先显示本地可用目录，并在后台更新 RSI 数据；网络不可用时保留最近一次有效内容。
- 折扣角标对应实际折扣商品数量。
- 升级目录支持从舰船到舰船的 CCU 路径选择。

### 终端与 Wiki

- 搜索舰船、武器、护盾和其他装备资料。
- 查看制造商、规格、图片以及按类别整理的条目详情。
- 提供筛选、排序和快速搜索。

### 舰船改装

- 选择舰船并更换可用挂点上的组件。
- 切换 LIVE / PTU 数据，调整电力分配和组件启用状态。
- 查看武器 DPS、护盾、电力、散热与性能汇总。
- 保存和调用配置方案。
- 使用本地目录快速打开规划器，并在后台刷新可用数据。

### 个性化

- 深色和浅色显示模式。
- 头像状态菜单与即时状态指示。
- 个人头像只从账户数据加载；未登录或图片不可用时使用通用占位图。

## 隐私

账户密码、RSI 会话信息和运行时账户缓存存放在 Android 应用私有存储中，不属于公开舰船目录资源。应用不会将运行时登录信息写入项目文件。仓库中的测试账号和凭据字符串仅用于自动化测试，不可用于登录。

反馈问题或分享日志、截图前，请检查是否包含邮箱、账户头像地址、Cookie、令牌或个人资料。

## 下载与安装

从 [Releases](https://github.com/Raveniume/Refuge-Pro/releases/latest) 下载最新 APK，安装后使用 RSI 账户登录。后续安装较新版本时可直接覆盖安装，以保留登录状态和应用内缓存。

## 参考资料

- [RefugeNext](https://github.com/summerkirakira/RefugeNext)：既有页面结构、筛选项与资料组织参考。
- [Erkul Games Calculator](https://erkul.games/calculator)：舰船配置与性能计算参考。
- [Star Citizen Tools](https://starcitizen.tools/)：舰船和装备 Wiki 资料参考。
- [compose-hig](https://github.com/ienground/compose-hig)：Android Compose 组件参考。
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)：Android 液态玻璃效果参考。

## 本地构建

项目使用 Kotlin、Jetpack Compose 和 Gradle，构建需要 JDK 17 与 Android SDK。

```powershell
.\gradlew.bat assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

---

<div id="refuge-pro-en"></div>

# Refuge Pro (English)

<div align="right"><a href="#refuge-pro">简体中文</a></div>

**An Android companion for Star Citizen accounts, hangars, store listings, ship data, and loadout planning.**

Refuge Pro brings RSI account hangar data, store and upgrade listings, ship references, and a native loadout planner into one Android app. Public reference data remains available offline. Account data is stored locally and refreshed when a connection is available.

## Features

### Account and Hangar

- Browse ships, packages, equipment, buyback entries, and account history.
- Review ship images, manufacturers, values, paid amounts, insurance, acquisition dates, and item details.
- Open upgrade options from the hangar; owned CCUs live in a dedicated secondary view.
- Keep sign-in state and account data in app-private storage across in-place updates.

### Store and Upgrades

- Browse store items, ships, manufacturers, images, prices, and discounts.
- Show the local catalog first and refresh RSI data in the background, retaining the last usable copy when offline.
- Count genuinely discounted items in the store badge.
- Build CCU paths from a starting ship to a target ship.

### Terminal and Wiki

- Search ships, weapons, shields, and other equipment.
- Read manufacturers, specifications, images, and categorized item details.
- Filter, sort, and search reference entries.

### Ship Loadout Planner

- Select a ship and replace components in its available hardpoints.
- Switch between LIVE and PTU data, adjust power distribution, and toggle component operation.
- Review weapon DPS, shields, power, cooling, and performance summaries.
- Save and restore loadout plans.
- Open quickly from local data while available online data refreshes in the background.

### Personalization

- Light and dark appearance.
- Avatar presence menu with an immediate visual indicator.
- Account avatars are loaded from account data; a generic placeholder is used when unavailable.

## Privacy

Passwords, RSI session data, and runtime account caches stay in Android app-private storage and are not part of the public ship-reference assets. Runtime login data is not written into project files. Test account and credential strings in this repository are synthetic values used only by automated tests.

Before sharing logs or screenshots, check for email addresses, avatar URLs, cookies, tokens, or profile details.

## Download

Download the latest APK from [GitHub Releases](https://github.com/Raveniume/Refuge-Pro/releases/latest), install it, and sign in with your RSI account. Newer versions can be installed over the existing app to retain sign-in state and local data.

## References

- [RefugeNext](https://github.com/summerkirakira/RefugeNext): existing page structure, filters, and data organization.
- [Erkul Games Calculator](https://erkul.games/calculator): ship loadout and performance calculations.
- [Star Citizen Tools](https://starcitizen.tools/): ship and equipment reference articles.
- [compose-hig](https://github.com/ienground/compose-hig): Android Compose components.
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass): Android Liquid Glass effects.

## Build from source

The project uses Kotlin, Jetpack Compose, and Gradle. A JDK 17 installation and Android SDK are required.

```powershell
.\gradlew.bat assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
