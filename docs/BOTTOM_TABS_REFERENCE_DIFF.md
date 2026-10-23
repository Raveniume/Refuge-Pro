# AndroidLiquidGlass reference mapping

Compared against the upstream `kmp` catalog on 2026-10-23:
[LiquidButton](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt),
[LiquidBottomTabs](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt), and
[LiquidSlider](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidSlider.kt).

The library supplies optical primitives; its catalog examples provide the control implementations. Refuge adapts those examples in `OfficialLiquidPorts.kt` and `RefugeLiquidSlider.kt`.

| Behavior | Current production mapping |
| --- | --- |
| Button optics | Vibrancy, 2dp blur, 12dp/24dp lens; tint uses the catalog's 0.75 alpha |
| Button motion | 4dp relative press expansion and 0.05 tanh translation |
| Tab optics | Vibrancy, 8dp blur, 24dp lens. Primitive defaults: 0.4 container / 0.1 selected tint. Production bottom navigation uses 0.70 / 0.08 in light mode and 0.42 / 0.12 in dark mode for the Refuge palette. |
| Bottom geometry | 64dp rail, 56dp selected lens, 78/56 pressed ratio; no extra 1.2 scale multiplier |
| Selected lens | Combined page/control sampling, 10dp/14dp chromatic lens while pressed, velocity deformation and spring settling |
| Accessibility | Only the invisible sampling row clears semantics; the visible row exposes selection and click actions |
| Editing | Native BasicTextField owns focus, editing, IME and hardware keys; its decoration uses the shared optical control |
| Continuous slider | `LiquidSlider.kt` port: 6dp track, 40×24dp thumb, 1.5 pressed scale, 8dp resting blur, 10/14dp chromatic lens, transformed track sampling, velocity deformation. Refuge adds a 48dp tap area, range semantics and updated callbacks. |
| Centered category strip | Keeps the requested fixed central lens and scrolling categories; uses 8dp bar blur, 24dp pressed bar lens, 10/14dp selected lens, neutral 0.1 resting tint and the reference press/scale spring parameters. Track and lens captures are separate. |

Intentional differences: four Refuge root destinations, theme accents, compact top-control geometry, restrained button highlight/shadow, and direct state/callback parameters. Refuge keeps a foreground label pass to preserve legibility during small drags and uses touch-slop handling so a single tap on an unselected destination works. It is an adaptation, not a byte-for-byte copy. Dense content cards and transaction sheets use opaque content surfaces; interactive controls retain the glass layer.

Test captures live under ignored `qa/current/qa`; the current verification status is recorded in `FINAL_FUNCTIONAL_QA_MATRIX.md`. A component fixture does not prove live RSI behavior.

2026-10-24: root capture now includes `palette.background` *inside* `layerBackdrop`. The previous transparent capture caused sharp Profile utility labels to remain visible through the composited blur. This changes the sampled content, not the reference's blur/lens/motion constants. Verified against the supplied Profile screenshot on the real authenticated app; current evidence is `qa/current/profile-nav-fixed.png`.
