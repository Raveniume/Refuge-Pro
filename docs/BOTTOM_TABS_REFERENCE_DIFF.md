# Bottom Tabs Reference Diff

Reference source: `third_party/AndroidLiquidGlass/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt` at the checkout recorded in `docs/REFERENCE_SOURCES.md`.

| Official `LiquidBottomTabs` | Refuge `RefugeBottomTabs` | Difference and reason |
| --- | --- | --- |
| `Backdrop` is the page backdrop passed to the outer capsule and hidden tab layer. | Same `Backdrop` is supplied by `PageGlassScope`, which combines wallpaper and rendered page content before navigation is drawn. | Refuge needs page content behind the root nav; this is the explicit production layering contract. |
| `tabsBackdrop = rememberLayerBackdrop()` records the hidden tab content. | Same local `tabsBackdrop`. | No difference. |
| Hidden tab layer uses `drawBackdrop(backdrop = backdrop, shape = Capsule(), vibrancy, blur, lens)` before `layerBackdrop(tabsBackdrop)`. | Same official draw order and effects for the hidden tab layer. | Restored after the earlier implementation omitted this sampling pass. |
| Selected lens uses `rememberCombinedBackdrop(backdrop, tabsBackdrop)`. | Same combined backdrop. | No difference. |
| Selected lens is positioned at `value * tabWidth`, with `tabWidth = (width - 8dp) / tabsCount`. | Same coordinate origin and tab width for both bottom tabs and segmented controls. | Removed local overscan and negative translation that could expose a hard crop of page imagery. |
| Selected bounds are `height = 56dp`, `fillMaxWidth(1f / tabsCount)`. | Bottom nav visual selected lens is 46dp inside a 54dp optical track; segmented lens is  height minus 18dp. | Refuge lowers visual density while retaining the official equal-width coordinate mapping and a larger outer hit area. |
| Outer capsule is 64dp with 4dp padding. | Outer capsule is 54dp with 4dp padding. | Production visual density requirement; interaction and backdrop pipeline are unchanged. |
| Drag maps pointer delta by `dragAmount.x / tabWidth`; release rounds target and springs to it. | Same `DampedDragAnimation` and release/velocity behavior. | No difference. |
| `InteractiveHighlight` is centered from the same `value * tabWidth` mapping. | Same center mapping. | Prevents the highlight and lens from drifting apart. |
| Outer layer uses `vibrancy`, `blur(8dp)`, `lens(24dp, 24dp)`. | Same effects, with low-alpha tint only. | Static fill is deliberately restrained so refraction remains the primary selected-state signal. |
| Selected lens uses interaction-driven lens, highlight, shadow, inner shadow, and velocity deformation. | Same effects and deformation. | No difference in interaction semantics. |

## Validation notes

- The production implementation does not replace `drawBackdrop` with a translucent fill.
- The selected state is a moving lens over the combined page/content backdrop; the low-alpha surface tint is only a legibility aid.
- Root navigation and segmented controls both call the shared `ReferenceLiquidSelectionBar`/`ReferenceLiquidBottomTabs` pipeline through `RefugeMovingLiquidLens`/`RefugeBottomTabs`.
