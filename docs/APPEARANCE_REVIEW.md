# Refuge appearance review

## 2026-10-24: information groups and native reference dashboard

- Use `RefugeInfoGroup` for distinct concepts inside one reading card, with an outside caption, a secondary semantic surface, and 12dp inner padding. `RefugeInfoRows` pairs labels with right-aligned values and quiet separators; above 130% text scaling it stacks labels and values. Avoid adding optical effects to these reading surfaces.
- Blueprint cards separate material/quantity, quality controls, and attribute results. Mission metadata separates commission details from acceptance/execution requirements. Inline mission placeholders retain their literal text in accessibility and gain a subtle accent background.
- The outfitting dashboard follows the supplied Erkul reference's metric tiles, source table, resistance/absorption chips, armor/signature groups, hull distribution, and flight section. Small screens stack panels; wide layouts use two columns. Switches use the existing AndroidLiquidGlass port. Values come from current public game data and the active local draft, not numbers copied from a screenshot.
- Root navigation must capture the opaque page canvas as well as content. A transparent capture let sharp Profile utility text show through the blurred sample. The canvas is now inside the root backdrop capture; no extra full-screen offscreen layer is retained.
- Source-table classification distinguishes pilot, manned turret, remote turret, and PDS using the live slot/container metadata. Flight figures show base and rated afterburner values from the selected real blade; this is not a complete simulation of power-dependent flight dynamics.

## 2026-10-24: online Erkul dashboard and terminal data

- Treat the public Erkul LIVE catalog as the source of truth for ship and component values. The app reads `https://cdn.erkul.games/LIVE/catalog.bin`, keeps a local cache for fast first render/offline fallback, and surfaces the branch/version in the dashboard.
- Keep the Damage, Defense, Hull, and Flight groups compact and data-driven. Read damage rows from actual weapon slot/container metadata; do not infer turret class from display labels or hard-code a ship's values.
- The live Avenger Stalker reference was verified against Erkul LIVE `4.10.2-LIVE.12913470`: burst DPS 2,360; shield HP 4,488; shield regeneration 852; armor HP 1,232; hull 11,900; hull mass 48,986.3 kg; and the published stock flight/quantum values. The client-computed sustained DPS remains an approximation and currently differs from the Erkul reference, so it must be corrected before claiming full parity or publishing a release.
- SCM blueprints and SCMDB missions are live independent services. A mission's item reward name is not necessarily a crafting-blueprint name; linked search may legitimately have no matching recipe. Keep that distinction visible and do not imply a product/recipe identity without a shared source identifier.
- Current authenticated ADB checks passed for live mission list/detail, live blueprint list/material detail, and the outfitting dashboard in both themes. Android rejected the Compose test process's direct targeted drag injection, but a physical-device `adb shell input swipe` from Profile to Terminal selected Terminal correctly and produced a fresh capture.

Design basis: Apple HIG materials (reading content separated from functional glass), layout (proximity communicates grouping), typography/accessibility (scalable labels and values). Optical behavior remains mapped in `BOTTOM_TABS_REFERENCE_DIFF.md`.

2026-10-23 · Android / Jetpack Compose · light appearance and grouped settings

## Assessment

The previous light palette separated page and cards, but full-white cards, strong dividers, and repeated shadows competed with the content. The user's iPad Settings reference calls for a quiet page, brighter grouped content, and functional controls that remain distinguishable from both.

## Applied changes

- Page canvas: `#ECEEF3`; content groups: `#FAFBFD`; secondary/modal surface: `#F4F5F9`. Avoid large areas of display white. Keep these semantic roles in `RefugePalette` across login, the four root routes, settings, missions, blueprints, and outfitting.
- Settings group labels align with row text, use secondary text styling, and sit outside each continuous-corner content group.
- Shared list dividers use 0.5 dp and a quieter light-mode tint. Elevated content shadows use 3.5% black. Ordinary groups remain flat.
- Light action blue is `#0066D6`; warning amber is `#906000`; error red is `#C53228`, preserving readable text on the softer surfaces. Main, secondary, muted, action, warning, success, and error text all exceed 4.5:1 against the three solid light surfaces.
- Keep optical blur/refraction on floating navigation and functional controls. Reading surfaces use solid semantic fills, so a busy image cannot obscure their text.
- Preserve existing Refuge typography, continuous corners, native inputs, dark theme, and scalable text.
- Continuous controls use the adapted AndroidLiquidGlass `LiquidSlider`, including its transformed track capture, pressed chromatic lens, velocity deformation and spring release. Category strips retain the user's fixed central lens while sharing the reference optical/press parameters. A generic Material Slider is no longer used for blueprint quality.

## Review guidance

Use the installed `apple-design` skill for future UI work. Its color, materials, accessibility, layout, and typography references informed this review. The relevant public design principles are semantic background hierarchy, sufficient text contrast, and using materials to distinguish controls from content:

- https://developer.apple.com/design/human-interface-guidelines/color
- https://developer.apple.com/design/human-interface-guidelines/materials
- https://developer.apple.com/design/human-interface-guidelines/accessibility

Visual acceptance requires current light/dark captures of all four root routes, settings, login, and the new terminal pages on the shared AVD. Test fixtures establish rendering and interaction behavior, not successful RSI authentication. See `FINAL_FUNCTIONAL_QA_MATRIX.md` for execution results and remaining gates.

Current evidence: `qa/current/qa/settings-light.png`, `settings-dark.png`, `root-light-*.png`, `root-dark-*.png`, `slider-light-pressed.png`, `slider-dark-pressed.png`, and the corresponding dragged/resting captures. The slider test checks physical tap/drag, both themes, its 48dp tap region, range changes, dragging after a range change, and accessible progress actions. Pointer input is keyed to the current animation instance so replacing a material range cannot retain obsolete drag conversion. The measured minimum text contrast over the three solid light surfaces is 4.57:1 (`qa/current/light-contrast.json`).
