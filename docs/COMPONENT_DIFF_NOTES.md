# Component Diff Notes

This is the honest comparison record for the five-component Reference Replication Lab. It distinguishes observable behavior from unverified Apple geometry.

| Component | Observable match | Observable gap / risk | Decision |
|---|---|---|---|
| Bottom Tab | Official catalog-like backdrop participation, selected lens refraction, drag continuity, velocity-aware settling, dark/light evidence | Apple tab-bar height, inset, icon optical size, and selected-state geometry were not inspectable in the public preview | Android fidelity PASS; Apple geometry unverified and non-blocking |
| Segmented | Selected item is a real moving lens, not a static fill; Optical Test and control share the lens pipeline; drag/release evidence exists | Current geometry is a calibrated derivative of `LiquidBottomTabs`, not verified Apple segmented geometry; selected text remains blue by local adaptation | Android fidelity PASS; Apple fidelity FAIL pending component inspection |
| Floating Icon Button | Live backdrop/lens/highlight response, no ordinary Material ripple, action semantics | The public Apple preview does not expose icon-only floating component proportions; circular treatment is a local calibration | Android optics PASS; Apple geometry unverified and non-blocking |
| Search / Field | Stable single-line field, quiet functional glass, leading icon, semantic label, clean dark/light evidence | Placeholder typography, field radius, and editing-state spacing are not measured against the Apple frame | Apple geometry unverified and non-blocking |
| Sheet / Alert | Explicit scrim, opaque modal base, local modal backdrop for actions, dark/light runtime evidence; no recursive page-content bleed | Apple sheet detents, alert width, typography, and action spacing are not measured; sheet drag is deferred | Apple fidelity FAIL pending component inspection |

## Known fixes in V4

- Pointer offset now drives button deformation instead of being tracked but unused.
- Selection and bottom-tab items expose `Role.Tab` and selected semantics.
- Search exposes a semantic `Search` label.
- Sheet and alert actions sample a local modal backdrop instead of an unattached page backdrop.
- Modal content uses `matchParentSize()` and an explicit opaque base so it does not expand into a full-screen accidental surface.
- The Optical Test uses the same combined backdrop and real lens refraction path as selected controls.

## Explicit non-goals

Hangar, Store, Terminal, Profile, CCU, production filter/sort controls, and Flutter UI are outside this lab and remain untouched.
