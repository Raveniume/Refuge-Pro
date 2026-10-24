# RefugeNext 修复验收清单

本清单对应 2026-10-25 的八项回归要求。账户数据和模拟器状态必须保留；禁止执行溶解船、回收、购买、赠送、升级等会改变账户的操作。

| 编号 | 要求 | 实现 | 自检 |
|---|---|---|---|
| 1 | APK 可直接覆盖安装 | debug/review 在正式签名可用时复用 `refugeRelease`，applicationId 固定为 `com.refuge.next.compose` | 待共享模拟器 `adb install -r` |
| 2 | Damage、图标、Power Management、多个 Cooler 与核心栏位 | Damage/武器/防御/船体/飞行图标按 Erkul 语义重绘；电力图标移除圆形底；按真实消费者逐项显示 Cooler/PowerPlant；保留完整电力组 | 已通过深浅色改船截图与电力断言 |
| 3 | 改船右上菜单和按钮可用 | 保留刷新、更多、性能、保存、调用、设置入口 | 已通过共享模拟器菜单/设置用例 |
| 4 | 蓝图中英文、紧凑材料块、品质滑杆与属性变化 | 材料改为“部位 / 材料 | SCU”蓝色圆角分组，加载块限宽 | 已通过真实中英文列表、详情与系统返回用例 |
| 5 | 登录后后台更新，页面进入按版本号复用缓存 | SCMDB 任务版本和蓝图 `dataVersion` 在进入时先做轻量检查；版本相同复用本地大数据，变化才完整更新；Erkul 使用 manifest 版本 | 已通过真实任务/蓝图/改船用例 |
| 6 | 详情页不使用左上角圆形返回 | 任务/蓝图详情、改船页、组件选择页不绘制顶部返回控件，仅由系统返回或父级菜单关闭 | 已通过语义断言与系统返回用例 |
| 7 | 设置页隐藏底部 navbar | navbar 仅在四个主根路由绘制 | 已通过设置页设备回归 |
| 8 | 设置、改船、详情进入/返回无动画 | 根路由、改船内部路由、任务/蓝图详情均直接替换；系统返回仍由 `BackHandler` 处理 | 已通过真实任务/蓝图/改船用例 |

## 允许的验证

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`、`assembleReview`
- 共享 AVD 上的 `adb install -r`、启动、截图、UIAutomator 和 instrumentation
- 改船、蓝图、任务、设置、我的页面的只读浏览和刷新

## 禁止的验证

- 溶解船、回收、购买、赠送、升级以及任何会提交账户变更的按钮

## Instrumentation note

The first full run reached 39/40 completed tests with one optional test skipped and one settings test timing out because it still expected the hidden bottom navbar to navigate away from Settings. That test now exits through the visible Settings back affordance; its targeted rerun completed without failure (the shared session was unavailable and the optional test was skipped).
