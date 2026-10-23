# Erkul dashboard memory

## Data contract

The outfitting dashboard reads the public Erkul LIVE catalog directly and caches the catalog under the app's private files directory. The visible source marker must include the branch and catalog version so a user can tell whether the page is showing current data or a cached copy. The current verified catalog is `4.10.2-LIVE.12913470`.

Ship, component, weapon, shield, hull, and flight values come from the selected ship and its live component records. The active local draft can change component selection, gimbal mode, flight mode, power allocation, and shield state; those changes are local and must never be sent to RSI.

Damage source groups are classified from Erkul's slot/container metadata: Pilot, Manned turret, Remote turret, and PDS. Do not classify them from a display path string or a translated label. Missing categories render as an em dash rather than a fabricated zero damage value.

## Layout and appearance

The page is an independent route. It uses four compact reading groups matching the Erkul information architecture: Damage, Defense, Hull, and Flight. Each group has an explicit symbol, a source/version marker, and tight internal spacing. Reading surfaces use the Refuge semantic light/dark surfaces; liquid glass is reserved for functional mode selectors and floating controls.

Use the official AndroidLiquidGlass-derived mode selector and slider ports for gimbal mode, flight mode, power, and quality controls. Verify resting, pressed, dragged, and released states in both themes on the shared `Shared_Pixel7_API35` emulator.

## Known limits

Erkul publishes stock burst DPS and component data, while sustained DPS is calculated locally from live fire actions, capacitor regeneration, heat, and power allocation. The verified Avenger Stalker now matches the published 1,459 sustained DPS reference (burst 2,360); this does not imply complete parity for advanced simulation features.

SCMDB mission data and SCM blueprint data are separate live services. A mission reward item can be searchable in the blueprint service without having a matching crafting recipe. The terminal must preserve that distinction and show an empty-result state when the services do not share an identifier.

## QA rule

Use ADB and the shared emulator for the authenticated loop. Preserve app data with `adb install -r`. Do not run reclaiming, melting, purchases, gifts, upgrades, or other account-mutating actions during dashboard and terminal verification.
