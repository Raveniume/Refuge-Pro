# 液态玻璃来源与迁移边界

核查日期：2026-11-09。不要把“加了模糊”写成完成 iOS 交互；检查按压、拖动、取消、释放、返回、字体和深浅色。

## 上游核查

- [BiliPai](https://github.com/jay3-yy/BiliPai) 是 Kotlin/Compose。README 致谢 Miuix、Haze；design-system 依赖 Haze 2.0.1、Cupertino alpha 和 Miuix。`AppLiquidValueControls.kt` 明确标注开关、滑块和阻尼动画来自 Kyant AndroidLiquidGlass（Apache-2.0），并增加水平手势归属、可访问性、任务中断、相邻图层采样。其改进主要在组件集成，不是一个可以替换所有控件的“更强 iOS 库”。
- [BiliPai 控件来源](https://github.com/jay3-yy/BiliPai/blob/01712e7d7ff7cc5e9ff2b1b39eccf9fb8450d724/design-system/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidValueControls.kt)。搜索组件把原生输入层放在共享玻璃底座上，避免在弹窗里重新采样错误窗口。底栏有独立材质参数、滚动反馈和性能分支。
- [FlClash](https://github.com/chen08209/FlClash) 当前使用自己的 Flutter 子模块 [prism_ui](https://github.com/chen08209/prism_ui/tree/63450c0ac0e8e1b2c0f3ed4ad7926dc94e083185)。`liquid_glass.dart`/shader 实现 standard、ultra 材质；`snap_sheet.dart` 实现分档面板、释放速度预测和橡皮筋边界。NOTICE 列出颜色、形状、列表等来源，没有列出可以直接接入 Android Compose 的完整 iOS 组件库。
- BiliPai/prism_ui 的项目许可证为 GPL-3.0；本次没有复制其整段实现或引入 Flutter/Miuix/Haze 并行渲染栈。通过源码研究行为，在 Refuge 已有 Kyant 组件上独立实现适配。原 Kyant 移植继续保留现有 Apache 声明。

## Refuge 实现

- 保留 `backdrop:2.0.0`、`shapes:1.2.0` 和 compose-hig；统一设置和筛选开关到已有 Kyant 液态透镜，而不是普通 CupertinoSwitch。
- 开关有 44dp 触摸高度、Switch 状态/无障碍动作；水平拖动才接管，纵向滚动不切换；通过最新回调提交当前状态。
- 设置增加持久化“**双层液态玻璃**”，默认关闭额外叠层。导航可采样底座与选中透镜两层；搜索通过相邻底座捕获避免自己采样自己，文字始终单独绘制。不开启时保留标准 Kyant 材质。
- 搜索统一胶囊底座、44dp 清除按钮和弹簧展开。保持原生可编辑输入层；此次未改变登录控件。
- 面板保留原有内容高度和滚动逻辑，增加弹簧进场/回弹、释放速度预测、上边界阻力、拖动中断和重复关闭保护；宽屏最大640dp。**不是 prism_ui 全套分档面板或 Flutter 库移植**。
- 光学材质仅用于功能控件和浮动导航。普通数据分组使用 RefugePalette 的实色层级，保持文本对比度；动画使用 Compose 标准动画机制，跟随系统动画时长设置。

## 验收

具体设备测试结果见 `QA_CUSTOMIZATION_REPAIR.md`。需额外检查开关纵向手势、双层开关持久化、搜索清除、面板拉回/关闭和两种主题；不得以研究上游或编译通过替代设备验证。
