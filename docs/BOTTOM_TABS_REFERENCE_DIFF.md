# Bottom Tabs Reference Diff

This note records the production mapping from the AndroidLiquidGlass catalog
component to Refuge's shared navigation primitive. The optical implementation
is kept in `OfficialLiquidPorts.kt`; Refuge only supplies its labels, icons,
theme, and selected index.

| Concern | Official `LiquidBottomTabs` | Refuge `OfficialLiquidBottomTabsPort` |
| --- | --- | --- |
| Backdrop | `backdrop` is the page backdrop passed to the outer and hidden rows | Same `Backdrop`; `RootBottomNav` receives the page-scoped combined backdrop |
| Lens | Outer vibrancy/8dp blur/24dp lens; selected lens uses the combined backdrop with 10dp/14dp chromatic lens | Direct port, with only explicit light/dark container colors and a five-tab API |
| Clip | `Capsule()` is used for the outer row, hidden selected row, and moving lens | Same `Capsule()` geometry |
| Selected bounds | Outer row is 64dp, hidden selected row is 56dp, moving lens is 56dp and one tab wide | Same official geometry; hit area remains the full tab row |
| Drag | `DampedDragAnimation`, tab-width mapping, panel offset, velocity deformation, spring settle | Same animation and mapping; `selectedIndex` is a state value instead of a lambda |
| Highlight | `InteractiveHighlight` tracks the moving tab center and press progress | `ReferenceInteractiveHighlight`, the existing direct port of the catalog helper |
| Vibrancy | Official `vibrancy()` on outer/hidden layers | Same |
| Blur | Official 8dp background blur on tab rows | Same |
| Coordinate mapping | Moving lens translation is `drag.value * tabWidth + panelOffset`; hidden row is layered before the combined backdrop | Same origin and translation; no local screenshot crop or content image is inserted |

The only intentional Refuge differences are the five production tabs, the
theme accent/container tokens, and the selected-index callback. No alternate
selected pill, border, ripple, or custom drag implementation is used.
