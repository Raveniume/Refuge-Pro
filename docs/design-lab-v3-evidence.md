# Reference Replication Lab V4 Evidence

Scope: five Apple-targeted components only. Hangar, Store, Terminal, Profile, and CCU remain out of scope.

## Verification boundary

The AndroidLiquidGlass catalog was cloned, built, and exercised before the local lab was changed. Its source SHA, catalog evidence, and local mapping are recorded in [REFERENCE_SOURCES.md](REFERENCE_SOURCES.md) and [ANDROID_LIQUID_GLASS_COMPONENT_MAP.md](ANDROID_LIQUID_GLASS_COMPONENT_MAP.md).

The Apple iOS/iPadOS 27 Community file was opened in the browser. The public page and embedded preview were readable, but the preview exposed only `Page: Cover`; the Figma editor and component-level frames were not available. Apple component fidelity is therefore not claimed as PASS.

## Required matrix

### Bottom Tab

- Apple Reference Fidelity: **UNVERIFIED / NON-BLOCKING** — the public Apple preview did not expose Tab Bar component geometry or selected/unselected frames. Current values are documented calibration targets, not measured Apple values.
- AndroidLiquidGlass Fidelity: **PASS** — the local implementation follows the catalog's layered backdrop, combined selected lens, highlight/shadow, damped drag, and release settling behavior. See `v4-bottom-tab-release.png` and `v4-bottom-tab-drag.mp4`.
- Refuge Adaptation Readiness: **PASS** — the lab has stable three-item icon/label semantics, dark/light runtime evidence, and no production data coupling. User confirmation is still required before migration.

### Segmented

- Apple Reference Fidelity: **FAIL** — the selected geometry is a calibrated `LiquidBottomTabs` derivative because the Apple segmented frame was not inspectable.
- AndroidLiquidGlass Fidelity: **PASS** — selected state is a moving glass lens with the same `drawBackdrop`/`lens` pipeline as the Optical Test; it is not a static filled capsule. See `v4-segmented-middle.png`, `v4-segmented-drag-release.png`, and `v4-segmented-drag.mp4`.
- Refuge Adaptation Readiness: **PASS** — dark/light labels, selected semantics, drag/release continuity, and stable touch targets are present. User confirmation is still required.

### Floating Icon Button

- Apple Reference Fidelity: **UNVERIFIED / NON-BLOCKING** — icon-only floating geometry and proportions could not be measured from the cover-only Apple preview.
- AndroidLiquidGlass Fidelity: **PASS** — the button uses live backdrop/lens/highlight deformation and `Role.Button` semantics rather than a normal Material rounded rectangle. See `v4-floating-button.mp4`.
- Refuge Adaptation Readiness: **PASS** — the action is isolated in the lab, has an explicit accessibility label, and works in both themes.

### Search / Field

- Apple Reference Fidelity: **UNVERIFIED / NON-BLOCKING** — field radius, typography, and editing-state spacing were not available for component-level comparison.
- Refuge Adaptation Readiness: **PASS** — the field is stable, single-line, theme-aware, uses a restrained functional glass surface, and exposes the `Search` semantic label. See `v4-search-field.png` and `v4-ui.xml`.

### Sheet / Alert

- Apple Reference Fidelity: **FAIL** — Apple sheet detents, alert width, action spacing, and typography were not inspectable.
- Refuge Adaptation Readiness: **PASS** — explicit scrim, opaque modal base, local modal backdrop for actions, dark/light captures, and stable sizing are verified in `v4-sheet-dark.png`, `v4-alert-dark.png`, and `v4-alert-light.png`.

## Optical and accessibility checks

- Optical Test and selected controls share the same real `drawBackdrop` + `lens` implementation. The test adds a local trace layer only to make displacement visible.
- Selected segmented and bottom-tab items expose `Role.Tab` and selected semantics; the search field exposes `Search`; the floating action exposes `Open alert`.
- The final `:app:assembleDebug` build passed after the semantics-only edit.
- V4 screenshots cover dark and light themes. Emulator evidence is not a physical-device performance claim.

## Decision

AndroidLiquidGlass behavior is ready for user review. Apple geometry is not approved for production migration because component-level Apple reference evidence is missing. Do not start Hangar migration until the user confirms the five components and whether to proceed with the documented Apple verification gap.
