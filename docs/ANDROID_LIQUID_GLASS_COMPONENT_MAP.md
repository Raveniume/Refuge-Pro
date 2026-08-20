# AndroidLiquidGlass Component Map

The Compose lab follows the official catalog's layering model. It does not use a static translucent fill as a substitute for the lens.

| Official reference | Local implementation | Shared behavior | Evidence |
|---|---|---|---|
| `LiquidBottomTabs.kt` | `ReferenceLiquidSelectionBar` + `ReferenceSelectionItem` | `rememberLayerBackdrop`, combined backdrop for the selected lens, `lens`, `Highlight`, `Shadow`, `InnerShadow`, damped drag, velocity-aware scale | `v4-bottom-tab-release.png`, `v4-bottom-tab-drag.mp4` |
| `LiquidBottomTabs.kt` selection layer | `ReferenceSegmentedControl` | Same `ReferenceLiquidSelectionBar` and selected moving lens pipeline; only geometry/labels differ | `v4-segmented-middle.png`, `v4-segmented-drag-release.png`, `v4-segmented-drag.mp4` |
| `LiquidButton.kt` | `ReferenceLiquidButton` | `drawBackdrop`, `vibrancy`, `blur`, `lens`, pointer-tracked `InteractiveHighlight`, deformation and `Role.Button` semantics | `v4-floating-button.mp4` |
| Functional glass field pattern in catalog | `ReferenceSearchField` | Local backdrop, restrained blur/lens/vibrancy, semantic `Search` label, stable single-line field | `v4-search-field.png`, `v4-ui.xml` |
| Catalog dialog/backdrop patterns | Sheet and alert blocks in `ReferenceLabScreen` | Explicit scrim, opaque modal base, local modal backdrop for actions, no recursive sampling of the page root | `v4-sheet-dark.png`, `v4-alert-dark.png` |
| Optical/backdrop primitives | `ReferenceOpticalTest` | `LayerBackdrop` for traces plus `rememberCombinedBackdrop` and the same `lens` pipeline used by real controls | `v4-dark-launch.png`, `v4-light-launch.png` |

## Pipeline contract

1. Page content is recorded into a backdrop.
2. A component samples that backdrop through `drawBackdrop`.
3. `blur`, `vibrancy`, and `lens` provide the optical surface; low-alpha color is only a restrained surface tint.
4. The selected object is a moving lens and receives interaction-driven highlight/shadow/inner-shadow.
5. Optical Test and real selected controls use the same `drawBackdrop`/`lens` mechanism. The Optical Test adds a second local trace layer only to make refraction observable.

## Deliberate differences from the catalog

- Apple-oriented geometry is represented with compact 52dp segmented and bottom-tab tracks and Refuge's three-item labels/icons.
- Search and modal content use product-readable text and local modal backdrops.
- These are adaptations, not claims that the local controls are official Apple or Android catalog components.
