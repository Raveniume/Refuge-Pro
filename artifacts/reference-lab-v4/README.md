# Reference Lab V4 Evidence

All captures are from the Compose `Reference Replication Lab` on the `RefugePro_API35` emulator. V4 is the current evidence set after the pointer, semantics, backdrop, and modal-layer fixes.

## Launch and themes

- `v4-dark-launch.png` — dark theme, all five components and Optical Test
- `v4-light-launch.png` — light theme, all five components and Optical Test
- `reference-lab-dark-launch.png` — earlier dark capture retained for provenance

## Interaction evidence

- `v4-segmented-middle.png` — selected segmented item after a middle selection
- `v4-segmented-drag-release.png` — selected item after drag and release
- `v4-segmented-drag.mp4` — segmented drag continuity recording
- `v4-bottom-tab-release.png` — selected bottom tab after interaction
- `v4-bottom-tab-drag.mp4` — bottom-tab drag continuity recording
- `v4-floating-button.mp4` — floating icon button interaction recording

## Field and modal evidence

- `v4-search-field.png` — clean unfocused search field
- `v4-search-focus-debug.png` — focus/debug capture retained to explain IME-toolbar noise
- `v4-sheet-dark.png` — dark sheet with stable modal base and scrim
- `v4-alert-dark.png` — dark alert with stable modal base and scrim
- `v4-alert-light.png` — light alert state

## Accessibility evidence

- `v4-ui.xml` — UI Automator hierarchy after the final semantics edit; includes `Search`, tab roles/selection, and action labels.
- `v4-ui-tab2.xml` — UI Automator hierarchy after selecting the middle segmented item; confirms the selected semantic moves to Tab 2.

## Reproduction

```text
.\gradlew.bat :app:assembleDebug
```

The final build completed successfully after the V4 semantics edit. The APK was exercised on the `RefugePro_API35` emulator (`emulator-5554`).

## Reading the evidence

The AndroidLiquidGlass optical behavior is directly observable in the selected lens edges, background displacement, and drag recordings. The Apple evidence folder contains only the public Figma Community cover/preview; it does not prove component-level Apple frame fidelity. See `docs/COMPONENT_DIFF_NOTES.md` and `docs/design-lab-v3-evidence.md` for the final matrix.
