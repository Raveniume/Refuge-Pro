# iOS/iPadOS 27 Reference Map

> Verification note: the public Figma Community page was opened, but its embedded preview remained on `Page: Cover`; component-level Apple frames were not available in this session. This file is retained for design-system provenance. The auditable V4 map and status live in [`docs/IOS27_REFERENCE_MAP.md`](../docs/IOS27_REFERENCE_MAP.md).

This document separates Apple reference decisions from RefugeNext adaptations. The source reference is Apple's [iOS and iPadOS 27 UI Kit](https://www.figma.com/community/file/1651309003795292092/ios-and-ipados-27). Frame names below use the UI Kit component taxonomy; exact Figma layer names may change between kit revisions.

## Shared Material Contract

`RefugeLiquidGlass` is the single Compose implementation for the optical test, segmented selection lens, tab selection lens, floating icon button, search field, and modal actions. The implementation uses AndroidLiquidGlass `drawBackdrop` with `vibrancy`, `blur`, and `lens`. Static fills are intentionally low alpha. Chromatic aberration is disabled at rest and may be enabled only while the interaction progress is non-zero.

## Tab Bar

COMPONENT: Tab Bar

Apple reference: iOS/iPadOS 27 UI Kit, Components / Tab Bar / Floating Liquid Glass

Figma component/frame: `Tab Bar`, `Floating`, `Selected` and `Unselected` states

Geometry: Floating full-width bar, 4dp internal inset, 52dp visual height, 58dp touch container. `CUSTOM` width is 88% of the app window for Refuge's floating composition.

Corner geometry: Capsule/concentric continuous curve.

Base material: Apple Liquid Glass with backdrop participation.

Selected material: Moving lens object sampled from the bar and page backdrop, with a theme-aware neutral selection surface.

Stroke: No explicit idle border. Boundary comes from the optical edge and restrained inner shadow.

Highlight: Interaction-driven `Highlight.Default`; no fixed specular dot.

Shadow: Low dynamic shadow during press/drag only.

Typography: Apple tab label hierarchy is represented by the app's icon-first tab treatment; labels remain in semantics for accessibility.

Light appearance: Neutral white glass over `systemGroupedBackground`-like `#F2F2F7`.

Dark appearance: Neutral white glass over `systemBackground`-like `#101114`.

Pressed: The shared lens increases refraction and highlight progressively.

Dragging: `DampedDragAnimation` continuously moves the lens and preserves velocity.

Released: Current position and velocity settle to the nearest tab.

Compose implementation: `RefugeBottomBar.kt` -> `RefugeLiquidGlass`.

## Segmented Control

COMPONENT: Segmented Control

Apple reference: iOS/iPadOS 27 UI Kit, Components / Segmented Control / Standard

Figma component/frame: `Segmented Control`, `Single Selection`, `Selected` / `Unselected`

Geometry: 46dp track with 3dp optical inset; equal-width segments.

Corner geometry: Capsule track and concentric selected capsule.

Base material: Transparent Liquid Glass track with sampled backdrop.

Selected material: The selection is a shared Liquid Glass lens with a theme-aware neutral selection surface, not a colored fill.

Stroke: `CUSTOM` no visible white outline at rest; Apple material edge is represented by refraction and inner shadow.

Highlight: Interaction-driven only.

Shadow: Low dynamic shadow while pressed.

Typography: Project headline token mapped to Apple's compact control label scale.

Light appearance: Neutral glass over a distinct `#F2F2F7` background.

Dark appearance: Neutral glass over a distinct `#101114` background.

Pressed: Refraction height/amount and highlight increase with press progress.

Dragging: Existing damped pointer attraction and velocity are retained.

Released: Nearest segment receives the continuous lens settle.

Compose implementation: `RefugeSegmentedControl.kt` -> `RefugeLiquidGlass`.

## Floating Icon Button

COMPONENT: Floating Icon Button

Apple reference: iOS/iPadOS 27 UI Kit, Components / Buttons / Icon Button / Floating

Figma component/frame: `Button`, `Icon Only`, `Floating` states

Geometry: 52dp visual control with a 44dp minimum touch target.

Corner geometry: `CUSTOM` 18dp continuous approximation for the compact square reference geometry.

Base material: Small Liquid Glass object with local backdrop.

Selected material: Not applicable; action uses pressed material.

Stroke: No idle border.

Highlight: Wide, low-alpha interaction highlight from the shared glass pipeline.

Shadow: Restrained dynamic shadow on press.

Typography: Icon-only; semantics always provide the action label.

Light appearance: Neutral glass with system blue accent icon.

Dark appearance: Neutral glass with lighter system blue accent icon.

Pressed: Shared refraction and highlight response, no Android ripple.

Dragging: Not applicable.

Released: Material settles without a positional animation.

Compose implementation: `DesignLabScreen.kt` -> `RefugeLiquidGlassButton`.

## Search / Field

COMPONENT: Search / Field

Apple reference: iOS/iPadOS 27 UI Kit, Components / Search Field

Figma component/frame: `Search Field`, `Empty`, `Editing`

Geometry: Full-width 44dp+ field with leading search icon and inline placeholder.

Corner geometry: Compact rounded rectangle with a concentric 12dp radius.

Base material: Functional Liquid Glass with restrained blur and lens.

Selected material: Focus is communicated by text/cursor and material response; no permanent blue outline.

Stroke: No explicit border at rest.

Highlight: `CUSTOM` shared press response; focus remains primarily semantic.

Shadow: None at rest; shared low dynamic shadow only when interacted with.

Typography: Body token maps Chinese PingFang to Apple's search text hierarchy.

Light appearance: Neutral translucent control over `#F2F2F7`.

Dark appearance: Neutral translucent control over `#101114`.

Pressed: Shared glass response.

Dragging: Not applicable.

Released: Field remains stable; no layout shift.

Compose implementation: `DesignLabScreen.kt` -> `BasicTextField` + `RefugeLiquidGlass`.

## Sheet / Alert

COMPONENT: Sheet / Alert

Apple reference: iOS/iPadOS 27 UI Kit, Components / Sheets and Alerts

Figma component/frame: `Sheet`, `Alert`, `Scrim`, `Action`

Geometry: Sheet is bottom aligned with page inset; alert is compact and centered.

Corner geometry: 20dp sheet/alert radius with concentric inner content.

Base material: Opaque or near-opaque standard content material above an explicit scrim. It must not recursively blur the app root.

Selected material: Actions may use the shared Liquid Glass button pipeline.

Stroke: No decorative border.

Highlight: Action interaction only.

Shadow: Modal elevation from standard material shadow.

Typography: Title/body/action use project tokens mapped to Apple hierarchy.

Light appearance: White elevated content surface over a neutral scrim.

Dark appearance: `#2C2C2E` elevated content surface over a dark scrim.

Pressed: Only the action responds optically.

Dragging: `CUSTOM` sheet drag is deferred; current slice validates layering and dismissal.

Released: Modal remains stable until an explicit action or dismiss gesture.

Compose implementation: `RefugeDialog.kt` and `DesignLabScreen.kt` sheet.

## Deliberate Refuge Adaptations

- Chinese PingFang resources are retained because they are project-approved and present.
- Star Citizen content, deep-space background, M80 imagery, and compact filter/sort semantics remain Refuge-specific and are not Apple references.
- App tab width, icon family, and copy are `CUSTOM` adaptations.
- Content-layer rows use stable material instead of per-row live backdrop effects.
