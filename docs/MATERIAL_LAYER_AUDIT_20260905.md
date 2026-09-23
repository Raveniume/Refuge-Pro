# Material Layer Audit - 2026-09-05

Scope: the Compose app on the API 35 Android emulator. This is a material and
rendering pass toward iOS 26, not a claim of pixel-identical Apple rendering.

## Diagnosis and Changes

- Content panels previously shared optical primitives with floating controls.
  Panels and connected lists now use opaque neutral surfaces, fine separators,
  and restrained elevation where appropriate. Dark surfaces use neutral grays.
- Page overlays now sample the composed page, including images and rows. The
  root owns one page capture; nested page scopes do not duplicate it. In-flow
  controls sample their explicit lower layer to avoid capturing themselves.
- Sheets and dialogs receive the actual page underlay across their window
  boundary. Sheet actions sample the page, modal base, and modal content. The
  presence picker is inside this scope as well.
- Shadow clipping and weak multiplied shadow alpha made light buttons lose
  elevation. Shared explicit shadow/highlight tokens now preserve external
  shadows. The icon-button wrapper also forwards its tint correctly.
- Floating buttons and bars have a theme-dependent neutral fill so image
  backgrounds do not erase their labels. Refraction remains active underneath.
- Detail sheets keep the reading area nearly opaque and reveal the page near
  the bottom action area instead of fading out behind the entire description.
- Hangar, buyback, store, and terminal lists now recycle individual rows instead
  of composing groups of eight. Row position determines group corners and
  separators; missing legacy numeric IDs no longer collide with each other.
- A resting tab bar uses its outer live glass pass. The additional selected-row
  capture and deforming lens are created only during press/drag animation.

## Verification

- `:app:testDebugUnitTest`: 42 tests passed, no failures or skipped tests.
- `:app:lintDebug`: passed; 38 warnings and one hint remain, no errors.
- `:app:assembleDebug` and `:app:assembleDebugAndroidTest`: passed.
- Android instrumentation: four tests passed, including page color sampling,
  opaque panel stability, external light-button shadows, sheet/dialog live
  sampling, bottom-tab click/drag, store cart updates, detail dismissal, and
  scrolling real Hangar/Store/Terminal composables.
- Material tests cover 360dp and 390dp in both themes. Production screen tests
  use the emulator's native 1080x2400 viewport and deterministic local fixtures.
  Their M80 rows are test data, not an authenticated account snapshot.
- Screenshots and measurements: `qa/material-layer-20260905/final/`.
  Production detail screenshots capture the composited display; material-test
  dialog captures isolate their window and may show black outside its surface.

## Performance Limits

Workload: 60 local-image store rows, dark theme, four upward and four downward
native touch swipes at 16ms input intervals after warmup. Android FrameMetrics
is collected from the activity window. This is a diagnostic workload in a
debuggable, instrumented app, not a release Macrobenchmark.

| Final Run | Frames | Over Frame Deadline | Median | P95 |
| --- | ---: | ---: | ---: | ---: |
| Glass enabled | 216 | 187 | 67.23 ms | 157.61 ms |
| Optical fallback | 234 | 167 | 32.35 ms | 56.88 ms |

The preceding run measured 34.92/97.74 ms with glass and 17.83/43.01 ms with
fallback. Host/emulator/test scheduling variation is substantial. Neither run
establishes smooth 60 Hz rendering, and neither supports a reliable percentage
improvement. Disabling optical rendering still produces slow frames. The
fallback comparison also changes control composition, so it does not isolate
GPU shader time by itself.

`native-before/` and `native-after/` preserve intermediate samples; these are
not an untouched original-app baseline. Older August gfxinfo files and the
zero-frame startup sample were not used to substantiate improvement. The early
`store-scroll-frames.json` used Compose's synthetic input clock and is superseded
by `store-scroll-glass.json` and `store-scroll-fallback.json`.

The next performance decision needs a profileable release build with a fixed
Macrobenchmark/Perfetto workload, separating main-thread layout, RenderThread,
GPU work, and emulator overhead. Authenticated network refresh, release-device
performance, large accessibility text, landscape, and other API levels were
not verified in this pass. Color-change tests prove live modal sampling but do
not establish pixel-exact cross-window refraction at every inset configuration.
