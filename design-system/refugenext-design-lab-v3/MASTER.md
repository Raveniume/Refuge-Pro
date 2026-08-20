# RefugeNext Design Lab V3

This is the active visual gate for the isolated Compose presentation layer. It is a reference replication lab, not a production Hangar screen.

## Reference Order

1. Apple iOS/iPadOS 27 UI Kit component geometry and state behavior.
2. Apple semantic light/dark hierarchy and material separation.
3. AndroidLiquidGlass implementation limits and performance budget.
4. Refuge-specific content, Chinese PingFang type resources, and Star Citizen accent usage.

The Apple reference is authoritative for Tab Bar, Segmented Control, Floating Icon Button, Search/Field, Sheet, and Alert. Refuge adaptation is explicit in `design-system/IOS27_REFERENCE_MAP.md`.

## Material Contract

Functional controls use `RefugeLiquidGlass`, which is the only presentation entry point for backdrop sampling, blur, vibrancy, lens refraction, dynamic highlight, and interaction shadow. The optical test and production controls must call the same implementation.

Content rows use `RefugeContentSurface` with stable tonal surfaces. They do not receive nested live backdrop effects.

## Color Contract

Dark and light begin from Apple semantic neutrals:

- Light background: `#F2F2F7`
- Dark background: `#101114`
- Light elevated material: white
- Dark elevated material: `#2C2C2E`
- System accent: blue, reserved for action and selection communication

No Refuge-specific cyan, yellow, or mineral palette is allowed to become the dominant surface system. Idle chromatic aberration is prohibited.

## Interaction Contract

- Controls expose at least a 44dp touch target.
- Press uses a broad, low-alpha optical response rather than a ripple.
- Segmented and tab selection preserve the existing damped drag, velocity, and continuous settle behavior.
- Selection must remain a moving material object during transitions.
- No fixed specular dots or static decorative highlights.

## V3 Scope

Only these components are in scope until manual visual confirmation:

- Tab Bar
- Segmented Control
- Floating Icon Button
- Search / Field
- Sheet / Alert

Do not use this lab as permission to rebuild Store, Terminal, Profile, CCU, or the Hangar Golden Master.
