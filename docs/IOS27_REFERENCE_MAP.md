# iOS 27 Reference Map

This map records the Apple targets separately from what was actually verifiable in the public Figma Community page. The page was opened at [iOS and iPadOS 27](https://www.figma.com/community/file/1651309003795292092/ios-and-ipados-27); its embedded preview remained on `Page: Cover`, so no component-level Figma frame measurements are asserted here.

## Shared hierarchy targets

| Token role | Apple target | Current lab treatment | Verification |
|---|---|---|---|
| Background | system background / grouped background hierarchy | Theme-specific dark/light app background with optical traces | Runtime screenshots |
| Primary label | highest-contrast label | White in dark theme, black in light theme | Runtime screenshots |
| Secondary label | reduced-emphasis label | Theme-aware reduced-alpha text/icons | Runtime screenshots |
| Separator | quiet, visible divider | Optical boundaries and modal scrims; no decorative idle border | Runtime screenshots |
| Functional material | quiet control surface | Search field uses restrained lens/blur/vibrancy | Runtime + UI XML |
| Elevated material | modal content above scrim | Opaque dark/light modal bases with explicit scrim | Sheet/alert screenshots |
| Accent | semantic accent only | Blue selected-state accent (`#0088FF`) | Runtime screenshots |

## Component targets and current implementation

| Component | Apple target | Current geometry / hierarchy | Status |
|---|---|---|---|
| Bottom Tab | Floating tab bar, selected and unselected states | 64dp track, 4dp inset, three equal items, icon + label; selected moving lens | Apple frame not inspectable; Android optics verified |
| Segmented | Single-selection compact control | 52dp track, 4dp inset, equal-width items, selected moving lens | Apple frame not inspectable; shared lens verified |
| Floating Icon Button | Icon-only floating control | 56dp touch region with live glass and action semantics | Apple frame not inspectable; Android button optics verified |
| Search / Field | Empty/editing field hierarchy | Full-width rounded field, leading icon, inline placeholder, no permanent focus border | Apple frame not inspectable; functional field behavior verified |
| Sheet / Alert | Sheet/alert with scrim and elevated material | Explicit scrim, stable opaque modal base, local action backdrop | Apple frame not inspectable; layering verified |

## Geometry policy

The current dp values are implementation calibration values, not extracted Apple measurements. They must be replaced or confirmed only after the Figma editor exposes the relevant component frames. No Hangar migration should consume these as final Apple geometry until that review occurs.
