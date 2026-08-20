# Reference Sources

This lab is reference-driven. GitHub determines the optical implementation; Apple determines geometry and hierarchy.

## AndroidLiquidGlass

- Repository: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- Local checkout: `third_party/AndroidLiquidGlass`
- Remote: `https://github.com/Kyant0/AndroidLiquidGlass.git`
- Branch: `kmp`
- Commit: `b18eb0ff12c616546a68c72e7d0097f1ab286c87`
- Checkout time: `2026-08-19 21:55:45 +08:00`
- README: `third_party/AndroidLiquidGlass/README.md`
- Catalog components inspected: `LiquidButton`, `LiquidToggle`, `LiquidSlider`, `LiquidBottomTabs`
- Optical primitives inspected: `drawBackdrop`, `layerBackdrop`, `CombinedBackdrop`, `blur`, `lens`, `vibrancy`, `Highlight`, `InnerShadow`, `Shadow`, `InteractiveHighlight`, and `DampedDragAnimation`
- Official runtime evidence: [reference-evidence/android-liquid-glass](../reference-evidence/android-liquid-glass/)

The official catalog was built and exercised on the `RefugePro_API35` emulator. The evidence folder contains the build log, stills, and recordings for the catalog home, button, toggle, slider, bottom tabs, dialog, playground, and adaptive-luminance surfaces.

## Apple iOS/iPadOS 27

- UI Kit: [iOS and iPadOS 27 on Figma Community](https://www.figma.com/community/file/1651309003795292092/ios-and-ipados-27)
- Apple design resources: [developer.apple.com/design/resources](https://developer.apple.com/design/resources/)
- Public page state inspected: title, description, update date, preview iframe, and the public preview's `Page: Cover` state.
- Evidence: [reference-evidence/apple-ios27](../reference-evidence/apple-ios27/)

The public Community page exposes the official resource description and a cover preview, but this session could not open the file in the Figma editor or inspect component-level frames. Therefore Apple geometry claims are recorded as targets and not as verified measurements. The final matrix does not mark Apple component fidelity PASS on cover-only evidence.

## Local implementation

- Lab screen: `app/src/main/java/com/refuge/next/screens/ReferenceLabScreen.kt`
- Shared reference components and optical test: `app/src/main/java/com/refuge/next/reference/ReferenceLiquidComponents.kt`
- Interaction highlight: `app/src/main/java/com/refuge/next/reference/ReferenceInteractiveHighlight.kt`
- V4 evidence index: [artifacts/reference-lab-v4/README.md](../artifacts/reference-lab-v4/README.md)
