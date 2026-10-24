# Current functional QA matrix

2026-10-25. Active checkout: `D:\RefugeNext`. The debug and AndroidTest APKs were installed with `adb install -r` on Google Android Emulator `Shared_Pixel7_API35`, API 35, serial `emulator-5554`; the existing application data was preserved. Evidence is in ignored `qa/current`.

| Area | Result | Evidence |
| --- | --- | --- |
| Unit tests | 95 passed, 0 failures, 0 errors | `app/build/test-results/testDebugUnitTest` and `qa/current/delivery-build.txt` |
| Build and lint | Debug, AndroidTest and Review build passed; lint has 118 warnings and 2 hints, no errors | `qa/current/delivery-build.txt` |
| Real authenticated session | Passed with the logged-in account and 109-item hangar still present after `adb install -r` | `qa/current/delivery-final-installed.png`, `qa/current/delivery-final-window.xml` |
| Tasks | Live task catalog, search, filters, language data, detail view, and detail/back navigation passed. The reward item and SCM crafting recipe services have no common stable identifier; direct item-name search can return no recipe, so this is not counted as a verified reward-to-recipe link. | `qa/current/mission-detail-native-light.png`, `qa/current/latest-knowledge-instrumentation-2.txt` |
| Blueprints | Live blueprint list, grouped materials, quality sections, and return navigation passed in both themes. | `qa/current/blueprints-native-dark.png`, `qa/current/blueprint-materials-native-dark.png`, `qa/current/latest-blueprint-instrumentation.txt` |
| Outfitting | Independent native page, real ship data, four draft slots, dashboard metrics, component selectors and safe local shield/power interactions passed | `qa/current/loadout-independent-light.png`, `qa/current/loadout-independent-dark.png`, `qa/current/dashboard-light-damage.png`, `qa/current/dashboard-dark-defense.png`, `qa/current/dashboard-dark-flight.png` |
| Root navigation and “我的” | Existing authenticated captures show Hangar, Store, Terminal and Profile in light and dark themes, with opaque captured backdrop and correct frosted navigation. The latest Compose drag harness hit Android's targeted-injection restriction; the equivalent physical device swipe via ADB from Profile to Terminal succeeded and selected Terminal. | `qa/current/live-root-{light,dark}-{hangar,store,terminal,profile}.png`, `qa/current/live-root-{light,dark}-drag-release.png`, `qa/current/live-root-adb-swipe-profile-terminal.png`, `qa/current/latest-root-final.txt` |
| Liquid controls | Slider and mode-selector press, drag, release and accessibility progress passed in both themes | `qa/current/slider-*.png`, `qa/current/modeSelector*.png` |
| Large text | Mission list was checked at 150% system font scale without clipped chips or overlapping content; font scale restored to 1.0 afterward | `qa/current/delivery-missions-large-text.png` |
| Device instrumentation | Full connected suite passed: 44 tests finished, 0 failures, 4 assumption-based skips. The run covered login, root navigation, hangar, store, terminal, blueprint, liquid controls, rendering, performance sampling, and native outfitting on the shared device. | `app/build/outputs/androidTest-results/connected/debug`, `qa/current/qa` |

The regression run did not execute ship reclaiming, melting, purchases, gifts, upgrades, or other account-mutating actions. The local shield and power checks only changed the draft shown in the editor.

## Remaining Erkul parity

The native editor follows the supplied Erkul dashboard structure and uses live ship/component data. The verified Avenger Stalker fixture renders burst DPS 2,360 and sustained DPS 1,459, matching the Erkul reference. Full projectile and penetration simulation, shopping-location inventory, crafting-quality application to the ship build, specialist flight tools, and power-dependent flight dynamics remain outside this verified scope; no blanket 60 fps or complete Erkul parity claim is made.

## 2026-10-25 repair pass

- `adb install -r` of the debug APK succeeded over the existing `com.refuge.next.compose` installation; the authenticated 109-item hangar remained available.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleReview` passed after the repair pass.
- Detail navigation now uses a flat inline back affordance instead of a circular top-left button. The hangar detail sheet was included in this change.
- Settings and independent routes no longer draw the root bottom navigation. Root, detail, settings, and outfitting navigation now swap in place without transitions.
- Power groups no longer render a circular icon background; real Cooler and PowerPlant consumers are listed separately below the pip chart.
- Blueprint material rows are grouped as a blue rounded material/SCU row followed by quality and modifier values. Loading surfaces are width constrained.
- SCMDB task versions, blueprint `dataVersion`, and Erkul manifest versions are checked before reusing persisted large payloads.

## Delivery state

The release candidate (`0.3.11`, versionCode 23) was installed on the shared emulator with `adb install -r`; the user's app data and login were preserved:

`app/build/outputs/apk/debug/app-debug.apk`

The Review APK and release workflow inputs are ready. The shared AVD remains `Shared_Pixel7_API35`; no additional emulator or checkout was created. No account-mutating action was used during verification.
